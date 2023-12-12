package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Streams.stream;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.LexiconStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import com.digitald4.biblical.util.BPETokenizer;
import com.digitald4.biblical.util.Constants;
import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.biblical.util.Language;
import com.digitald4.biblical.util.LexiconFetcher;
import com.digitald4.biblical.util.LexiconFetcherBlueLetterImpl;
import com.digitald4.biblical.util.ScriptureFetcherBibleGateway;
import com.digitald4.biblical.util.ScriptureFetcherBibleHub;
import com.digitald4.biblical.util.ScriptureFetcherJWOrg;
import com.digitald4.biblical.util.ScriptureFetcherKJV1611;
import com.digitald4.biblical.util.ScriptureFetcherOneOff;
import com.digitald4.biblical.util.ScriptureFetcherPseudepigrapha;
import com.digitald4.biblical.util.ScriptureFetcherRouter;
import com.digitald4.biblical.util.ScriptureFetcherSefariaOrg;
import com.digitald4.biblical.util.ScriptureFetcherStepBibleOrg;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOFileBasedImpl;
import com.digitald4.common.util.Calculate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.IntStream;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation;

public class DiffStats {
  private final static String DB_FILE = "data/Isaiah-Hebrew.db";

  private final ScriptureStore scriptureStore;
  private final InterlinearStore interlinearStore;

  public DiffStats(ScriptureStore scriptureStore, InterlinearStore interlinearStore) {
    this.scriptureStore = scriptureStore;
    this.interlinearStore = interlinearStore;
  }

  public ImmutableList<ScriptureDiffStats> process(boolean ignorePun) {
    return IntStream.range(1, 67)
        .boxed()
        .flatMap(chapter -> {
          System.out.println("Chapter: " + chapter);
          ImmutableMap<String, String> dss =
              scriptureStore.getScriptures("DSS", "he", "Isa " + chapter).getItems().stream()
                  .collect(
                      toImmutableMap(
                          s -> String.format("DSS-%d", s.getVerse()),
                          s -> s.getText().toString()));
          ImmutableMap<String, String> wlco =
              scriptureStore.getScriptures("WLCO", "he", "Isa " + chapter).getItems().stream()
                  .collect(
                      toImmutableMap(
                          s -> String.format("WLCO-%d", s.getVerse()),
                          s -> s.getText().toString()));

          return IntStream.range(1, Math.max(dss.size(), wlco.size()) + 1).mapToObj(
              v -> ScriptureDiffStats.of("Isaiah", chapter, v,
                  dss.getOrDefault("DSS-" + v, ""), wlco.getOrDefault("WLCO-" + v, ""), ignorePun));
        })
        // .peek(s -> System.out.println(s.lettersRemoved))
        .collect(toImmutableList());
  }

  public void printWordStats(String version) {
    BPETokenizer bpeTokenizer = new BPETokenizer();
    ImmutableList<String> totalWords = IntStream.range(1, 67)
        .boxed()
        .flatMap(c -> scriptureStore.getScriptures(version, "he", String.format("Isa %d", c)).getItems().stream())
        .map(Scripture::getText)
        .map(HebrewConverter::removePunctuation)
        .flatMap(text -> Arrays.stream(text.split(" ")))
        .peek(bpeTokenizer::tokenize)
        .collect(toImmutableList());
    System.out.printf("%d total words in the %s book of Isaiah\n", totalWords.size(), version);

    Map<String, Long> wordCounts = totalWords.stream().collect(groupingBy(identity(), counting()));
    System.out.printf("%d unique %s words\n", wordCounts.size(), version);

    wordCounts.entrySet().stream().sorted(Entry.comparingByValue(reverseOrder()))
        .limit(21).forEach(System.out::println);

    System.out.println("Root words: " + bpeTokenizer.getTokens().subList(0, 100));
  }

  public void fillDssWords() {
    IntStream.range(1, 67).boxed()
        .flatMap(c -> scriptureStore.getScriptures("WLCO", Language.INTERLINEAR, "Isa " + c).getItems().stream())
        .forEach(scripture -> interlinearStore.create(((InterlinearScripture) scripture).getInterlinears()));
  }

  public static void main(String[] args) throws IOException {
    long startTime = System.currentTimeMillis();
    APIConnector apiConnector =
        new APIConnector(Constants.API_URL, Constants.API_VERSION, 100).setIdToken("39284720");
    DAOFileBasedImpl dao = new DAOFileBasedImpl(DB_FILE).loadFromFile();
    StaticDataDAO staticDataDAO = new StaticDataDAO();
    BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
    ScriptureReferenceProcessor referenceProcessor =
        new ScriptureReferenceProcessorSplitImpl(bibleBookStore);
    LexiconFetcher lexiconFetcher = new LexiconFetcherBlueLetterImpl(apiConnector, false);
    InterlinearStore interlinearStore =
        new InterlinearStore(() -> dao, referenceProcessor, lexiconFetcher);
    LexiconStore lexiconStore = new LexiconStore(() -> dao, lexiconFetcher);
    ScriptureStore scriptureStore = new ScriptureStore(
        () -> dao, null, bibleBookStore,
        referenceProcessor,
        new ScriptureFetcherRouter(
            new ScriptureFetcherBibleGateway(apiConnector),
            new ScriptureFetcherBibleHub(apiConnector),
            new ScriptureFetcherJWOrg(apiConnector),
            new ScriptureFetcherKJV1611(apiConnector),
            new ScriptureFetcherOneOff(apiConnector),
            new ScriptureFetcherPseudepigrapha(apiConnector),
            new ScriptureFetcherSefariaOrg(apiConnector),
            new ScriptureFetcherStepBibleOrg(apiConnector)),
        interlinearStore, null);

    scriptureStore.getScriptures("DSS", "he", "Isa 64:1");

    DiffStats statsProcessor = new DiffStats(scriptureStore, interlinearStore);

    /* ImmutableList<ScriptureDiffStats> stats = statsProcessor.process(true);
    HashMap<Character, AtomicLong> removed = new HashMap<>();
    HashMap<Character, AtomicLong> added = new HashMap<>();
    stats.forEach(s -> {
      s.lettersRemoved.forEach(
          (c, count) -> removed.computeIfAbsent(c, c_ -> new AtomicLong()).addAndGet(count));
      s.lettersAdded.forEach(
          (c, count) -> added.computeIfAbsent(c, c_ -> new AtomicLong()).addAndGet(count));
    });

    System.out.println("Added Characters:");
    added.entrySet().stream()
        .sorted(comparing(e -> e.getValue().get())).forEach(System.out::println);

    System.out.println("\nRemoved Characters:");
    removed.entrySet().stream()
        .sorted(comparing(e -> e.getValue().get())).forEach(System.out::println);

    System.out.println("\nPercent Match:");
    stats.stream().sorted(comparing(ScriptureDiffStats::getPercentMatch)).forEach(
        s -> System.out.printf("%s %d:%d %d%%\n", s.book, s.chapter, s.verse, s.percentMatch));

    System.out.println("\nLDs:");
    stats.stream().sorted(comparing(ScriptureDiffStats::getLd))
        .forEach(s -> System.out.printf("%s %d:%d %d\n", s.book, s.chapter, s.verse, s.ld));

    System.out.println("\nLDs:");
    ImmutableList<Integer> lds =
        stats.stream().map(ScriptureDiffStats::getLd).sorted().collect(toImmutableList());
    System.out.println(lds.stream().map(String::valueOf).collect(joining(",")));

    int sum = lds.stream().mapToInt(ld -> ld).sum();
    int count = lds.size();
    double mean = (double) sum / count;
    int q1 = lds.get(count / 4);
    int median = lds.get(count / 2);
    int q3 = lds.get(count / 4 * 3);
    int mode = lds.stream().collect(groupingBy(ld -> ld, Collectors.counting())).entrySet().stream()
        .max(Entry.comparingByValue()).orElseThrow(() -> new RuntimeException("no data")).getKey();
    int max = lds.stream().mapToInt(ld -> ld).max().orElse(0);
    int min = lds.stream().mapToInt(ld -> ld).min().orElse(0);
    int range = max - min;
    double sd = Calculate.standardDeviation(lds.stream().mapToDouble(ld -> ld).toArray());
    int iqr = q3 - q1;

    System.out.println("Mean: " + mean);
    System.out.println("Median: " + median);
    System.out.println("Mode: " + mode);
    System.out.println("Range: " + range);
    System.out.println("Minimum: " + min);
    System.out.println("Maximum: " + max);
    System.out.println("Count: " + count);
    System.out.println("Sum: " + sum);
    System.out.println("Standard Deviation: " + sd);
    System.out.println("Q1 -> " + q1);
    System.out.println("Q2 -> " + median);
    System.out.println("Q3 -> " + q3);

    double outlierLimit = q3 + iqr * 1.5;
    System.out.println("Outliers: \n" + lds.stream().distinct().filter(ld -> ld > outlierLimit)
        .map(String::valueOf).collect(joining(",")));

    String outliers = "Isa " + stats.stream().filter(s -> s.ld >= 21).filter(s -> !s.dss.isEmpty())
        .map(s -> s.chapter + ":" + s.verse).collect(joining(","));
    System.out.printf("%s/#/read_the_word?reference=%s&lang=interlaced\n",
        BASE_URL, outliers.replaceAll(" ", "%20"));

    stats.stream().filter(s -> s.dss.isEmpty())
        .forEach(s -> System.out.printf("Missing %s %d:%d\n", "Isaiah", s.chapter, s.verse));

    BufferedWriter bw = new BufferedWriter(new FileWriter("data/isa-diff.csv"));
    bw.write("Scripture,DSS,WLCO,LD,MT Missing,DSS Missing");
    for (char l = 'א'; l <= 'ת'; l++) {
      bw.write(",MT Miss " + l);
    }
    for (char l = 'א'; l <= 'ת'; l++) {
      bw.write(",DSS Miss " + l);
    }
    bw.newLine();

    bw.write(
        String.format("%d,%d,%d,%d,%d,%d",
            stats.size(),
            stats.stream().filter(s -> !s.dss.isEmpty()).count(),
            stats.stream().filter(s -> !s.wlco.isEmpty()).count(),
            sum,
            stats.stream().filter(s -> !s.lettersRemoved.isEmpty()).count(),
            stats.stream().filter(s -> !s.lettersAdded.isEmpty()).count()));

    for (char l = 'א'; l <= 'ת'; l++) {
      bw.write("," + removed.getOrDefault(l, new AtomicLong()));
    }
    for (char l = 'א'; l <= 'ת'; l++) {
      bw.write("," + added.getOrDefault(l, new AtomicLong()));
    }
    bw.newLine();
    for (ScriptureDiffStats s : stats) {
      bw.write(
          String.format("%s %d:%d,%s,%s,%d,%s,%s",
              s.book, s.chapter, s.verse, s.dss, s.wlco, s.ld, s.removed, s.added));
      for (char l = 'א'; l <= 'ת'; l++) {
        bw.write("," + s.lettersRemoved.getOrDefault(l, 0L));
      }
      for (char l = 'א'; l <= 'ת'; l++) {
        bw.write("," + s.lettersAdded.getOrDefault(l, 0L));
      }
      bw.newLine();
    }
    bw.close();

    for (int c = 1; c <= 66; c++) {
      scriptureStore.getScriptures("DSS", "he", "Isa " + c).getItems().stream()
          .filter(s -> {
            try {
              return scriptureStore.getScriptures(
                  "DSS", "en", String.format("Isa %d:%d", s.getChapter(), s.getVerse())) == null;
            } catch (Exception e) {
              return true;
            }
          })
          .forEach(
              s -> System.out.printf("Missing DSS en: Isa %d:%d\n", s.getChapter(), s.getVerse()));
    } */

    statsProcessor.printWordStats("WLCO");
    statsProcessor.printWordStats("DSS");

    // statsProcessor.fillDssWords();

    ImmutableList<Interlinear> allInterlinears = IntStream.range(1, 67).boxed()
        .flatMap(c -> interlinearStore.getInterlinear("Isa " + c).stream())
        .collect(toImmutableList());

    Map<String, Long> strongIds = allInterlinears.stream()
        .map(Interlinear::getStrongsId)
        .filter(Objects::nonNull)
        .collect(groupingBy(identity(), counting()));
    System.out.printf("%d unique Strong's Ids\n", strongIds.size());

    strongIds.entrySet().stream().sorted(Entry.comparingByValue(reverseOrder()))
        .limit(21)
        .forEach(e -> {
          Lexicon lexicon = lexiconStore.get(e.getKey());
          System.out.printf("%s %s %s %d\n",
              e.getKey(), lexicon.getConstantsOnly(), lexicon.translation(), e.getValue());
        });

    Map<String, Map<String, List<Interlinear>>> wordsByStrongIds = allInterlinears.stream()
        .filter(i -> i.getStrongsId() != null)
        .collect(groupingBy(Interlinear::getConstantsOnly, groupingBy(Interlinear::getStrongsId)));

    BufferedWriter bw = new BufferedWriter(new FileWriter("data/isa-overrides.csv"));
    bw.write("Hebrew Word"
        + ",First Strongs,DSS Word,First Link,First Count"
        + ",Second Strongs,DSS Word,Second Link,Second Count"
        + ",Third Strongs,DSS Word,Third Link,Third Count"
        + ",Fourth Strongs,DSS Word,Fourth Link,Fourth Count"
        + ",Fifth Strongs,DSS Word,Fifth Link,Fifth Count\n");

    wordsByStrongIds.entrySet().stream().filter(e -> e.getValue().size() > 1).sorted(Entry.comparingByKey()).forEach(e -> {
      try {
        bw.write(e.getKey());
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
      e.getValue().entrySet().stream()
          .sorted(comparing(i -> i.getValue().size(), reverseOrder()))
          .forEach(i -> {
            try {
              bw.write(
                  String.format(",%s,%s,%s,%d", i.getKey(), statsProcessor.getDssWord(i.getValue()),
                      createLink(i.getValue()), i.getValue().size()));
            } catch (IOException ioe) {
              throw new RuntimeException(ioe);
            }
          });
      try {
        bw.write("\n");
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    });
    bw.close();

    BufferedWriter bw2 = new BufferedWriter(new FileWriter("data/isa-interlinear.csv"));
    bw2.write(
        "Id,Strong's Id,MT Word,Transliteration,Hebrew Word,DSS Word,Change,Translation,Word Type,Link\n");
    allInterlinears
        // .stream().sorted(comparing(i -> String.format("%s-%s", i.getStrongsId(), i.getConstantsOnly())))
        .stream().filter(i -> "-".equals(i.getTranslation()) && i.getStrongsId() == null && i.getMorphology() == null)
        .forEach(i -> {
          try {
            bw2.write(
                String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    i.getId(), sanitize(i.getStrongsId()), i.getWord(), sanitize(i.getTransliteration()),
                    i.getConstantsOnly(), i.getDss(), i.getConstantsOnly().equals(i.getDss()) ? "" : "TRUE",
                    i.getTranslation(), sanitize(i.getMorphology()),
                    createLink(ImmutableList.of(i))));
          } catch (IOException ioe) {
            throw new RuntimeException(ioe);
          }
        });
    bw2.close();

    BufferedWriter bw3 = new BufferedWriter(new FileWriter("data/isa-words.csv"));
    bw3.write("Strong's Id,MT Word,Hebrew Word,DSS Word,Translation,Transliteration,Change,Link,Count\n");
    allInterlinears.stream()
        .collect(groupingBy(i -> String.format("%s-%s", i.getStrongsId(), i.getConstantsOnly())))
        .entrySet().stream()
        .sorted(comparing(e -> String.format("%s-%s", e.getValue().get(0).getStrongsId(), e.getValue().get(0).getConstantsOnly())))
        .map(Entry::getValue).forEach(is -> {
          Interlinear i = is.get(0);
          String dssWord = statsProcessor.getDssWord(is);
          try {
            bw3.write(
                String.format("%s,%s,%s,%s,%s,%s,%s,%s,%d\n", i.getStrongsId(), i.getWord(),
                    i.getConstantsOnly(), dssWord, i.getTranslation(), sanitize(i.getTransliteration()),
                    i.getConstantsOnly().equals(dssWord) ? "" : "TRUE", createLink(is), is.size()));
          } catch (IOException ioe) {
            throw new RuntimeException(ioe);
          }
        });
    bw3.close();

    dao.saveToFile();
    System.out.printf("Total time: %3.2f seconds", (System.currentTimeMillis() - startTime) / 1000.0);
  }

  private static String createLink(Iterable<Interlinear> interlinears) {
    return String.format("%s/#/read_the_word?highlight=%s&reference=Isa%%20%s&lang=interlinear",
        Constants.BASE_URL, interlinears.iterator().next().getStrongsId(),
        stream(interlinears).limit(7).map(s -> s.getChapter() + ":" + s.getVerse()).collect(joining(";")));
  }

  private static String sanitize(String input) {
    return input == null ? "" : input.replaceAll(",", ";");
  }

  private String getDssWord(Iterable<Interlinear> interlinears) {
    return stream(interlinears).map(Interlinear::getDss).filter(Objects::nonNull)
        .collect(groupingBy(identity(), counting()))
        .entrySet().stream().min(Entry.comparingByValue(reverseOrder())).map(Entry::getKey).orElse("");
  }

  private static class ScriptureDiffStats {
    private final String book;
    private final int chapter;
    private final int verse;
    private final String dss;
    private final String wlco;
    private final String removed;
    private final Map<Character, Long> lettersRemoved;
    private final String added;
    private final Map<Character, Long> lettersAdded;
    private final int ld;
    private final int percentMatch;

    public ScriptureDiffStats(String book, int chapter, int verse, String dss, String wlco) {
      this.book = book;
      this.chapter = chapter;
      this.verse = verse;
      this.dss = dss;
      this.wlco = wlco;

      ld = Calculate.LD(dss, wlco);
      ImmutableList<Diff> diffs = Calculate.getDiff(dss, wlco);

      removed = diffs.stream()
          .filter(d -> d.operation == Operation.DELETE).map(d -> d.text).collect(joining(" "));
      lettersRemoved = diffs.stream()
          .filter(d -> d.operation == Operation.DELETE)
          .flatMapToInt(d -> d.text.chars())
          .mapToObj(i -> (char) i)
          .collect(groupingBy(identity(), counting()));
      added = diffs.stream()
          .filter(d -> d.operation == Operation.INSERT).map(d -> d.text).collect(joining(" "));
      lettersAdded = diffs.stream()
          .filter(d -> d.operation == Operation.INSERT)
          .flatMapToInt(d -> d.text.chars())
          .mapToObj(i -> (char) i)
          .collect(groupingBy(identity(), counting()));
      percentMatch = dss.isEmpty() ? 0 : (dss.length() - ld) * 100 / dss.length();
    }

    public int getLd() {
      return ld;
    }

    public int getPercentMatch() {
      return percentMatch;
    }

    public static ScriptureDiffStats of(
        String name, int chapter, int verse, String original, String revised, boolean ignorePun) {
      if (ignorePun) {
        original = HebrewConverter.removePunctuation(original);
        revised = HebrewConverter.removePunctuation(revised);
      }

      return new ScriptureDiffStats(name, chapter, verse, original, revised);
    }
  }
}

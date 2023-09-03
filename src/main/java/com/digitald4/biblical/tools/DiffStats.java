package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import com.digitald4.biblical.util.ScriptureFetcherBibleGateway;
import com.digitald4.biblical.util.ScriptureFetcherBibleHub;
import com.digitald4.biblical.util.ScriptureFetcherJWOrg;
import com.digitald4.biblical.util.ScriptureFetcherKJV1611;
import com.digitald4.biblical.util.ScriptureFetcherOneOff;
import com.digitald4.biblical.util.ScriptureFetcherPseudepigrapha;
import com.digitald4.biblical.util.ScriptureFetcherRouter;
import com.digitald4.biblical.util.ScriptureFetcherSefariaOrg;
import com.digitald4.biblical.util.ScriptureFetcherStepBibleOrg;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOApiImpl;
import com.digitald4.common.storage.DAOInMemoryImpl;
import com.digitald4.common.util.Calculate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Diff;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;

public class DiffStats {
  private final static String BASE_URL = "https://dd4-biblical.appspot.com";
  private final static String API_URL = BASE_URL + "/_api";
  private final static String API_VERSION = "v1";
  private final static String DB_FILE = "data/Isaiah-Hebrew.db";

  private final ScriptureStore scriptureStore;

  public DiffStats(ScriptureStore scriptureStore) {
    this.scriptureStore = scriptureStore;
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

  public static void main(String[] args) throws IOException {
    APIConnector apiConnector = new APIConnector(API_URL, API_VERSION, 100).setIdToken("39284720");
    DAOInMemoryImpl dao = new DAOInMemoryImpl().loadFromFile(DB_FILE);
    StaticDataDAO staticDataDAO = new StaticDataDAO();
    BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
    ScriptureStore scriptureStore = new ScriptureStore(
        () -> dao, null, bibleBookStore,
        new ScriptureReferenceProcessorSplitImpl(bibleBookStore),
        new ScriptureFetcherRouter(
            new ScriptureFetcherBibleGateway(apiConnector),
            new ScriptureFetcherBibleHub(apiConnector),
            new ScriptureFetcherJWOrg(apiConnector),
            new ScriptureFetcherKJV1611(apiConnector),
            new ScriptureFetcherOneOff(apiConnector),
            new ScriptureFetcherPseudepigrapha(apiConnector),
            new ScriptureFetcherSefariaOrg(apiConnector),
            new ScriptureFetcherStepBibleOrg(apiConnector)));

    scriptureStore.getScriptures("DSS", "he", "Isa 64:1");

    ImmutableList<ScriptureDiffStats> stats = new DiffStats(scriptureStore).process(true);
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

    dao.saveToFile(DB_FILE);

    String url = "%s/search?lang=en&pageSize=50&pageToken=1&searchText=Isa+%d&version=DSS";
    String baseUrl = apiConnector.formatUrl("scriptures");
    for (int c = 1; c <= 66; c++) {
      JSONArray scriptures = new JSONObject(apiConnector.sendGet(String.format(url, baseUrl, c)))
          .getJSONArray("items");
      HashSet<Integer> enVerses = new HashSet<>();
      for (int i = 0; i < scriptures.length(); i++) {
        enVerses.add(scriptures.getJSONObject(i).getInt("verse"));
      }
      scriptureStore.getScriptures("DSS", "he", "Isa " + c).getItems().stream()
          .filter(s -> !enVerses.contains(s.getVerse()))
          .forEach(
              s -> System.out.printf("Missing DSS en: Isa %d:%d\n", s.getChapter(), s.getVerse()));
    }

    /* DAOApiImpl apiDao = new DAOApiImpl(apiConnector);
    ScriptureStore scriptureStore1 =
        new ScriptureStore(() -> apiDao, null, new ScriptureReferenceProcessorSplitImpl(), null);
    scriptureStore1.create(
        scriptureStore1.getScriptures("DSS", "en", "Isa 1").getItems().stream()
            .peek(s -> s.setVersion("qumran"))
            .collect(toImmutableList())); */
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
          .mapToObj(i -> (char) i).collect(groupingBy(identity(), counting()));
      added = diffs.stream()
          .filter(d -> d.operation == Operation.INSERT).map(d -> d.text).collect(joining(" "));
      lettersAdded = diffs.stream()
          .filter(d -> d.operation == Operation.INSERT)
          .flatMapToInt(d -> d.text.chars())
          .mapToObj(i -> (char) i).collect(groupingBy(identity(), counting()));
      percentMatch = dss.isEmpty() ? 0 : ((dss.length() - ld) * 100 / dss.length());
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
        revised = revised.replaceAll("־", " ");
        if (revised.indexOf("׃") > 0) {
          revised = revised.substring(0, revised.indexOf("׃")).trim();
        }
      }

      return new ScriptureDiffStats(name, chapter, verse, original, revised);
    }
  }
}

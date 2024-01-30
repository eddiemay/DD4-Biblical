package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static java.util.Comparator.comparing;

import com.digitald4.biblical.model.AncientLexicon;
import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.LexiconStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import com.digitald4.biblical.util.AncientLexiconFetcher;
import com.digitald4.biblical.util.BPETokenizer;
import com.digitald4.biblical.util.Constants;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.biblical.util.InterlinearFetcher;
import com.digitald4.biblical.util.LexiconFetcher;
import com.digitald4.biblical.util.LexiconFetcherBlueLetterImpl;
import com.digitald4.biblical.util.ScriptureFetcherBibleHub;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOApiImpl;
import com.digitald4.common.storage.DAOFileBasedImpl;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONObject;

public class LexiconTool {
  private final static String BASE_URL = "https://dd4-biblical.appspot.com/";
  private final static String API_URL = BASE_URL + "_api";
  private final static String API_VERSION = "v1";
  private final static String URL = "%s/%s?startIndex=%d&endIndex=%d&lang=%s";
  private final static String INTER_REINDEX_URL = "%s/reindexInterlinear?book=%s&chapter=%d";
  private final static String INTER_MIGRATE_URL = "%s/migrateInterlinear?book=%s&chapter=%d";
  private final static String INTER_DELETE_URL = "%s/deleteInterlinear?book=%s&chapter=%d";
  private final static String INTER_FETCH_URL = "%s/search?lang=interlinear&searchText=%s+%d:1";
  private final APIConnector apiConnector;
  private final LexiconStore lexiconStore;
  private final LexiconFetcher lexiconFetcher;
  private final InterlinearStore interlinearStore;
  private final InterlinearFetcher interlinearFetcher;
  private final BibleBookStore bibleBookStore;
  private final AncientLexiconFetcher ancientLexiconFetcher;

  LexiconTool(APIConnector apiConnector, LexiconStore lexiconStore, LexiconFetcher lexiconFetcher,
      InterlinearStore interlinearStore, InterlinearFetcher interlinearFetcher,
      BibleBookStore bibleBookStore, AncientLexiconFetcher ancientLexiconFetcher) {
    this.apiConnector = apiConnector;
    this.lexiconStore = lexiconStore;
    this.lexiconFetcher = lexiconFetcher;
    this.interlinearStore = interlinearStore;
    this.interlinearFetcher = interlinearFetcher;
    this.bibleBookStore = bibleBookStore;
    this.ancientLexiconFetcher = ancientLexiconFetcher;
  }

  public void reindexLexicon(String language, int startIndex, int endIndex) {
    String baseUrl = apiConnector.formatUrl("lexicons");
    System.out.printf("Reindexing: %d-%d...\n", startIndex, endIndex);
    apiConnector.sendGet(String.format(URL, baseUrl, "reindex", startIndex, endIndex, language));
  }

  public void migrateLexicon(String language, int startIndex, int endIndex) {
    String baseUrl = apiConnector.formatUrl("lexicons");
    System.out.printf("Migrating: %s%d-%s%d...\n", language, startIndex, language, endIndex);
    apiConnector.sendGet(
        String.format(URL, baseUrl, "migrateLexicon", startIndex, endIndex, language));
  }

  private void reindexInterlinear(String startBook, String endBook) {
    BibleBook start = bibleBookStore.get(startBook);
    BibleBook stop = bibleBookStore.get(endBook);
    bibleBookStore.getAllBooks().stream()
        .filter(book -> book.getNumber() >= start.getNumber() && book.getNumber() <= stop.getNumber())
        .forEach(book ->
            range(1, book.getChapterCount() + 1)
                .forEach(chapter -> reindexInterlinear(book.name(), chapter)));
  }

  public void fetchInterlinear(BibleBook bibleBook, int chapter) {
    String baseUrl = apiConnector.formatUrl("scriptures");
    System.out.printf("Fetching: %s %d:1...\n", bibleBook, chapter);
    apiConnector.sendGet(String.format(INTER_FETCH_URL, baseUrl, bibleBook, chapter));
  }

  public void reindexInterlinear(String book, int chapter) {
    String baseUrl = apiConnector.formatUrl("lexicons");
    System.out.printf("Reindexing: %s %d...\n", book, chapter);
    apiConnector.sendGet(String.format(INTER_REINDEX_URL, baseUrl, book, chapter));
  }

  public void migrateInterlinear(String bibleBook, int chapter) {
    String baseUrl = apiConnector.formatUrl("lexicons");
    System.out.printf("Migrating: %s %d...\n", bibleBook, chapter);
    apiConnector.sendGet(String.format(INTER_MIGRATE_URL, baseUrl, bibleBook, chapter));
  }

  public int deleteInterlinear(String book, int chapter) {
    String baseUrl = apiConnector.formatUrl("lexicons");
    System.out.printf("Deleting: %s %d...\n", book, chapter);
    return Integer.parseInt(apiConnector.sendGet(String.format(INTER_DELETE_URL, baseUrl, book, chapter)).trim());
  }

  public void printInterlinear(String scripture) {
    AtomicReference<String> currentScripture = new AtomicReference<>();
    interlinearFetcher.fetchInterlinear(new BibleBook().setName("Ezra"), 10)
    //interlinearStore.getInterlinear(scripture)
        .forEach(inter -> {
          String script =
              String.format("%s %d:%d", inter.getBook(), inter.getChapter(), inter.getVerse());
          if (!script.equals(currentScripture.get())) {
            System.out.println("\n" + script);
            currentScripture.set(script);
          }
          System.out.printf("%s | %s | %s | %s\n", inter.getWord(),
              inter.getTransliteration(), inter.getTranslation(), inter.getStrongsId());
        });
  }

  private String readData(String type, String value) throws IOException {
    String fileName = String.format("data/%s-%s.json", type, value);
    try {
      BufferedReader br = new BufferedReader(new FileReader(fileName));
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        builder.append(line);
      }
      return builder.toString();
    } catch (FileNotFoundException e) {
      System.out.println("File not found, reading from api...");
    }

    String baseUrl = apiConnector.formatUrl("lexicons");
    String url = String.format("%s/getReferences?%s=%s&pageSize=7000", baseUrl, type, value);
    System.out.println(url);
    String response = apiConnector.sendGet(url);
    BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
    bw.write(response);
    bw.close();

    return response;
  }

  private static String getTranslationText(ImmutableList<Interlinear> interlinears, int index) {
    return range(Math.max(0, index - 3), Math.min(index + 2, interlinears.size()))
        .mapToObj(interlinears::get)
        .map(Interlinear::getTranslation)
        .collect(joining(" "));
  }

  private static String getHebrewText(ImmutableList<Interlinear> interlinears, int index) {
    return range(Math.max(0, index - 3), Math.min(index + 2, interlinears.size()))
        .mapToObj(interlinears::get)
        .map(Interlinear::getConstantsOnly)
        .collect(joining(" "));
  }

  private ImmutableSet<Interlinear> getReferences(String type, String... values)
      throws IOException {
    ImmutableSet.Builder<Interlinear> results = ImmutableSet.builder();
    for (String value : values) {
      JSONObject queryResult = new JSONObject(readData(type, value));
      System.out.printf("Found %d items\n", queryResult.getInt("totalSize"));
      JSONArray resultArray = queryResult.getJSONArray("items");
      results.addAll(
          range(0, resultArray.length())
              .mapToObj(x ->
                  JSONUtil.toObject(Interlinear.class, resultArray.getJSONObject(x).toString()))
              .collect(toImmutableList()));
    }

    return results.build();
  }

  private void outputReferences(String name, String type, String... values) throws IOException {
    outputResults(name, getReferences(type, values));
  }

  private void outputResults(String name, Iterable<Interlinear> results) throws IOException {
    BufferedWriter bw =
        new BufferedWriter(new FileWriter(String.format("data/references-%s.csv", name)));
    bw.write(
        "Scripture,Word,Strong's Id,Hebrew Only,Translation,Prior Morph,Morph,Post Morph,Link,Hebrew,KJV\n");
    for (Interlinear result : results) {
      ImmutableList<Interlinear> interlinears = interlinearStore.getInterlinear(result.reference());
      int index = result.getIndex();
      // System.out.println(result.getId());
      bw.write(
          String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
              result.reference(), result.getWord(), result.getStrongsId(),
              result.getConstantsOnly(),
              result.getTranslation(),
              getMorphology(interlinears, index - 1),
              getMorphology(result),
              getMorphology(interlinears, index + 1),
              String.format("%s/#/read_the_word?reference=%s&lang=interlinear",
                  BASE_URL, result.reference().replaceAll(" ", "%20")),
              getHebrewText(interlinears, index),
              getTranslationText(interlinears, index)));
    }
    bw.close();
  }

  private static String getMorphology(ImmutableList<Interlinear> interlinears, int index) {
    return (index < 1 || index >= interlinears.size())
        ? "" : getMorphology(interlinears.get(index - 1));
  }

  private static String getMorphology(Interlinear interlinear) {
    String mprph = interlinear.getMorphology();
    if (mprph == null) {
      return "";
    }

    return mprph.replaceAll(",", "|");
  }

  private ImmutableList<Lexicon> getLexicons(int batch) {
    int start = batch * 1000;
    int end = (batch + 1) * 1000;
    Query.List query = Query.forList(
        Filter.of("strongsId", ">=", String.format("H%04d", start)),
        Filter.of("strongsId", "<", String.format("H%04d", end)));
    query.setPageSize(1000);

    ImmutableList<Lexicon> lexicons = lexiconStore.list(query).getItems();
    if (lexicons.isEmpty()) {
      System.out.printf("fetching lexicons: H%d -> H%d\n",start, end);
      DAOApiImpl apiDao = new DAOApiImpl(apiConnector);
      LexiconStore apiStore = new LexiconStore(() -> apiDao, null);
      lexicons = lexiconStore.create(apiStore.list(query).getItems());
    }

    return lexicons;
  }

  private ImmutableList<AncientLexicon> getAncientLexicons(int batch) {
    return ancientLexiconFetcher.fetch(batch);
  }

  private void outputLexiconJSON() throws IOException {
    ImmutableList<Lexicon> lexicons = range(0, 9).boxed()
        .flatMap(batch -> getLexicons(batch).stream())
        .collect(toImmutableList());
    try (BufferedWriter bw = new BufferedWriter(new FileWriter("src/main/webapp/ml/heb_vocab_lexicon_strongs.txt"))) {
      lexicons.stream()
          .map(TokenWord::from)
          .sorted(comparing(TokenWord::getStrongsId))
          .map(JSONObject::new)
          .forEach(json -> {
            try {
              bw.write(json + "\n");
            } catch (IOException ioe) {
              throw new DD4StorageException("Error writing value: " + json, ioe);
            }
          });
    }
  }

  private void outputAncientLexiconJSON() throws IOException {
    ImmutableList<AncientLexicon> ancientLexicons = range(0, 23).boxed()
        .flatMap(batch -> getAncientLexicons(batch).stream())
        .collect(toImmutableList());
    try (BufferedWriter bw = new BufferedWriter(new FileWriter("src/main/webapp/ml/heb_vocab_lexicon_ancient.txt"))) {
      ancientLexicons.stream()
          .flatMap(ancientLexicon -> TokenWord.from(ancientLexicon).stream())
          .sorted(comparing(TokenWord::getStrongsId))
          .map(JSONObject::new)
          .forEach(json -> {
            try {
              bw.write(json + "\n");
            } catch (IOException ioe) {
              throw new DD4StorageException("Error writing value: " + json, ioe);
            }
          });
    }
  }

  private static void outputLexiconOverridesJSON() throws IOException {
    ImmutableSet<String> declared = Constants.VOCAB_FILES.stream()
        .filter(file -> !file.equals("heb_vocab_overrides.txt"))
        .flatMap(file -> TranslationTool.loadFromFile(file).stream())
        .map(JSONObject::new)
        .map(JSONObject::toString)
        .collect(toImmutableSet());

    ImmutableList<TokenWord> overrides = TranslationTool.loadFromFile("heb_vocab_overrides.txt");

    try (BufferedWriter bw = new BufferedWriter(new FileWriter("src/main/webapp/ml/heb_vocab_overrides.txt"))) {
      overrides
          .stream()
          .sorted(comparing(TokenWord::getStrongsId))
          .map(JSONObject::new)
          .map(JSONObject::toString)
          // .filter(json -> !declared.contains(json))
          .forEach(json -> {
            try {
              bw.write(json + "\n");
            } catch (IOException ioe) {
              throw new DD4StorageException("Error writing value: " + json, ioe);
            }
          });
    }
  }

  private void outputLexiconCSV() {
    System.out.println("[");
    range(0, 9).boxed().flatMap(batch -> getLexicons(batch).stream())
        .map(lexicon -> new JSONObject()
            .put("hebrew", lexicon.getConstantsOnly())
            .put("strongsId", lexicon.getId())
            .put("translation", lexicon.translation())
            .put("transliteration", lexicon.getTransliteration())
            .put("partOfSpeach", lexicon.getPartOfSpeech())
            .put("pronunciation", lexicon.getPronunciation()))
        .forEach(json -> System.out.printf("  %s\n", json));
    System.out.println("]");
  }

  public static void findRoots(Iterable<Interlinear> interlinears) {
    BPETokenizer bpeTokenizer = new BPETokenizer();
    stream(interlinears).map(Interlinear::getConstantsOnly).forEach(bpeTokenizer::tokenize);
    System.out.println(bpeTokenizer.getTokens());
  }

  public static void main(String[] args) throws IOException {
    APIConnector apiConnector = new APIConnector(API_URL, API_VERSION, 50);
    apiConnector.setIdToken("716656750");
    StaticDataDAO staticDataDAO = new StaticDataDAO();
    DAOFileBasedImpl fileDao = new DAOFileBasedImpl("data/interlinear-references.db").loadFromFile();
    LexiconFetcher lexiconFetcher = new LexiconFetcherBlueLetterImpl(apiConnector);
    InterlinearFetcher interlinearFetcher = new ScriptureFetcherBibleHub(apiConnector);
    BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
    ScriptureReferenceProcessor referenceProcessor =
        new ScriptureReferenceProcessorSplitImpl(bibleBookStore);
    InterlinearStore interlinearStore =
        new InterlinearStore(() -> fileDao, referenceProcessor, interlinearFetcher);
    DAOFileBasedImpl lexiconDAO = new DAOFileBasedImpl("data/lexicon.db").loadFromFile();
    LexiconStore lexiconStore = new LexiconStore(() -> lexiconDAO, lexiconFetcher);
    AncientLexiconFetcher ancientLexiconFetcher = new AncientLexiconFetcher(apiConnector);
    LexiconTool lexiconTool = new LexiconTool(apiConnector, lexiconStore, lexiconFetcher,
        interlinearStore, interlinearFetcher, bibleBookStore, ancientLexiconFetcher);
    lexiconTool.printInterlinear("Judges 21");

    // System.out.println(lexiconStore.get("H997"));

    findRoots(lexiconTool.getReferences("strongsId", "H410", "H426", "H430", "H433"));
    findRoots(lexiconTool.getReferences("strongsId", "H175"));


    int batchSize = 100;
    // range(0, 7000 / 100)
        // .forEach(s -> lexiconTool.migrateLexicon("G", s * batchSize + 1, (s + 1) * batchSize + 1));
    // lexiconTool.migrateLexicon("G", 6090, 6091);

    // lexiconTool.outputLexiconJSON();
    // lexiconTool.outputAncientLexiconJSON();
    // outputLexiconOverridesJSON();
    /* lexiconStore.list(Query.forList()).getItems().stream()
        .collect(groupingBy(Lexicon::getConstantsOnly)).entrySet().stream()
        .filter(e -> e.getValue().size() > 1)
        .forEach(
            e -> System.out.println(
                e.getKey() + "=" + e.getValue().stream().map(l -> l.getId() + ":" + l.translation()).collect(joining(",")))); */

    // Reindexed without chapter & verse: Gen-2Chr .
    // Reindexed correctly: Esra-Mal, -Psa, -Jer

    // lexiconTool.reindexInterlinear("Ezra", 10);
    // lexiconTool.reindexInterlinear("Eze", "Mal");
    // IntStream.range(51, 151).forEach(chapter -> lexiconTool.reindexInterlinear("Psa", chapter));
    // lexiconTool.reindexInterlinear("Jer", 31);

    Stream.of(bibleBookStore.get("Isa"), bibleBookStore.get("Ezra"))
    /* bibleBookStore.getAllBooks().stream()
        .filter(b -> b.getNumber() > 20 && b.getNumber() < 40
            && !b.name().equals("Daniel") && !b.name().equals("Isaiah")
            && !b.name().equals("Jeremiah") && !b.name().equals("Ezekiel")) */
        .forEach(book -> {
          int end = book.getChapterCount() + 1;
          range(1, end).forEach(c -> lexiconTool.deleteInterlinear(book.name(), c));
          range(1, end).forEach(c -> lexiconTool.reindexInterlinear(book.name(), c));
        });

    /* System.out.println("Total records deleted: " + Stream
        .of(bibleBookStore.get("Matt"), bibleBookStore.get("Luke"), bibleBookStore.get("John"),
            bibleBookStore.get("Heb"), bibleBookStore.get("Rev"))
        .flatMapToInt(book -> range(1, book.getChapterCount() + 1).map(c -> lexiconTool.deleteInterlinear(book.name(), c)))
        .sum()); */

    // lexiconFetcher.fetchInterlinear(bibleBookStore.get("Song of Solomon"), 1);



    // lexiconTool.migrateInterlinear("Judges", 21);

    /* bibleBookStore.getAllBooks().stream().filter(book -> book.getNumber() > 22 && book.getNumber() < 40)
        .forEach(book -> range(book.getChapterCount(), book.getChapterCount() + 1)
            .forEach(c -> lexiconTool.reindexInterlinear(book.name(), c))); */

    // lexiconTool.outputReferences("אל", "hebrewWord", "אל");
    // lexiconTool.outputReferences("No", "strongsId", "H408", "H409", "H3808", "H3809");
    // lexiconTool.outputReferences("God", "strongsId", "H410", "H426", "H430", "H433");
    // lexiconTool.outputReferences("These", "strongsId", "H411", "H412", "H428");
    // lexiconTool.outputReferences("H413", "strongsId", "H413");
    // lexiconTool.outputReferences("Moses", "strongsId", "H4872");

    /* ImmutableSet<Interlinear> references =
        lexiconTool.getReferences("strongsId", "H410", "H426", "H430", "H433");
        // lexiconTool.getReferences("hebrewWord", "אל");
        // lexiconTool.getReferences("strongsId", "H4872");
    System.out.println();
    references.stream()
        .filter(interlinear -> interlinear.getStrongsId() != null)
        .collect(Collectors.groupingBy(Interlinear::getStrongsId, Collectors.counting()))
        .entrySet().stream()
        .sorted(Entry.comparingByValue())
        .forEach(e -> System.out.printf(
            "%s %d %3.1f%%\n", e.getKey(), e.getValue(), (e.getValue() * 100f / references.size())));

    System.out.println();
    references.stream()
        .collect(Collectors.groupingBy(Interlinear::getConstantsOnly, Collectors.counting()))
        .entrySet().stream()
        .sorted(Entry.comparingByValue())
        .forEach(e ->
            System.out.printf("%s %d %3.1f%%\n",
                e.getKey(), e.getValue(), (e.getValue() * 100f / references.size()))); */

    fileDao.saveToFile();
    lexiconDAO.saveToFile();
  }
}

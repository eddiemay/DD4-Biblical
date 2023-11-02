package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Lexicon.Interlinear;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import com.digitald4.biblical.util.LexiconFetcher;
import com.digitald4.biblical.util.LexiconFetcherBlueLetterImpl;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOFileBasedImpl;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONObject;

public class LexiconTool {
  private final static String BASE_URL = "https://dd4-biblical.appspot.com/";
  private final static String API_URL = BASE_URL + "_api";
  private final static String API_VERSION = "v1";
  private final static String URL = "%s/%s?startIndex=%d&endIndex=%d&language=%s";
  private final static String INTER_REINDEX_URL = "%s/reindexInterlinear?book=%s&chapter=%d";
  private final static String INTER_FETCH_URL = "%s/search?lang=interlinear&searchText=%s+%d:1";
  private final APIConnector apiConnector;
  private final InterlinearStore interlinearStore;
  private final BibleBookStore bibleBookStore;

  LexiconTool(
      APIConnector apiConnector, InterlinearStore interlinearStore, BibleBookStore bibleBookStore) {
    this.apiConnector = apiConnector;
    this.interlinearStore = interlinearStore;
    this.bibleBookStore = bibleBookStore;
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

  public void reindexInterlinear(String book, int chapter) {
    String baseUrl = apiConnector.formatUrl("lexicons");
    System.out.printf("Reindexing: %s %d...\n", book, chapter);
    apiConnector.sendGet(String.format(INTER_REINDEX_URL, baseUrl, book, chapter));
  }

  public void fetchInterlinear(BibleBook bibleBook, int chapter) {
    String baseUrl = apiConnector.formatUrl("scriptures");
    System.out.printf("Fetching: %s %d:1...\n", bibleBook, chapter);
    apiConnector.sendGet(String.format(INTER_FETCH_URL, baseUrl, bibleBook, chapter));
  }

  public void printInterlinear(String scripture) {
    AtomicReference<String> currentScripture = new AtomicReference<>();
    interlinearStore.getInterlinear(scripture)
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

  public static void main(String[] args) throws IOException {
    APIConnector apiConnector = new APIConnector(API_URL, API_VERSION, 100);
    StaticDataDAO staticDataDAO = new StaticDataDAO();
    DAOFileBasedImpl fileDao = new DAOFileBasedImpl("data/interlinear-references.db").loadFromFile();
    LexiconFetcher lexiconFetcher = new LexiconFetcherBlueLetterImpl(apiConnector, true);
    BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
    ScriptureReferenceProcessor referenceProcessor =
        new ScriptureReferenceProcessorSplitImpl(bibleBookStore);
    InterlinearStore interlinearStore =
        new InterlinearStore(() -> fileDao, referenceProcessor, lexiconFetcher);
    LexiconTool lexiconTool = new LexiconTool(apiConnector, interlinearStore, bibleBookStore);
    // lexiconTool.printInterlinear("Gen 1:1-2,2:2-3");
    int batchSize = 100;
    for (int x = 6000; x < 6001; x++) {
      System.out.println(lexiconFetcher.getLexicon("G" + x));
    }

    /* for (int s = 6075; s < 6100; s += batchSize) {
      // lexiconTool.migrateLexicon("H", s, s + batchSize);
      lexiconTool.migrateLexicon("G", s, s + batchSize);
    } */

    // Reindexed without chapter & verse: Gen-2Chr .
    // Reindexed correctly: Esra-Mal, -Psa, -Jer
    // lexiconTool.reindexInterlinear("Eze", "Mal");
    // IntStream.range(51, 151).forEach(chapter -> lexiconTool.reindexInterlinear("Psa", chapter));
    // lexiconTool.reindexInterlinear("Jer", 31);

    Stream.of(bibleBookStore.get("Num"), bibleBookStore.get("Proverbs")).forEach(book ->
        range(1, book.getChapterCount()).forEach(c -> lexiconTool.reindexInterlinear(book.name(), c)));

    // lexiconTool.outputReferences("אל", "hebrewWord", "אל");
    // lexiconTool.outputReferences("No", "strongsId", "H408", "H409", "H3808", "H3809");
    // lexiconTool.outputReferences("God", "strongsId", "H410", "H426", "H430", "H433");
    // lexiconTool.outputReferences("These", "strongsId", "H411", "H412", "H428");
    // lexiconTool.outputReferences("H413", "strongsId", "H413");
    // lexiconTool.outputReferences("Moses", "strongsId", "H4872");

    ImmutableSet<Interlinear> references =
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
                e.getKey(), e.getValue(), (e.getValue() * 100f / references.size())));

    fileDao.saveToFile();
  }
}

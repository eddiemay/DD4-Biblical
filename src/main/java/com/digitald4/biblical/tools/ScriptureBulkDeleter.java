package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.ScriptureVersion;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.util.Constants;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOApiImpl;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.QueryResult;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ScriptureBulkDeleter {
  private final static String VERSE_COUNT_URL = "%s/verseCount?version=%s&language=%s&book=%s&chapter=%d";
  private final static String ID_FORMAT = "%s-%s-%s-%d-%d";
  private final APIConnector apiConnector;
  private final DAO dao;
  private final BibleBookStore bibleBookStore;

  public ScriptureBulkDeleter(APIConnector apiConnector, DAO dao) {
    this.apiConnector = apiConnector;
    this.dao = dao;
    bibleBookStore = new BibleBookStore(() -> dao);
  }

  public void delete(BibleBook startBook) {
    String booksBaseUrl = apiConnector.formatUrl("books");
    bibleBookStore.getAllBooks().stream()
        .filter(book -> startBook == null || book.getNumber() >= startBook.getNumber())
        .forEach(book -> {
          String bookName = book.name();
          System.out.printf("\n%s %d =>", book.name(), book.getChapterCount());
          IntStream.range(1, book.getChapterCount() + 1).forEach(chapter -> {
            int verses = Integer.parseInt(
                apiConnector.sendGet(
                    String.format(
                        VERSE_COUNT_URL, booksBaseUrl, "Sefaria", "en", book, chapter)).trim());
            System.out.printf("\n  %d:%d", chapter, verses);
            if (verses == 0) {
              return;
            }
            ScriptureVersion.ALL_VERSIONS.stream()
                .map(ScriptureVersion::getVersion)
                .filter(version -> bibleBookStore.meetsCriteria(version, book, null))
                .peek(version -> System.out.printf(" %s", version))
                .forEach(version ->
                    dao.delete(Scripture.class,
                        IntStream.range(1, verses + 1)
                            .boxed()
                            .flatMap(
                                verse ->
                                    Stream.of("en", "he").map(language ->
                                        String
                                            .format(ID_FORMAT,
                                                version, language, bookName, chapter, verse)
                                            .replaceAll(" ", "_")))
                            .peek(System.out::println)
                            .collect(toImmutableList())));
          });
        });
    System.out.println();
  }

  public ScriptureBulkDeleter deleteDepcreatedVersions() {
    ScriptureVersion.ALL_VERSIONS.stream()
        .filter(version -> version.getTags().isEmpty())
        .forEach(version -> {
          QueryResult<Scripture> queryResult =
              dao.list(Scripture.class, Query.forList().setFilters(Filter.of("version", version)));
          System.out.printf("%d scriptures to delete for %s\n", queryResult.getTotalSize(), version);
          int d = 0;
          while (queryResult.getTotalSize() > 0) {
            System.out.printf("Deleting %d to %d of %d\n", (d + 1), d + queryResult.getItems().size(), queryResult.getTotalSize());
            d += dao.delete(
                Scripture.class,
                queryResult.getItems().stream().map(Scripture::getId).collect(toImmutableList()));
            queryResult =
                dao.list(Scripture.class, Query.forList().addFilter(Filter.of("version", version)));
          }

        });
    return this;
  }

  public static void main(String[] args) {
    String version = null;
    String language = null;
    String idToken = "750425853";
    for (int a = 0; a < args.length; a++) {
      if (args[a].isEmpty()) {
        break;
      }
      switch (args[a]) {
        case "--version": version = args[++a]; break;
        case "--language": language = args[++a]; break;
        case "--idToken": idToken = args[++a]; break;
      }
    }
    APIConnector apiConnector =
        new APIConnector(Constants.API_URL, Constants.API_VERSION, 100).loadIdToken();
    DAOApiImpl dao = new DAOApiImpl(apiConnector);
    new ScriptureBulkDeleter(apiConnector, dao).deleteDepcreatedVersions();
        // .delete(args.length < 1 || args[0].isEmpty() ? null : BibleBook.get(args[0]));
    /* dao.delete(Scripture.class,
        IntStream.range(1, 31)
            .mapToObj(verse -> "KJV1611-Sirach-100-" + verse)
            .collect(toImmutableList()));  */
  }
}

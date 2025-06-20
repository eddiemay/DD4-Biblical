package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;

import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.util.Constants;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.ChangeTracker;
import com.digitald4.common.storage.DAOFileDBImpl;
import com.google.common.collect.ImmutableSet;

public class ScriptureReindexer {
  private final static String URL = "%s/scriptures?reference=%s%%20%d:1&version=%s";
  private final APIConnector apiConnector;
  private final BibleBookStore bibleBookStore;

  public ScriptureReindexer(APIConnector apiConnector, BibleBookStore bibleBookStore) {
    this.apiConnector = apiConnector;
    this.bibleBookStore = bibleBookStore;
  }

  public void reindex(String version, ImmutableSet<String> books) {
    String baseUrl = apiConnector.formatUrl("scriptures");

    bibleBookStore.getBibleBooks(version).stream()
        .filter(b -> books.isEmpty() || books.contains(b.name()) || b.getAltNames().stream().anyMatch(books::contains))
        .forEach(book -> {
          System.out.printf("\n%s %d =>", book.name(), book.getChapterCount());
          range(1, book.getChapterCount() + 1).forEach(chapter -> {
            System.out.printf(" %d", chapter);
            apiConnector.sendGet(String.format(URL, baseUrl, book, chapter, version));
          });
        });
    System.out.println();
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Usage: version [book1] [book2] [...]");
      System.exit(1);
    }

    APIConnector apiConnector = new APIConnector(Constants.API_URL, Constants.API_VERSION, 100);
    ChangeTracker changeTracker = new ChangeTracker(null, null, null, null);
    DAOFileDBImpl daoFileDB = new DAOFileDBImpl(changeTracker);
    BibleBookStore bibleBookStore = new BibleBookStore(() -> daoFileDB);

    new ScriptureReindexer(apiConnector, bibleBookStore).reindex(
        args[0], stream(args).skip(1).filter(a -> !a.isEmpty()).peek(System.out::println).collect(toImmutableSet()));
  }
}

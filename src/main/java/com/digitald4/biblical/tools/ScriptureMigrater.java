package com.digitald4.biblical.tools;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.util.Constants;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOApiImpl;
import java.util.stream.IntStream;

public class ScriptureMigrater {
  private final static String URL = "%s/migrateScriptures?version=%s&book=%s&chapter=%d";
  private final APIConnector apiConnector;
  private final BibleBookStore bibleBookStore;

  public ScriptureMigrater(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
    DAOApiImpl daoApi = new DAOApiImpl(apiConnector);
    bibleBookStore = new BibleBookStore(() -> daoApi);
  }

  public void migrate(String version, BibleBook startBook, BibleBook endBook) {
    String baseUrl = apiConnector.formatUrl("scriptures");
    System.out.println("Startbook: " + startBook);
    System.out.println("Endbook: " + endBook);

    bibleBookStore.getBibleBooks(version).stream()
        .filter(book -> startBook == null || book.getNumber() >= startBook.getNumber())
        .filter(book -> endBook == null || book.getNumber() < endBook.getNumber())
        .forEach(book -> {
          System.out.printf("\n%s %d =>", book.name(), book.getChapterCount());
          IntStream.range(1, book.getChapterCount() + 1).forEach(chapter -> {
            System.out.printf(" %d", chapter);
            apiConnector.sendGet(String.format(URL, baseUrl, version, book, chapter));
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
    BibleBookStore bibleBookStore = new BibleBookStore(() -> new DAOApiImpl(apiConnector));

    new ScriptureMigrater(apiConnector).migrate(
        args[0],
        args.length < 2 || args[1].isEmpty() ? null : bibleBookStore.get(args[1]),
        args.length < 3 || args[2].isEmpty() ? null : bibleBookStore.get(args[2]));
  }
}

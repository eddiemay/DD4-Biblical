package com.digitald4.biblical.tools;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.util.Constants;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOFileDBImpl;

import java.util.Comparator;
import java.util.stream.IntStream;

public class ScriptureFetcher {
  private final static String URL = "%s/scriptures?reference=%s%%20%d:1&version=%s&lang=gez";
  private final APIConnector apiConnector;
  private final BibleBookStore bibleBookStore;

  public ScriptureFetcher(APIConnector apiConnector, BibleBookStore bibleBookStore) {
    this.apiConnector = apiConnector;
    this.bibleBookStore = bibleBookStore;
  }

  public void fetch(String version, String start, String end) {
    String baseUrl = apiConnector.formatUrl("scriptures");
    BibleBook startBook = start == null ? null : bibleBookStore.get(start);
    BibleBook endBook = end == null ? null : bibleBookStore.get(end);
    System.out.println("Startbook: " + startBook);
    System.out.println("Endbook: " + endBook);

    bibleBookStore.getBibleBooks(version).stream()
        .sorted(Comparator.comparing(BibleBook::getNumber))
        .filter(book -> startBook == null || book.getNumber() >= startBook.getNumber())
        .filter(book -> endBook == null || book.getNumber() <= endBook.getNumber())
        .forEach(book -> {
          System.out.printf("\n%s %d =>", book.name(), book.getChapterCount());
          IntStream.range(1, book.getChapterCount() + 1).forEach(chapter -> {
            System.out.printf(" %d", chapter);
            apiConnector.sendGet(String.format(URL, baseUrl, book, chapter, version));
          });
        });
    System.out.println();
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Usage: version [start_book] [end_book]");
      System.exit(1);
    }

    String version = args[0];
    String start = null;
    String end = null;
    if (args.length > 1 && !args[1].isEmpty()) {
      start = args[1];
    }
    if (args.length > 2 && !args[2].isEmpty()) {
      end = args[2];
    }

    APIConnector apiConnector = new APIConnector(Constants.API_URL, Constants.API_VERSION, 100);
    DAOFileDBImpl daoFileDB = new DAOFileDBImpl();
    BibleBookStore bibleBookStore = new BibleBookStore(() -> daoFileDB);

    new ScriptureFetcher(apiConnector, bibleBookStore).fetch(version, start, end);
  }
}

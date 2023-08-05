package com.digitald4.biblical.tools;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.ScriptureVersion;
import com.digitald4.common.server.APIConnector;
import java.util.stream.IntStream;

public class ScriptureMigrater {
  private final static String API_URL = "https://dd4-biblical.appspot.com/_api";
  private final static String API_VERSION = "v1";
  private final static String URL = "%s/migrateScriptures?version=%s&book=%s&chapter=%d";
  private final APIConnector apiConnector;

  public ScriptureMigrater(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  public void migrate(String version, BibleBook startBook, BibleBook endBook) {
    String baseUrl = apiConnector.formatUrl("scriptures");
    System.out.println("Startbook: " + startBook);
    System.out.println("Endbook: " + endBook);

    ScriptureVersion scriptureVersion = ScriptureVersion.get(version);
    scriptureVersion.getBibleBooks().stream()
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

    new ScriptureMigrater(new APIConnector(API_URL, API_VERSION, 100)).migrate(
        args[0],
        args.length < 2 || args[1].isEmpty() ? null : BibleBook.get(args[1]),
        args.length < 3 || args[2].isEmpty() ? null : BibleBook.get(args[2]));
  }
}

package com.digitald4.biblical.tools;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.util.Constants;
import com.digitald4.biblical.util.Language;
import com.digitald4.biblical.util.ScriptureFetcherRouter;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOFileDBImpl;
import java.util.Comparator;
import java.util.stream.IntStream;

public class ScriptureLocalCacher {
  private final BibleBookStore bibleBookStore;
  private final ScriptureStore scriptureStore;

  public ScriptureLocalCacher(BibleBookStore bibleBookStore, ScriptureStore scriptureStore) {
    this.bibleBookStore = bibleBookStore;
    this.scriptureStore = scriptureStore;
  }

  public void fetch(String version, String lang, String start, String end) {
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
            scriptureStore.getScriptures(version, lang, String.format("%s %d:1", book, chapter));
          });
        });
    System.out.println();
  }

  public static void main(String[] args) {
    String lang = null; // Language.GEEZ;
    if (args.length == 0) {
      System.err.println("Usage: version [start_book] [end_book]");
      System.exit(1);
    }

    APIConnector apiConnector = new APIConnector(Constants.API_URL, Constants.API_VERSION, 100);
    DAOFileDBImpl daoFileDB = new DAOFileDBImpl();
    BibleBookStore bibleBookStore = new BibleBookStore(() -> daoFileDB);
    ScriptureStore scriptureStore = new ScriptureStore(
        () -> daoFileDB, null, bibleBookStore, new ScriptureReferenceProcessorSplitImpl(bibleBookStore),
        new ScriptureFetcherRouter(apiConnector), null, null);

    try {
      new ScriptureLocalCacher(bibleBookStore, scriptureStore).fetch(
          args[0],
          lang,
          args.length < 2 || args[1].isEmpty() ? null : args[1],
          args.length < 3 || args[2].isEmpty() ? null : args[2]);
    } finally {
      daoFileDB.saveFiles();
    }
  }
}

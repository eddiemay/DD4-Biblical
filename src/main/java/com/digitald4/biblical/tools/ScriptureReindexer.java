package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.util.Constants;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOApiImpl;
import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.stream.IntStream;

public class ScriptureReindexer {
  private final static String URL = "%s/reindex?version=%s&book=%s&chapter=%d&lang=en";
  private final APIConnector apiConnector;
  private final BibleBookStore bibleBookStore;

  public ScriptureReindexer(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
    DAOApiImpl daoApi = new DAOApiImpl(apiConnector);
    bibleBookStore = new BibleBookStore(() -> daoApi);
  }

  public void reindex(String version, ImmutableSet<String> books) {
    String baseUrl = apiConnector.formatUrl("scriptures");

    bibleBookStore.getBibleBooks(version).stream()
        .filter(
            book -> books.isEmpty() || books.contains(book.name())
                || book.getAltNames().stream().anyMatch(books::contains))
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

    new ScriptureReindexer(new APIConnector(Constants.API_URL, Constants.API_VERSION, 100)).reindex(
        args[0],
        Arrays.stream(args)
            .skip(1).filter(a -> !a.isEmpty()).peek(System.out::println).collect(toImmutableSet()));
  }
}

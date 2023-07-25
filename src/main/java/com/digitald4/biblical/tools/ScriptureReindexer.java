package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.digitald4.biblical.model.ScriptureVersion;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.stream.IntStream;

public class ScriptureReindexer {
  private final static String API_URL = "https://dd4-biblical.appspot.com/_api";
  private final static String API_VERSION = "v1";
  private final static String URL = "%s/reindex?version=%s&book=%s&chapter=%d";
  private final APIConnector apiConnector;

  public ScriptureReindexer(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  public void reindex(String version, ImmutableSet<String> books) {
    String baseUrl = apiConnector.formatUrl("scriptures");

    ScriptureVersion scriptureVersion = ScriptureVersion.get(version);
    scriptureVersion.getBibleBooks().stream()
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

    new ScriptureReindexer(new APIConnector(API_URL, API_VERSION, 100)).reindex(
        args[0],
        Arrays.stream(args)
            .skip(1).filter(a -> !a.isEmpty()).peek(System.out::println).collect(toImmutableSet()));
  }
}

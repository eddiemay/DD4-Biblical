package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.ScriptureVersion;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.DAOApiImpl;
import java.util.stream.IntStream;

public class ScriptureBulkDeleter {
  private final static String API_URL = "https://dd4-biblical.appspot.com/_api";
  private final static String API_VERSION = "v1";
  private final static String VERSE_COUNT_URL = "%s/verseCount?version=%s&locale=%s&book=%s&chapter=%d";
  private final static String OLD_ID_FORMAT = "%s-%s-%d-%d";
  private final APIConnector apiConnector;
  private final DAO dao;

  public ScriptureBulkDeleter(APIConnector apiConnector, DAO dao) {
    this.apiConnector = apiConnector;
    this.dao = dao;
  }

  public void delete(BibleBook startBook) {
    String booksBaseUrl = apiConnector.formatUrl("books");
    BibleBook.ALL_BOOKS.stream()
        .filter(book -> startBook == null || book.getBookNum() >= startBook.getBookNum())
        .forEach(book -> {
          String bookName = book.getName();
          System.out.printf("\n%s %d =>", book.getName(), book.getChapterCount());
          IntStream.range(1, book.getChapterCount() + 1).forEach(chapter -> {
            int verses = Integer.parseInt(
                apiConnector.sendGet(
                    String.format(
                        VERSE_COUNT_URL, booksBaseUrl, "NRSV", "en", book, chapter)).trim());
            System.out.printf("\n  %d:%d", chapter, verses);
            if (verses == 0) {
              return;
            }
            ScriptureVersion.ALL_VERSIONS.stream()
                .filter(version -> version.meetsCriteria(book, null))
                .map(ScriptureVersion::getVersion)
                .peek(version -> System.out.printf(" %s", version))
                .forEach(version ->
                    dao.delete(Scripture.class,
                        IntStream.range(version.equals("ISR") ? verses : 1, verses + 1)
                            .mapToObj(
                                verse ->
                                    String.format(OLD_ID_FORMAT, version, bookName, chapter, verse)
                                        .replaceAll(" ", "_"))
                            .collect(toImmutableList())));
          });
        });
    System.out.println();
  }

  public static void main(String[] args) {
    APIConnector apiConnector =
        new APIConnector(API_URL, API_VERSION, 100).setIdToken("226573473");
    DAOApiImpl dao = new DAOApiImpl(apiConnector);
    new ScriptureBulkDeleter(apiConnector, dao).delete(
        args.length < 1 || args[0].isEmpty() ? null : BibleBook.get(args[0]));
  }
}

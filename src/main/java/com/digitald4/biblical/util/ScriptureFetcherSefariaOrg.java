package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScriptureFetcherSefariaOrg implements ScriptureFetcher {
  private final APIConnector apiConnector;
  private static final String URL_TEMPLATE =
      "https://www.sefaria.org/api/texts/%s.%d?commentary=0&context=1&pad=0&wrapLinks=1&wrapNamedEntities=1&multiple=0&stripItags=0&transLangPref=&firstAvailableRef=1&fallbackOnDefaultVersion=1";
  private static final String TESTAMENTS_TEMPLATE =
      "The_Testaments_of_the_Twelve_Patriarchs,_The_%s_the_%s_born_Son_of_Jacob_and_%s";

  @Inject
  public ScriptureFetcherSefariaOrg(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    String bookName = book.getName().replace(" ", "_");
    if (book == BibleBook.JUBILEES) {
      return fetch(version, book, chapter, "Book_of_Jubilees");
    } else if (book == BibleBook.TESTAMENT_OF_REUBEN) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "First", "Leah"));
    } else if (book == BibleBook.TESTAMENT_OF_SIMEON) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Second", "Leah"));
    } else if (book == BibleBook.TESTAMENT_OF_LEVI) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Third", "Leah"));
    } else if (book == BibleBook.TESTAMENT_OF_JUDAH) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Fourth", "Leah"));
    } else if (book == BibleBook.TESTAMENT_OF_ISSACHAR) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Fifth", "Leah"));
    } else if (book == BibleBook.TESTAMENT_OF_ZEBULUN) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Sixth", "Leah"));
    } else if (book == BibleBook.TESTAMENT_OF_DAN) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Seventh", "Bilhah"));
    } else if (book == BibleBook.TESTAMENT_OF_NAPHTALI) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Eighth", "Bilhah"));
    } else if (book == BibleBook.TESTAMENT_OF_GAD) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Ninth", "Zilpah"));
    } else if (book == BibleBook.TESTAMENT_OF_ASHER) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Tenth", "Zilpah"));
    } else if (book == BibleBook.TESTAMENT_OF_JOSEPH) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Eleventh", "Rachel"));
    } else if (book == BibleBook.TESTAMENT_OF_BENJAMIN) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Twelfth", "Rachel"));
    }

    throw new DD4StorageException("Unknown sefaria fetch request for book: " + book);
  }

  private synchronized ImmutableList<Scripture> fetch(
      String version, BibleBook book, int chapter, String sefariaBookName) {
    ImmutableList.Builder<Scripture> result = ImmutableList.builder();
    JSONObject json =
        new JSONObject(apiConnector.sendGet(String.format(URL_TEMPLATE, sefariaBookName, chapter)));

    JSONArray english = json.getJSONArray("text");
    for (int i = 0; i < english.length(); i++) {
      result.add(
          new Scripture()
              .setVersion(version)
              .setBook(book.getName())
              .setLocale("en")
              .setChapter(chapter)
              .setVerse(i + 1)
              .setText(new StringBuilder(english.getString(i))));
    }

    JSONArray hebrew = json.getJSONArray("he");
    for (int i = 0; i < hebrew.length(); i++) {
      result.add(
          new Scripture()
              .setVersion(version)
              .setBook(book.getName())
              .setLocale("he")
              .setChapter(chapter)
              .setVerse(i + 1)
              .setText(new StringBuilder(hebrew.getString(i))));
    }

    return result.build();
  }

}

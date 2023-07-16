package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javax.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

public class ScriptureFetcherSefariaOrg implements ScriptureFetcher {
  private final APIConnector apiConnector;
  private static final String URL_TEMPLATE =
      "https://www.sefaria.org/api/texts/%s.%d?commentary=0&context=1&pad=0&wrapLinks=1&wrapNamedEntities=1&multiple=0&stripItags=0&transLangPref=&firstAvailableRef=1&fallbackOnDefaultVersion=1";
  private static final String TESTAMENTS_TEMPLATE =
      "The_Testaments_of_the_Twelve_Patriarchs,_The_%s_the_%s_Son_of_Jacob_and_%s";

  @Inject
  public ScriptureFetcherSefariaOrg(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    String bookName = book.getName().replace(" ", "_");

    if (book == BibleBook.MACCABEES_1) {
      return fetch(version, book, chapter, "The_Book_of_Maccabees_I");
    } else if (book == BibleBook.MACCABEES_2) {
      return fetch(version, book, chapter, "The_Book_of_Maccabees_II");
    } else if (book == BibleBook.SUSANNA) {
      return fetch(version, book, chapter, "The_Book_of_Susanna");
    } else if (book == BibleBook.TESTAMENTS_OF_THE_TWELVE_PATRIARCHS) {
      return fetchTestamentsOfTheTwelve(version, book, chapter);
    } else if (book == BibleBook.TESTAMENT_OF_REUBEN) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "First born", "Leah"));
    } else if (book == BibleBook.TESTAMENT_OF_SIMEON) {
      return fetch(version, book, chapter,
          String.format(TESTAMENTS_TEMPLATE, bookName, "Second", "Leah").replaceAll("Son_", ""));
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

    return fetch(version, book, chapter, String.format("Book_of_%s", bookName));
  }

  private synchronized ImmutableList<Scripture> fetch(
      String version, BibleBook book, int chapter, String sefariaBookName) {
    ImmutableList.Builder<Scripture> result = ImmutableList.builder();
    String url = String.format(URL_TEMPLATE, sefariaBookName, chapter).replaceAll(",", "%2C");
    System.out.println("Fetching: " + url + "\n");
    JSONObject json =
        new JSONObject(apiConnector.sendGet(url));

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

  private synchronized ImmutableList<Scripture> fetchTestamentsOfTheTwelve(
      String version, BibleBook book, int chapter) {
    BibleBook baseBook;
    switch (chapter) {
      case 1: baseBook = BibleBook.TESTAMENT_OF_REUBEN; break;
      case 2: baseBook = BibleBook.TESTAMENT_OF_SIMEON; break;
      case 3: baseBook = BibleBook.TESTAMENT_OF_LEVI; break;
      case 4: baseBook = BibleBook.TESTAMENT_OF_JUDAH; break;
      case 5: baseBook = BibleBook.TESTAMENT_OF_ISSACHAR; break;
      case 6: baseBook = BibleBook.TESTAMENT_OF_ZEBULUN; break;
      case 7: baseBook = BibleBook.TESTAMENT_OF_DAN; break;
      case 8: baseBook = BibleBook.TESTAMENT_OF_NAPHTALI; break;
      case 9: baseBook = BibleBook.TESTAMENT_OF_GAD; break;
      case 10: baseBook = BibleBook.TESTAMENT_OF_ASHER; break;
      case 11: baseBook = BibleBook.TESTAMENT_OF_JOSEPH; break;
      case 12: baseBook = BibleBook.TESTAMENT_OF_BENJAMIN; break;
      default: throw new DD4StorageException("Chapter out of bounds for " + book.getName());
    }

    AtomicInteger enVerse = new AtomicInteger();
    AtomicInteger heVerse = new AtomicInteger();
    return IntStream.range(1, baseBook.getChapterCount() + 1)
        .boxed()
        .flatMap(c -> fetch(version, baseBook, c).stream())
        .peek(s -> s.setBook(book.getName())
            .setChapter(chapter)
            .setVerse(
                s.getLocale().equals("en") ? enVerse.incrementAndGet() : heVerse.incrementAndGet()))
        .collect(toImmutableList());
  }
}

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
      "The_Testaments_of_the_Twelve_Patriarchs,_The_Testament_of_%s_the_%s_Son_of_Jacob_and_%s";

  @Inject
  public ScriptureFetcherSefariaOrg(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    String bookName = book.name().replace(" ", "_");

    if (book == BibleBook.MACCABEES_1) {
      return fetch(version, book, chapter, "The_Book_of_Maccabees_I");
    } else if (book == BibleBook.MACCABEES_2) {
      return fetch(version, book, chapter, "The_Book_of_Maccabees_II");
    } else if (book == BibleBook.SUSANNA) {
      return fetch(version, book, chapter, "The_Book_of_Susanna");
    } else if (book == BibleBook.TESTAMENTS_OF_THE_TWELVE_PATRIARCHS) {
      return fetchTestamentsOfTheTwelve(version, book, chapter);
    }

    return fetch(version, book, chapter, String.format("Book_of_%s", bookName));
  }

  private synchronized ImmutableList<Scripture> fetch(
      String version, BibleBook book, int chapter, String sefariaBookName) {
    ImmutableList.Builder<Scripture> result = ImmutableList.builder();
    String url = String.format(URL_TEMPLATE, sefariaBookName, chapter).replaceAll(",", "%2C");
    // System.out.println("Fetching: " + url + "\n");
    JSONObject json =
        new JSONObject(apiConnector.sendGet(url));

    JSONArray english = json.getJSONArray("text");
    for (int i = 0; i < english.length(); i++) {
      result.add(
          new Scripture()
              .setVersion(version)
              .setBook(book.name())
              .setLanguage("en")
              .setChapter(chapter)
              .setVerse(i + 1)
              .setText(new StringBuilder(english.getString(i))));
    }

    JSONArray hebrew = json.getJSONArray("he");
    for (int i = 0; i < hebrew.length(); i++) {
      result.add(
          new Scripture()
              .setVersion(version)
              .setBook(book.name())
              .setLanguage("he")
              .setChapter(chapter)
              .setVerse(i + 1)
              .setText(new StringBuilder(hebrew.getString(i))));
    }

    return result.build();
  }

  private synchronized ImmutableList<Scripture> fetchTestamentsOfTheTwelve(
      String version, BibleBook book, int chapter) {
    PatriarchSubBook patriarch;
    switch (chapter) {
      case 1: patriarch = new PatriarchSubBook("Reuben", "First_born", "Leah", 7); break;
      case 2: patriarch = new PatriarchSubBook("Simeon", "Second", "Leah", 9); break;
      case 3: patriarch = new PatriarchSubBook("Levi", "Third", "Leah", 19); break;
      case 4: patriarch = new PatriarchSubBook("Judah", "Fourth", "Leah", 26); break;
      case 5: patriarch = new PatriarchSubBook("Issachar", "Fifth", "Leah", 7); break;
      case 6: patriarch = new PatriarchSubBook("Zebulun", "Sixth", "Leah", 10); break;
      case 7: patriarch = new PatriarchSubBook("Dan", "Seventh", "Bilhah", 7); break;
      case 8: patriarch = new PatriarchSubBook("Naphtali", "Eighth", "Bilhah", 9); break;
      case 9: patriarch = new PatriarchSubBook("Gad", "Ninth", "Zilpah", 8); break;
      case 10: patriarch = new PatriarchSubBook("Asher", "Tenth", "Zilpah", 8); break;
      case 11: patriarch = new PatriarchSubBook("Joseph", "Eleventh", "Rachel", 20); break;
      case 12: patriarch = new PatriarchSubBook("Benjamin", "Twelfth", "Rachel", 12); break;
      default: throw new DD4StorageException("Chapter out of bounds for " + book.name());
    }

    AtomicInteger enVerse = new AtomicInteger();
    AtomicInteger heVerse = new AtomicInteger();
    return IntStream.range(1, patriarch.chapters + 1)
        .boxed()
        .flatMap(c -> fetch(version, book, c, patriarch.getBookName()).stream())
        .peek(s ->
            s.setChapter(chapter)
            .setVerse(
                s.getLanguage().equals("en") ? enVerse.incrementAndGet() : heVerse.incrementAndGet()))
        .collect(toImmutableList());
  }

  private static class PatriarchSubBook {
    private final int chapters;
    private final String bookName;

    public PatriarchSubBook(String name, String born, String mother, int chapters) {
      this.chapters = chapters;
      String bookName = String.format(TESTAMENTS_TEMPLATE, name, born, mother);
      this.bookName = name.equals("Simeon") ? bookName.replaceAll("Son_", "") : bookName;
    }

    public String getBookName() {
      return bookName;
    }
  }
}

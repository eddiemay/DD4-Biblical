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
  public synchronized ImmutableList<Scripture> fetch(String version, String language, BibleBook book, int chapter) {
    return switch (book.name()) {
      case BibleBook.MACCABEES_1 -> fetch(version, book, chapter, "The_Book_of_Maccabees_I");
      case BibleBook.MACCABEES_2 -> fetch(version, book, chapter, "The_Book_of_Maccabees_II");
      case BibleBook.SUSANNA -> fetch(version, book, chapter, "The_Book_of_Susanna");
      case BibleBook.TESTAMENTS_OF_THE_TWELVE_PATRIARCHS -> fetchTestamentsOfTheTwelve(version, book, chapter);
      case BibleBook.SIRACH -> fetch(version, book, chapter, "Ben_Sira");
      case BibleBook.LETTER_OF_ARISTEAS -> fetch(version, book, chapter, "Letter_of_Aristeas");
      case BibleBook.MEGILLAT_ANTIOCHUS -> fetch(version, book, chapter, "Megillat_Antiochus");
      case BibleBook.PRAYER_OF_MANESSEH -> fetch(version, book, chapter, "Prayer_of_Manasseh");
      case BibleBook.WISDOM_OF_SOLOMON -> fetch(version, book, chapter, "The_Wisdom_of_Solomon");
      default -> fetch(version, book, chapter, String.format("Book_of_%s", book.name().replace(" ", "_")));
    };
  }

  private synchronized ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter, String sefariaName) {
    ImmutableList.Builder<Scripture> result = ImmutableList.builder();
    String url = String.format(URL_TEMPLATE, sefariaName, chapter).replaceAll(",", "%2C");
    System.out.println("Fetching: " + url + "\n");
    JSONObject json = new JSONObject(apiConnector.sendGet(url));

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
    PatriarchSubBook patriarch = switch (chapter) {
      case 1 -> new PatriarchSubBook("Reuben", "First_born", "Leah", 7);
      case 2 -> new PatriarchSubBook("Simeon", "Second", "Leah", 9);
      case 3 -> new PatriarchSubBook("Levi", "Third", "Leah", 19);
      case 4 -> new PatriarchSubBook("Judah", "Fourth", "Leah", 26);
      case 5 -> new PatriarchSubBook("Issachar", "Fifth", "Leah", 7);
      case 6 -> new PatriarchSubBook("Zebulun", "Sixth", "Leah", 10);
      case 7 -> new PatriarchSubBook("Dan", "Seventh", "Bilhah", 7);
      case 8 -> new PatriarchSubBook("Naphtali", "Eighth", "Bilhah", 9);
      case 9 -> new PatriarchSubBook("Gad", "Ninth", "Zilpah", 8);
      case 10 -> new PatriarchSubBook("Asher", "Tenth", "Zilpah", 8);
      case 11 -> new PatriarchSubBook("Joseph", "Eleventh", "Rachel", 20);
      case 12 -> new PatriarchSubBook("Benjamin", "Twelfth", "Rachel", 12);
      default -> throw new DD4StorageException("Chapter out of bounds for " + book.name());
    };

    AtomicInteger enVerse = new AtomicInteger();
    AtomicInteger heVerse = new AtomicInteger();
    return IntStream.range(1, patriarch.chapters + 1)
        .boxed()
        .flatMap(c -> fetch(version, book, c, patriarch.getBookName()).stream())
        .peek(s -> s
            .setChapter(chapter)
            .setVerse(s.getLanguage().equals("en") ? enVerse.incrementAndGet() : heVerse.incrementAndGet()))
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

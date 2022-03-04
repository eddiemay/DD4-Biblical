package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptureFetcherStepBibleOrg implements ScriptureFetcher {
  public static final String URL = "https://us.api.stepbible.org/rest/search/masterSearch/reference=%s|version=%s/VNHUG//////en?lang=en-US";
  private static final String CHAPTER_URL = "https://www.stepbible.org/?q=reference=%s|version=%s&options=VNHUG";
  private static final String VERSE_URL = "https://www.stepbible.org/?q=reference=%s.%d|version=%s&options=VNHUG";
  private static final Pattern VERSE_PATTERN = Pattern.compile("(\\d+)(.+)");

  private final APIConnector apiConnector;

  @Inject
  public ScriptureFetcherStepBibleOrg(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    String result = apiConnector.sendGet(String.format(URL, formatBookForUrl(book, chapter), version));
    // System.out.println(result);

    JSONObject jsonResult = new JSONObject(result);
    Document doc = Jsoup.parse(jsonResult.getString("value").trim(), "", Parser.xmlParser());

    return doc.getElementsByClass("verse").stream()
        .map(
            verse -> new Scripture()
                .setVersion(version)
                .setBook(book.getName())
                .setChapter(chapter)
                .setVerse(Integer.parseInt(verse.getElementsByClass("verseNumber").get(0).text()))
                .setText(getText(verse.text())))
        .collect(toImmutableList());
  }

  @Override
  public String getChapterUrl(String version, ScriptureReferenceProcessor.VerseRange verseRange) {
    return String.format(CHAPTER_URL, formatBookForUrl(verseRange.getBook(), verseRange.getChapter()), version);
  }

  @Override
  public String getVerseUrl(Scripture scripture) {
    BibleBook bibleBook = BibleBook.get(scripture.getBook());
    return String.format(
        VERSE_URL, formatBookForUrl(bibleBook, scripture.getChapter()), scripture.getVerse(), scripture.getVersion());
  }

  private static StringBuilder getText(String text) {
    Matcher matcher = VERSE_PATTERN.matcher(text);
    if (matcher.matches()) {
      text = matcher.group(2);
    }

    return new StringBuilder(ScriptureFetcher.trim(text));
  }

  private static String formatBookForUrl(BibleBook book, int chapter) {
    String bookName = book.getName().replace(" ", "");
    if (book.getChapters() == 1) {
      return bookName;
    }

    return bookName + "." + chapter;
  }
}

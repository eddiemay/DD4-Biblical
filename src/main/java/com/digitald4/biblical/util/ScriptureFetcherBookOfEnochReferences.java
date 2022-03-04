package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import javax.inject.Inject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class ScriptureFetcherBookOfEnochReferences implements ScriptureFetcher {
  private static final String URL =
      "https://bookofenochreferences.wordpress.com/category/the-book-of-enoch-with-biblical-references-chapters-%d-to-%d/chapter-%d/";
  private static final Pattern VERSE_PATTERN = Pattern.compile("(\\d+). ([^<]+)");

  private final APIConnector apiConnector;

  @Inject
  public ScriptureFetcherBookOfEnochReferences(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    int rangeStart = getRangeStart(chapter);
    String htmlResult = apiConnector.sendGet(String.format(URL, rangeStart, rangeStart + 9, chapter));
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    Elements wrappers = doc.getElementsByClass("entry-content");
    if (wrappers.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    return wrappers.get(0).getElementsByTag("p").stream()
        .map(Element::ownText)
        .map(VERSE_PATTERN::matcher)
        .filter(Matcher::matches)
        .map(
            matcher -> new Scripture()
                .setVersion(version)
                .setBook(book.getName())
                .setChapter(chapter)
                .setVerse(Integer.parseInt(matcher.group(1)))
                .setText(new StringBuilder(matcher.group(2))))
        .collect(toImmutableList());
  }

  @Override
  public String getChapterUrl(String version, ScriptureReferenceProcessor.VerseRange verseRange) {
    int rangeStart = getRangeStart(verseRange.getChapter());
    return String.format(URL, rangeStart, rangeStart + 9, verseRange.getChapter());
  }

  @Override
  public String getVerseUrl(Scripture scripture) {
    int rangeStart = getRangeStart(scripture.getChapter());
    return String.format(URL, rangeStart, rangeStart + 9, scripture.getChapter());
  }

  private static int getRangeStart(int chapter) {
    return ((chapter - 1) / 10) * 10 + 1;
  }
}

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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class ScriptureFetcherOneOff implements ScriptureFetcher {
  private static final String COMMUNITY_RULE_URL = "https://www.essene.com/History&Essenes/md.htm";
  private static final String ENOCH_URL =
      "https://bookofenochreferences.wordpress.com/category/the-book-of-enoch-with-biblical-references-chapters-%d-to-%d/chapter-%d/";
  private static final String WAR_SCROLL_URL = "https://www.qumran.org/js/qumran/hss/1qm";

  private final APIConnector apiConnector;

  @Inject
  public ScriptureFetcherOneOff(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    if (book == BibleBook.ENOCH) {
      return fetchEnoch(version, book, chapter);
    } else if (book == BibleBook.COMMUNITY_RULE) {
      return fetchCommunityRule(version, book);
    } else if (book == BibleBook.WAR_SCROLL) {
      return fetchWarScroll(version, book);
    } else if (book == BibleBook.JOSEPHUS) {
      return fetchJosephus(version, book, chapter);
    }

    throw new DD4StorageException("Unknown oneoff fetch request for book: " + book);
  }

  public synchronized ImmutableList<Scripture> fetchCommunityRule(String version, BibleBook book) {
    String htmlResult = apiConnector.sendGet(COMMUNITY_RULE_URL);
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    Elements paragraphs = doc.getElementsByTag("p");
    if (paragraphs.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    AtomicInteger verse = new AtomicInteger();
    return paragraphs.stream()
        .filter(p -> {
          Elements fonts = p.getElementsByTag("font");
          return fonts.size() == 0 || fonts.get(0).attr("size").isEmpty();
        })
        .map(Element::text)
        .map(String::trim)
        .filter(text -> !text.isEmpty())
        .map(
            text -> new Scripture()
                .setVersion(version)
                .setBook(book.getName())
                .setChapter(1)
                .setVerse(verse.incrementAndGet())
                .setText(text))
        .collect(toImmutableList());
  }

  public synchronized ImmutableList<Scripture> fetchEnoch(String version, BibleBook book, int chapter) {
    final Pattern versePattern = Pattern.compile("(\\d+). ([^<]+)");
    int rangeStart = getRangeStart(chapter);
    String htmlResult = apiConnector.sendGet(String.format(ENOCH_URL, rangeStart, rangeStart + 9, chapter));
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    Elements wrappers = doc.getElementsByClass("entry-content");
    if (wrappers.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    return wrappers.get(0).getElementsByTag("p").stream()
        .map(Element::ownText)
        .map(versePattern::matcher)
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

  public synchronized ImmutableList<Scripture> fetchWarScroll(String version, BibleBook book) {
    final Pattern versePattern = Pattern.compile("\\((\\d+)\\) ([^<]+)");
    String htmlResult = apiConnector.sendGet(WAR_SCROLL_URL);
    Document doc = Jsoup.parse(htmlResult.trim());
    Elements rows = doc.getElementsByTag("tr");
    if (rows.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    AtomicInteger chapter = new AtomicInteger();
    return rows.stream()
        .map(Element::text)
        .map(versePattern::matcher)
        .filter(Matcher::matches)
        .map(
            matcher -> {
              int verse = Integer.parseInt(matcher.group(1));
              if (verse == 1) {
                chapter.incrementAndGet();
              }

              return new Scripture()
                  .setVersion(version)
                  .setBook(book.getName())
                  .setChapter(chapter.get())
                  .setVerse(verse)
                  .setText(new StringBuilder(matcher.group(2)));
            })
        .collect(toImmutableList());
  }

  public synchronized ImmutableList<Scripture> fetchJosephus(String version, BibleBook book, int chapter) {
    String urlTemplate = "http://penelope.uchicago.edu/josephus/ant-%d.html";
    final Pattern isNumber = Pattern.compile("(\\d+)");

    String htmlResult = apiConnector.sendGet(String.format(urlTemplate, chapter));
    Document doc = Jsoup.parse(htmlResult.trim());
    Elements paragraphs = doc.getElementsByTag("p");
    if (paragraphs.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    AtomicInteger verse = new AtomicInteger();
    return paragraphs.stream()
        .filter(p -> p.hasClass("indent") || p.hasClass("noindent"))
        .peek(
            p -> p.getElementsByTag("a").stream()
                .filter(a -> isNumber.matcher(a.text()).find())
                .forEach(Element::remove))
        .map(Element::text)
        .map(ScriptureFetcher::trim)
        .map(
            text -> new Scripture()
                  .setVersion(version)
                  .setBook(book.getName())
                  .setChapter(chapter)
                  .setVerse(verse.incrementAndGet())
                  .setText(new StringBuilder(text)))
        .collect(toImmutableList());
  }

  @Override
  public String getChapterUrl(String version, ScriptureReferenceProcessor.VerseRange verseRange) {
    int rangeStart = getRangeStart(verseRange.getChapter());
    return String.format(ENOCH_URL, rangeStart, rangeStart + 9, verseRange.getChapter());
  }

  @Override
  public String getVerseUrl(Scripture scripture) {
    int rangeStart = getRangeStart(scripture.getChapter());
    return String.format(ENOCH_URL, rangeStart, rangeStart + 9, scripture.getChapter());
  }

  private static int getRangeStart(int chapter) {
    return ((chapter - 1) / 10) * 10 + 1;
  }
}

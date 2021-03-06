package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class ScriptureFetcherPseudepigrapha implements ScriptureFetcher {
  private static final String BASE_URL = "http://www.pseudepigrapha.com/";
  private static final String URL = BASE_URL + "%s/%d.htm";
  private static final Pattern VERSE_PATTERN = Pattern.compile("(\\d+)(.+)");
  private static final Pattern VERSE_PATTERN2 = Pattern.compile("(\\d+) (\\D+)");
  private static final Pattern VERSE_PATTERN3 = Pattern.compile("(\\d+)[a-z]?\\. (\\D+\\d* \\D*)");
  private static final Pattern CHAPTER_PATTERN = Pattern.compile("Chapter (\\d+)");

  private final APIConnector apiConnector;

  @Inject
  public ScriptureFetcherPseudepigrapha(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    if (book == BibleBook.JUBILEES) {
      return fetchJubilees(version, book, chapter);
    } else if (book == BibleBook.JASHER) {
      return fetchJasher(version, book, chapter);
    } else if (book == BibleBook.ENOCH) {
      return version.equals("OXFORD") ? fetchEnoch(version, book, chapter) : fetchEnochOther(version, book, chapter);
    }
    throw new DD4StorageException(String.format("Unsupported book: (%s) %s %d", version, book, chapter));
  }

  private ImmutableList<Scripture> fetchJubilees(String version, BibleBook book, int chapter) {
    String htmlResult = apiConnector.sendGet(String.format(URL, book.getName().toLowerCase(), chapter));
    Document doc = Jsoup.parse(htmlResult.trim());
    Elements wrappers = doc.getElementsByTag("ol");
    if (wrappers.size() == 0) {
      throw new DD4StorageException(
          String.format("Unable to find scripture content for: (%s) %s %d", version, book, chapter));
    }
    AtomicInteger verse = new AtomicInteger();

    return wrappers.get(0).getElementsByTag("li").stream()
        .map(
            li -> new Scripture()
                .setVersion(version)
                .setBook(book.getName())
                .setChapter(chapter)
                .setVerse(verse.incrementAndGet())
                .setText(new StringBuilder(li.text().trim())))
        .collect(toImmutableList());
  }

  private ImmutableList<Scripture> fetchJasher(String version, BibleBook book, int chapter) {
    String htmlResult = apiConnector.sendGet(BASE_URL + "pseudepigrapha/jasher.html");
    Document doc = Jsoup.parse(htmlResult.trim());

    return doc.getElementsByTag("h3").stream()
        .filter(h3 -> h3.text().startsWith("CHAPTER") && Integer.parseInt(h3.text().substring(8)) == chapter)
        .flatMap(h3 -> {
          ImmutableList.Builder<TextNode> textNodes = ImmutableList.builder();
          Node nextNode = h3.nextSibling();
          while (nextNode != null && !(nextNode instanceof Element && ((Element) nextNode).tagName().equals("h3"))) {
            if (nextNode instanceof TextNode) {
              textNodes.add((TextNode) nextNode);
            }
            nextNode = nextNode.nextSibling();
          }
          return textNodes.build().stream();
        })
        .map(TextNode::text)
        .map(VERSE_PATTERN::matcher)
        .filter(Matcher::matches)
        .map(
            matcher -> new Scripture()
                .setVersion(version)
                .setBook(book.getName())
                .setChapter(chapter)
                .setVerse(Integer.parseInt(matcher.group(1)))
                .setText(new StringBuilder(matcher.group(2).trim())))
        .collect(toImmutableList());
  }

  private ImmutableList<Scripture> fetchEnoch(String version, BibleBook book, int chapter) {
    String htmlResult = apiConnector.sendGet(BASE_URL + "pseudepigrapha/enoch.htm");
    Document doc = Jsoup.parse(htmlResult.trim());

    return doc.getElementsByTag("td").stream()
        .filter(td -> {
          Matcher matcher = CHAPTER_PATTERN.matcher(td.text());
          return matcher.find() && Integer.parseInt(matcher.group(1)) == chapter;
        })
        // .peek(td -> td.getElementsByTag("BR").remove())
        .flatMap(td -> {
          Map<Integer, Scripture> scriptures = new HashMap<>();
          Matcher matcher = VERSE_PATTERN3.matcher(td.text());
          if (!matcher.find()) {
            // If we can not find a match then this must be a single verse chapter
            scriptures.put(
                1,
                new Scripture()
                    .setVersion(version)
                    .setBook(book.getName())
                    .setChapter(chapter)
                    .setVerse(1)
                    .setText(new StringBuilder(td.getElementsByTag("p").get(0).text().trim())));
          } else {
            do {
              Scripture scripture = scriptures.computeIfAbsent(
                  Integer.parseInt(matcher.group(1)),
                  verse -> new Scripture()
                      .setVersion(version)
                      .setBook(book.getName())
                      .setChapter(chapter)
                      .setVerse(verse)
                      .setText(new StringBuilder()));
              scripture.getText().append(scripture.getText().length() == 0 ? "" : " ").append(matcher.group(2).trim());
            } while (matcher.find());
          }

          return scriptures.values().stream();
        })
        .collect(toImmutableList());
  }

  private ImmutableList<Scripture> fetchEnochOther(String version, BibleBook book, int chapter) {
    String htmlResult = apiConnector.sendGet(BASE_URL + "pseudepigrapha/enoch1b.htm");
    Document doc = Jsoup.parse(htmlResult.trim());

    return doc.getElementsByTag("h3").stream()
        .filter(h3 -> h3.text().startsWith("Chapter") && Integer.parseInt(h3.text().substring(8)) == chapter)
        .map(Element::nextElementSibling)
        .map(Element::text)
        .flatMap(text -> {
          ImmutableList.Builder<Scripture> scriptures = ImmutableList.builder();
          Matcher matcher = VERSE_PATTERN2.matcher(text);
          while (matcher.find()) {
            scriptures.add(
                new Scripture()
                    .setVersion(version)
                    .setBook(book.getName())
                    .setChapter(chapter)
                    .setVerse(Integer.parseInt(matcher.group(1)))
                    .setText(new StringBuilder(matcher.group(2).trim())));
          }

          return scriptures.build().stream();
        })
        .collect(toImmutableList());
  }

  @Override
  public String getChapterUrl(String version, ScriptureReferenceProcessor.VerseRange verseRange) {
    return String.format(URL, verseRange.getBook().getName().toLowerCase(), verseRange.getChapter());
  }

  @Override
  public String getVerseUrl(Scripture scripture) {
    return String.format(URL, scripture.getBook().toLowerCase(), scripture.getChapter());
  }
}

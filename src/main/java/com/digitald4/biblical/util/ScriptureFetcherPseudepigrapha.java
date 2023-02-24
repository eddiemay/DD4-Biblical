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
      return fetchJasher(version, book);
    } else if (book == BibleBook.ENOCH) {
      return version.equals("OXFORD") ? fetchEnoch(version, book) : fetchEnochOther(version, book);
    } else if (book == BibleBook.ENOCH_2) {
      return fetch2Enoch(version, book);
    } else if (book == BibleBook.BOOK_OF_ADAM_AND_EVE) {
      return fetchBookOfAdamAndEve(version, book);
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

  private ImmutableList<Scripture> fetchJasher(String version, BibleBook book) {
    String htmlResult = apiConnector.sendGet(BASE_URL + "pseudepigrapha/jasher.html");
    Document doc = Jsoup.parse(htmlResult.trim());

    AtomicInteger chapter = new AtomicInteger();

    return doc.getElementsByTag("h3").stream()
        .filter(h3 -> h3.text().startsWith("CHAPTER"))
        .peek(h3 -> chapter.set(Integer.parseInt(h3.text().substring(8))))
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
                .setChapter(chapter.get())
                .setVerse(Integer.parseInt(matcher.group(1)))
                .setText(new StringBuilder(matcher.group(2).trim())))
        .collect(toImmutableList());
  }

  private ImmutableList<Scripture> fetchEnoch(String version, BibleBook book) {
    String htmlResult = apiConnector.sendGet(BASE_URL + "pseudepigrapha/enoch.htm");
    Document doc = Jsoup.parse(htmlResult.trim());

    return doc.getElementsByTag("td").stream()
        .flatMap(td -> {
          Matcher chapMatcher = CHAPTER_PATTERN.matcher(td.text());
          if (!chapMatcher.find()) {
            return ImmutableList.<Scripture>of().stream();
          }
          int chapter = Integer.parseInt(chapMatcher.group(1));
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

  private ImmutableList<Scripture> fetch2Enoch(String version, BibleBook book) {
    String htmlResult = apiConnector.sendGet(BASE_URL + "pseudepigrapha/enochs2.htm");
    Document doc = Jsoup.parse(htmlResult.trim());

    AtomicInteger chapter = new AtomicInteger();

    return doc.getElementsByTag("td").stream()
        .filter(td -> {
          int ch = td.getElementsByTag("a").stream()
              .map(Element::text)
              .map(CHAPTER_PATTERN::matcher)
              .filter(Matcher::find)
              .map(matcher -> matcher.group(1))
              .map(Integer::parseInt)
              .findFirst().orElse(0);
          if (ch == 0) {
            return false;
          }

          chapter.set(ch);
          return true;
        })
        .flatMap(td -> td.getElementsByTag("p").stream())
        .map(Element::text)
        .map(VERSE_PATTERN::matcher)
        .filter(Matcher::matches)
        .map(
            matcher -> new Scripture()
                .setVersion(version)
                .setBook(book.getName())
                .setChapter(chapter.get())
                .setVerse(Integer.parseInt(matcher.group(1)))
                .setText(new StringBuilder(matcher.group(2).trim())))
        .collect(toImmutableList());
  }

  private ImmutableList<Scripture> fetchEnochOther(String version, BibleBook book) {
    String htmlResult = apiConnector.sendGet(BASE_URL + "pseudepigrapha/enoch1b.htm");
    Document doc = Jsoup.parse(htmlResult.trim());

    AtomicInteger chapter = new AtomicInteger();

    return doc.getElementsByTag("h3").stream()
        .filter(h3 -> h3.text().startsWith("Chapter"))
        .peek(h3 -> chapter.set(Integer.parseInt(h3.text().substring(8))))
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
                    .setChapter(chapter.get())
                    .setVerse(Integer.parseInt(matcher.group(1)))
                    .setText(new StringBuilder(matcher.group(2).trim())));
          }

          return scriptures.build().stream();
        })
        .collect(toImmutableList());
  }

  private ImmutableList<Scripture> fetchBookOfAdamAndEve(String version, BibleBook book) {
    final Pattern versePattern = Pattern.compile("(\\d+) ([^<]+)");
    String htmlResult = apiConnector.sendGet(BASE_URL + "pseudepigrapha/adamnev.htm");
    Document doc = Jsoup.parse(htmlResult.trim());
    AtomicInteger chapter = new AtomicInteger();

    Element toReplace = doc.getElementsByTag("p").stream()
        .filter(p -> p.text().startsWith("xv 1,2"))
        .findFirst()
        .orElse(null);
    if (toReplace != null) {
      toReplace.text("1 When the angels, who were under me, heard this, they refused to worship him. And Michael saith,");
      toReplace.after("<p>2 'Worship the image of God, but if thou wilt not worship him, the Lord God will be wrath</p>");
    }

    toReplace = doc.getElementsByTag("p").stream()
        .filter(p -> p.text().startsWith("2,3"))
        .findFirst()
        .orElse(null);
    if (toReplace != null) {
      toReplace.text("2 me his glory which he himself hath lost.' And at that moment, the devil vanished before him.");
      toReplace.after("<p>3 But Adam endured in his penance, standing for forty days (on end) in the water of Jordan.</p>");
    }

    return doc.getElementsByTag("p").stream()
        .map(Element::text)
        .map(String::trim)
        .filter(text -> !text.isEmpty())
        .map(text -> {
           if (text.startsWith("iii")) {
             return text.replace("iii", "iii 1");
           } else if (text.startsWith("'Worship the image of God")) {
             return "2 " + text;
           } else if (text.startsWith("And Michael himself")) {
             return "3 " + text;
           } else if (text.startsWith("And Adam answered")) {
             return "1 " + text;
           } else if (text.startsWith("Then Seth and his mother")) {
             return "1 " + text;
           } else if (text.startsWith("And all angels")) {
             return "4 " + text;
           }

           return text;
        })
        .map(text -> {
          Matcher matcher = versePattern.matcher(text);
          if (!matcher.find()) {
            throw new DD4StorageException("No match for: " + text);
          }
          return matcher;
        })
        .map(matcher -> {
          int verse = Integer.parseInt(matcher.group(1));
          if (verse == 1) {
            chapter.incrementAndGet();
          }

          return new Scripture()
              .setVersion(version)
              .setBook(book.getName())
              .setChapter(chapter.get())
              .setVerse(verse)
              .setText(new StringBuilder(matcher.group(2).trim()));
        })
        .collect(toImmutableList());
  }
}

package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.Comparator.comparing;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.util.Pair;
import com.google.common.collect.ImmutableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import javax.inject.Inject;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptureFetcherBibleGateway implements ScriptureFetcher {
  private static final String URL = "https://www.biblegateway.com/passage/?version=%s&search=%s+%d";
  private static final Pattern VERSE_CLS_PATTERN = Pattern.compile("([\\w\\d]+)-(\\d+)-(\\d+)");
  private static final Pattern VERSE_PATTERN = Pattern.compile("(\\d+)(.+)");

  private final APIConnector apiConnector;

  @Inject
  public ScriptureFetcherBibleGateway(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, String language, BibleBook book, int chapter) {
    String url = String.format(URL, version, formatBookForUrl(book.name()), chapter);
    // System.out.println("Fetch of: " + url);
    String htmlResult = apiConnector.sendGet(url);
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    Elements wrappers = doc.getElementsByClass("passage-text");
    if (wrappers.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }
    Element wrapper = wrappers.get(0);

    wrapper.getElementsByClass("footnote").forEach(Element::remove);
    wrapper.getElementsByClass("crossreference").forEach(Element::remove);
    wrapper.getElementsByTag("h3").forEach(Element::remove);
    wrapper.getElementsByTag("h4").forEach(Element::remove);

    AtomicBoolean firstNumFound = new AtomicBoolean();

    return wrapper.getElementsByClass("text").stream()
        .map(element -> {
          int verse = element.classNames().stream()
              .map(VERSE_CLS_PATTERN::matcher).filter(Matcher::matches)
              .map(matcher -> Integer.parseInt(matcher.group(3))).findFirst().orElse(0);
          if (verse == 0) {
            return null;
          }

          // We need to filter out commentary text if we have not found a chapternum or versenum yet.
          if (!firstNumFound.get()) {
            firstNumFound.set(element.getElementsByClass("chapternum").size() > 0 ||
                element.getElementsByClass("versenum").size() > 0);
            if (!firstNumFound.get()) {
              return null;
            }
          }

          return Pair.of(verse, element);
        })
        .filter(Objects::nonNull)
        .collect(groupingBy(Pair::getLeft))
        .entrySet()
        .stream()
        .map(
            entry -> new Scripture()
                .setVersion(version)
                .setBook(book.name())
                .setChapter(chapter)
                .setVerse(entry.getKey())
                .setText(getText(entry.getValue().stream().map(Pair::getRight).collect(toImmutableList()))))
        .sorted(comparing(Scripture::getVerse))
        .collect(toImmutableList());
  }

  private static String formatBookForUrl(String book) {
    if (book.equals(BibleBook.WISDOM_OF_SOLOMON)) {
      book = "Wisdom";
    }

    return book.replace(" ", "+");
  }

  private static StringBuilder getText(ImmutableList<Element> elements) {
    String text = elements.stream().map(Element::text).map(ScriptureFetcher::trim).collect(joining(" ")).trim();
    Matcher matcher = VERSE_PATTERN.matcher(text);
    if (matcher.matches()) {
      text = matcher.group(2);
    }

    return new StringBuilder(text.trim());
  }
}

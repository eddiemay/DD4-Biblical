package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.HebrewConverter.*;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.util.Pair;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import javax.inject.Inject;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptureFetcherBibleHub implements ScriptureFetcher, InterlinearFetcher {
  private static final String CHAPTER_URL = "https://biblehub.com/%s/%s/%d.htm";
  private static final String INTERLINEAR_PAGE_URL = "https://biblehub.com/interlinear/%s/%d.htm";
  private static final String REF_TEXT = "reftext";
  private static final Pattern VERSE_PATTERN = Pattern.compile("(\\d+)(.+)");
  private static final Pattern VERSE_CLS_PATTERN = Pattern.compile("([\\d\\w]+)-(\\d+)-(\\d+)");

  private final APIConnector apiConnector;

  @Inject
  public ScriptureFetcherBibleHub(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, String language, BibleBook book, int chapter) {
    if (version.startsWith("WLC")) {
      return fetchWLC(version, book, chapter);
    } else if (version.equals("Nestle")) {
      return fetchNestle(version, book, chapter);
    }

    return fetchOther(version, book, chapter);
  }

  public synchronized ImmutableList<Scripture> fetchOther(String version, BibleBook book, int chapter) {
    String url = String.format(CHAPTER_URL, version.toLowerCase(), formatBookForUrl(book.name()), chapter);
    String htmlResult = apiConnector.sendGet(url);
    Document doc = Jsoup.parse(htmlResult.trim());
    Elements chaps = doc.getElementsByClass("chap");
    if (chaps.isEmpty()) {
      throw new DD4StorageException("Unable to find scripture content for " + url);
    }
    Element chap = chaps.get(0);

    Elements scs = chap.getElementsByClass("sc");
    if (!scs.isEmpty()) {
      return scs.stream()
          .peek(sc -> sc.getElementsByClass("fn").forEach(Element::remove))
          .map(sc -> new Scripture()
              .setVersion(version)
              .setBook(book.name())
              .setChapter(chapter)
              .setVerse(Integer.parseInt(sc.getElementsByClass(REF_TEXT).get(0).getElementsByTag("b").text()))
              .setText(getText(sc.text())))
          .collect(toImmutableList());
    }

    return chap.getElementsByClass(REF_TEXT).stream()
        .map(reftext -> new Scripture()
            .setVersion(version)
            .setBook(book.name())
            .setChapter(chapter)
            .setVerse(Integer.parseInt(reftext.getElementsByTag("b").text()))
            .setText(getText(reftext)))
        .collect(toImmutableList());
  }

  public synchronized ImmutableList<Scripture> fetchWLC(String version, BibleBook book, int chapter) {
    String url = String.format(CHAPTER_URL, version.toLowerCase(), formatBookForUrl(book.name()), chapter);
    Document doc = Jsoup.parse(apiConnector.sendGet(url).trim());
    Elements scs = doc.getElementsByClass("spcl");
    if (scs.isEmpty()) {
      throw new DD4StorageException("Unable to find scripture content for " + url);
    }

    AtomicInteger verse = new AtomicInteger();

    return scs.stream()
        .peek(sc -> sc.getElementsByClass("fn").forEach(Element::remove))
        .map(sc -> new Scripture()
            .setVersion(version)
            .setLanguage(version.equals("WLC") ? Language.YIDDISH : Language.HEBREW)
            .setBook(book.name())
            .setChapter(chapter)
            .setVerse(verse.incrementAndGet())
            .setText(getText(sc.text())))
        .collect(toImmutableList());
  }

  public synchronized ImmutableList<Scripture> fetchNestle(String version, BibleBook book, int chapter) {
    String url = String.format(CHAPTER_URL, version.toLowerCase(), formatBookForUrl(book.name()), chapter);
    Document doc = Jsoup.parse(apiConnector.sendGet(url).trim());
    Elements spans = doc.getElementsByClass("chap").first().getElementsByClass("greek");
    if (spans.isEmpty()) {
      throw new DD4StorageException("Unable to find scripture content for " + url);
    }

    AtomicInteger verse = new AtomicInteger();

    return spans.stream()
        .map(span -> new Scripture()
            .setVersion(version)
            .setLanguage("gk")
            .setBook(book.name())
            .setChapter(chapter)
            .setVerse(verse.incrementAndGet())
            .setText(getText(span.text())))
        .collect(toImmutableList());
  }

  public ImmutableList<Scripture> fetchAlt(String version, String book, int chapter) {
    String url = String.format(CHAPTER_URL, version, formatBookForUrl(book), chapter);
    String htmlResult = apiConnector.sendGet(url);
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    Elements wrappers = doc.getElementsByClass("chap");
    if (wrappers.isEmpty()) {
      throw new DD4StorageException("Unable to find scripture content");
    }
    Element wrapper = wrappers.get(0);

    wrapper.getElementsByClass("fn").forEach(Element::remove);
    wrapper.getElementsByTag("h3").forEach(Element::remove);
    wrapper.getElementsByTag("h4").forEach(Element::remove);

    return wrapper.getElementsByClass("text").stream()
        .map(element -> {
          int verse = element.classNames().stream()
              .map(VERSE_CLS_PATTERN::matcher)
              .filter(Matcher::matches)
              .map(matcher -> Integer.parseInt(matcher.group(3))).findFirst()
              .orElse(0);
          if (verse == 0) {
            return null;
          }

          return Pair.of(verse, element);
        })
        .filter(Objects::nonNull)
        .collect(groupingBy(Pair::getLeft))
        .entrySet()
        .stream()
        .map(entry -> new Scripture()
            .setVersion(version)
            .setBook(book)
            .setChapter(chapter)
            .setVerse(entry.getKey())
            .setText(getText(entry.getValue().stream().map(Pair::getRight).collect(toImmutableList()))))
        .sorted(comparing(Scripture::getVerse))
        .collect(toImmutableList());
  }

  @Override
  public ImmutableList<Interlinear> fetchInterlinear(BibleBook book, int chapter) {
    boolean hebrew = book.getNumber() < 40;
    String url = String.format(INTERLINEAR_PAGE_URL, formatBookForUrl(book.name()), chapter);
    String htmlResult = apiConnector.sendGet(url);
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.htmlParser());
    AtomicInteger verse = new AtomicInteger(1);
    AtomicInteger index = new AtomicInteger();
    AtomicReference<Interlinear> strongIdOwner = new AtomicReference<>();
    try {
      return doc.getElementsByClass("chap").first().getElementsByTag("table").stream()
          .filter(table -> table.hasAttr("border") && table.hasAttr("cellspacing") && table.hasAttr("cellpadding"))
          .flatMap(vTable -> vTable.getElementsByClass(hebrew ? "tablefloatheb" : "tablefloat").stream())
          .map(iTable -> {
            Element verseElem = iTable.getElementsByClass(hebrew ? "refheb" : "refmain").first();
            if (verseElem != null && !verseElem.text().isEmpty()) {
              verse.set(Integer.parseInt(verseElem.text().trim()));
              index.set(0);
              strongIdOwner.set(null);
            }
            Elements strongsElems = iTable.getElementsByClass(hebrew ? "strongs" : "pos");
            Element strongsElem = strongsElems.first();
            if (strongsElem.getElementsByTag("a").first() == null && strongsElems.size() > 1) {
              strongsElem = strongsElems.get(1);
            }
            String strongsId = getStrongsId(strongsElem.getElementsByTag("a").first());
            String transliteration = iTable.getElementsByClass("translit").last().text();
            String word = removeGarbage(iTable.getElementsByClass(hebrew ? "hebrew" : "greek").last().text());
            if (word.endsWith("׃")) {
              word = word.substring(0, word.length() - 1);
            }
            String translation = iTable.getElementsByClass("eng").last().text();
            String morphology = iTable.getElementsByClass(hebrew ? "strongsnt" : "strongsnt2").last().text();

            // Get rid of the last item if it doesn't have a translation, this is normally the
            // marker of end of a paragraph or end of a chapter.
            if (strongsId == null && translation == null && "Punc".equals(morphology)) {
              return null;
            }

            if (strongsId != null && strongIdOwner.get() != null) {
              strongIdOwner.get().setStrongsId(strongsId).setTranslation(translation);
              strongsId = translation = null;
              strongIdOwner.set(null);
            }

            Interlinear interlinear = new Interlinear()
                .setBook(book.name())
                .setBookNumber(book.getNumber())
                .setChapter(chapter)
                .setVerse(verse.get())
                .setIndex(index.incrementAndGet())
                .setStrongsId(strongsId)
                .setWord(word)
                .setConstantsOnly(toConstantsOnly(word))
                .setTransliteration(transliteration)
                .setMorphology(morphology)
                .setTranslation(translation);

            if (strongsId == null && "-".equals(translation) && morphology.isEmpty() && strongIdOwner.get() == null) {
              strongIdOwner.set(interlinear);
            }

            return interlinear;
          })
          .collect(toImmutableList());
    } catch (NullPointerException | NumberFormatException npe) {
      throw new DD4StorageException(
          String.format("Error while reading: %s %d:%s:%s %s", book, chapter, verse, index, url), npe);
    }
  }

  private static String getStrongsId(Element strongsLink) {
    return strongsLink == null ? null
        : toStrongsId((strongsLink.attr("href").contains("/hebrew") ? "H" : "G") + strongsLink.text());
  }

  public static String formatBookForUrl(String book) {
    if (book.equals(BibleBook.SONG_OF_SOLOMON)) {
      return "songs";
    } else if (book.equals(BibleBook.SIRACH)) {
      return "ecclesiasticus";
    }

    return book.toLowerCase().replace(" ", "_");
  }

  private static StringBuilder getText(String text) {
    Matcher matcher = VERSE_PATTERN.matcher(text);
    if (matcher.matches()) {
      text = matcher.group(2);
    }

    return new StringBuilder(ScriptureFetcher.trim(text));
  }

  private static StringBuilder getText(Element reftext) {
    StringBuilder builder = new StringBuilder();
    Node next = reftext.nextSibling();
    while (next != null) {
      if (next instanceof Element element) {
        if (element.tagName().equals("p") || element.tagName().equals("span") && element.hasClass("reftext")) {
          break;
        }
        if (!element.hasClass("footnote")) {
          builder.append(element.text());
        }
      } else {
        builder.append(next.outerHtml());
      }
      next = next.nextSibling();
    }

    return new StringBuilder(builder.toString().trim());
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

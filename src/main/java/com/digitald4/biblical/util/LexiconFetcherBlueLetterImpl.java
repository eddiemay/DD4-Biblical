package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.ScriptureFetcherBibleHub.formatBookForUrl;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Lexicon.Node;
import com.digitald4.biblical.model.Lexicon.TranslationCount;
import com.digitald4.biblical.store.Annotations.FetchInterlinearByVerse;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.VerseRange;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.inject.Inject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class LexiconFetcherBlueLetterImpl implements LexiconFetcher {
  private static final String URL = "https://www.blueletterbible.org/lexicon/%s/kjv/wlc/0-1/";
  private static final String INTERLINEAR_PAGE_URL = "https://biblehub.com/interlinear/%s/%d.htm";
  private static final String INTERLINEAR_URL = "https://biblehub.com/text/%s/%d-%d.htm";
  private static final Pattern LEX_COUNT = Pattern.compile("([A-z()0-9 ]+) \\(([0-9]+)x\\)");
  private static final Pattern VERSE_PATTERN = Pattern.compile("([0-9]+:[0-9]+)");

  private static final Pattern STRONGS_REF_PATTERN = Pattern.compile("([HG]\\d+)");
  private static final Pattern SCRIPTURE_REF_PATTERN = Pattern.compile("([0-9 ]*[A-z]+ \\d+:\\d+)");

  private final APIConnector apiConnector;
  private final boolean fetchByVerse;

  @Inject
  public LexiconFetcherBlueLetterImpl(
      APIConnector apiConnector, @FetchInterlinearByVerse boolean fetchByVerse) {
    this.apiConnector = apiConnector;
    this.fetchByVerse = fetchByVerse;
  }

  @Override
  public Lexicon getLexicon(String strongsId) {
    System.out.println("Fetching: " + strongsId);
    String htmlResult = apiConnector.sendGet(String.format(URL, strongsId));
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    String modern = doc.getElementsByClass("lexTitle" + (strongsId.startsWith("H") ? "Hb" : "GK"))
        .first().text().trim();

    return new Lexicon()
        .setId(strongsId)
        .setWord(modern)
        .setConstantsOnly(HebrewConverter.toConstantsOnly(modern))
        .setTransliteration(doc.getElementById("lexTrans")
            .getElementsByClass("small-text-right").first().text().trim())
        .setPronunciation(doc.getElementById("lexPro")
            .getElementsByClass("small-text-right").first().ownText().trim())
        .setPartOfSpeech(doc.getElementById("lexPart")
            .getElementsByClass("small-text-right").first().text().trim())
        .setRootWord(processRootText(doc.getElementById("lexRoot")))
        .setDictionaryAid(getDictionaryAid(doc.getElementById("lexDict")))
        .setTranslationCounts(parseTranslationCounts(doc.getElementById("lexCount")))
        .setStrongsDefinition(
            processScriptureReferences(
                processStrongsReferences(
                    doc.getElementsByClass("lexStrongsDef").first().text().trim())))
        .setBrownDriverBriggs(getText(doc.getElementById("BDBTayersLexBlock")))
        .setOutline(parseOutline(doc.getElementById("outlineBiblical")))
        .setBriggsReferences(getBriggsReferences(doc.getElementsByClass("scriptureIndex").first()));
  }

  private static String getText(Element element) {
    return element == null ? null : element.text();
  }

  private static String getDictionaryAid(Element element) {
    return element == null ? null
        : element.children().first().children().first().children().get(1).text().trim();
  }

  private static ImmutableList<TranslationCount> parseTranslationCounts(Element element) {
    if (element == null) {
      return ImmutableList.of();
    }
    String text = element.children().first().children().first().children().get(1).text().trim();
    Matcher matcher = LEX_COUNT.matcher(text);
    ImmutableList.Builder<TranslationCount> translationCounts = ImmutableList.builder();
    while (matcher.find()) {
      translationCounts.add(
          new TranslationCount()
              .setWord(matcher.group(1).trim())
              .setCount(Integer.parseInt(matcher.group(2))));
    }

    return translationCounts.build();
  }

  private static ImmutableList<Node> parseOutline(Element element) {
    Element ol = element.getElementsByTag("ol").first();

    return ol == null ? null : ol.children().stream()
        .map(li -> new Node().setValue(li.children().first().text()).setChildren(parseOutline(li)))
        .collect(toImmutableList());
  }

  private static ImmutableList<String> getBriggsReferences(Element element) {
    if (element == null) {
      return ImmutableList.of();
    }

    ImmutableList.Builder<String> briggsReferences = ImmutableList.builder();
    Elements ps = element.getElementsByTag("p");
    for (int x = 0; x < ps.size(); x++) {
      String book = ps.get(x).text().trim();
      if (x + 1 < ps.size()) {
        Matcher matcher = VERSE_PATTERN.matcher(ps.get(++x).text());
        while (matcher.find()) {
          briggsReferences.add(book + " " + matcher.group(1));
        }
      }
    }
    return briggsReferences.build();
  }

  private static String processRootText(Element element) {
    return element == null ? null : processStrongsReferences(
        element.children().first().children().first().children().get(1).text().trim());
  }

  public static String processStrongsReferences(String rootText) {
    if (!rootText.contains("showStrongsDefs")) {
      Matcher matcher = STRONGS_REF_PATTERN.matcher(rootText);
      while (matcher.find()) {
        String rootRef = matcher.group(1);
        rootText = rootText.replace(rootRef,
            String.format(
                "<a href=\"\" data-ng-click=\"$ctrl.showStrongsDefs('%s')\">%s</a>", rootRef,
                rootRef));
      }
    }

    return rootText;
  }

  public static String processScriptureReferences(String text) {
    if (!text.contains("scripture ref")) {
      Matcher matcher = SCRIPTURE_REF_PATTERN.matcher(text);
      while (matcher.find()) {
        String reference = matcher.group(1).trim();
        text = text.replace(reference, String.format("<scripture ref=\"%s\"/>", reference));
      }
    }

    return text;
  }

  @Override
  public ImmutableList<Interlinear> fetchInterlinear(VerseRange vr) {
    return !fetchByVerse ? fetchInterlinear(vr.getBook(), vr.getChapter())
        : IntStream.range(vr.getStartVerse(), vr.getEndVerse() + 1).boxed()
            .flatMap(verse -> fetchInterlinear(vr.getBook(), vr.getChapter(), verse).stream())
            .collect(toImmutableList());
  }

  @Override
  public ImmutableList<Interlinear> fetchInterlinear(BibleBook book, int chapter, int verse) {
    String htmlResult = apiConnector.sendGet(
        String.format(INTERLINEAR_URL, formatBookForUrl(book.name()), chapter, verse));
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.htmlParser());

    AtomicInteger index = new AtomicInteger();
    AtomicReference<Interlinear> strongIdOwner = new AtomicReference<>();
    return doc.getElementsByClass("maintext").first().getElementsByTag("tr").stream().skip(1)
        .map(tr -> {
          String word = tr.children().get(1).ownText();
          if (word.endsWith("׃")) {
            word = word.substring(0, word.length() - 1);
          }

          String strongsId = getStrongsId(tr.children().first().getElementsByTag("a").first());
          String translation = tr.children().get(2).text();
          String morphology = tr.getElementsByClass("pos").last().text().trim();

          // Get rid of the last item if it doesn't have a translation, this is normally the marker,
          // of end of a paragraph or end of a chapter.
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
              .setVerse(verse)
              .setIndex(index.incrementAndGet())
              .setStrongsId(strongsId)
              .setWord(word)
              .setConstantsOnly(HebrewConverter.toConstantsOnly(word))
              .setTransliteration(tr.getElementsByClass("translit").text())
              .setMorphology(morphology)
              .setTranslation(translation);

          if (strongsId == null && "-".equals(translation) && morphology.isEmpty() && strongIdOwner.get() == null) {
            strongIdOwner.set(interlinear);
          }

          return interlinear;
        })
        .filter(Objects::nonNull)
        .collect(toImmutableList());
  }

  @Override
  public ImmutableList<Interlinear> fetchInterlinear(BibleBook book, int chapter) {
    String htmlResult = apiConnector.sendGet(
        String.format(INTERLINEAR_PAGE_URL, formatBookForUrl(book.name()), chapter));
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.htmlParser());
    AtomicInteger verse = new AtomicInteger();
    AtomicInteger index = new AtomicInteger();
    AtomicReference<Interlinear> strongIdOwner = new AtomicReference<>();
    return doc.getElementsByClass("chap").first().children().stream()
        .filter(child -> child.tagName().equals("table"))
        .flatMap(verseTable -> verseTable.children().first().children().first().children().first().children().stream())
        .map(iTable -> {
          Element verseElem = iTable.getElementsByClass("refheb").first();
          if (verseElem != null) {
            verse.set(Integer.parseInt(verseElem.text().trim()));
            index.set(0);
            strongIdOwner.set(null);
          }
          Element strongsElem = iTable.getElementsByClass("strongs").first();
          if (strongsElem.getElementsByTag("a").first() == null && iTable.getElementsByClass("strongs").size() > 1) {
            strongsElem = iTable.getElementsByClass("strongs").get(1);
          }
          String strongsId = getStrongsId(strongsElem.getElementsByTag("a").first());
          String transliteration = iTable.getElementsByClass("translit").last().text();
          String word = iTable.getElementsByClass("hebrew").last().text();
          if (word.endsWith("׃")) {
            word = word.substring(0, word.length() - 1);
          }
          String translation = iTable.getElementsByClass("eng").last().text();
          String morphology = iTable.getElementsByClass("strongsnt").last().text();

          // Get rid of the last item if it doesn't have a translation, this is normally the marker,
          // of end of a paragraph or end of a chapter.
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
              .setConstantsOnly(HebrewConverter.toConstantsOnly(word))
              .setTransliteration(transliteration)
              .setMorphology(morphology)
              .setTranslation(translation);

          if (strongsId == null && "-".equals(translation) && morphology.isEmpty()
              && strongIdOwner.get() == null) {
            strongIdOwner.set(interlinear);
          }

          return interlinear;
        })
        .collect(toImmutableList());
  }

  private static String getStrongsId(Element strongsLink) {
    if (strongsLink == null) {
      return null;
    }

    return (strongsLink.attr("href").contains("/hebrew") ? "H" : "G") + strongsLink.text();
  }
}

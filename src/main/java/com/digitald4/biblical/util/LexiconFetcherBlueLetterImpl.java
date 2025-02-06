package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.model.Lexicon.Node;
import com.digitald4.biblical.model.Lexicon.TranslationCount;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class LexiconFetcherBlueLetterImpl implements LexiconFetcher {
  private static final String URL = "https://www.blueletterbible.org/lexicon/%s/kjv/wlc/0-1/";
  private static final Pattern LEX_COUNT = Pattern.compile("([A-z()0-9 ]+) \\(([0-9]+)x\\)");
  private static final Pattern VERSE_PATTERN = Pattern.compile("([0-9]+:[0-9]+)");

  private static final Pattern STRONGS_REF_PATTERN = Pattern.compile("([HG]\\d+)");
  private static final Pattern SCRIPTURE_REF_PATTERN = Pattern.compile("([0-9 ]*[A-z]+ \\d+:\\d+)");

  private final APIConnector apiConnector;

  @Inject
  public LexiconFetcherBlueLetterImpl(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public Lexicon getLexicon(String strongsId) {
    System.out.println("Fetching " + strongsId);
    String htmlResult = apiConnector.sendGet(String.format(URL, strongsId));
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    String modern = doc.getElementsByClass("lexTitle" + (strongsId.startsWith("H") ? "Hb" : "GK")).first().text().trim();

    return new Lexicon()
        .setId(strongsId)
        .setWord(modern)
        .setTransliteration(doc.getElementById("lexTrans").getElementsByClass("small-text-right").first().text().trim())
        .setPronunciation(doc.getElementById("lexPro").getElementsByClass("small-text-right").first().ownText().trim())
        .setPartOfSpeech(doc.getElementById("lexPart").getElementsByClass("small-text-right").first().text().trim())
        .setRootWord(processRootText(doc.getElementById("lexRoot")))
        .setDictionaryAid(getDictionaryAid(doc.getElementById("lexDict")))
        .setTranslationCounts(parseTranslationCounts(doc.getElementById("lexCount")))
        .setStrongsDefinition(
            processScriptureReferences(
                processStrongsReferences(doc.getElementsByClass("lexStrongsDef").first().text().trim())))
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
    return element == null ? null
        : processStrongsReferences(element.children().first().children().first().children().get(1).text().trim());
  }

  public static String processStrongsReferences(String rootText) {
    if (!rootText.contains("showStrongsDef")) {
      Matcher matcher = STRONGS_REF_PATTERN.matcher(rootText);
      while (matcher.find()) {
        String rootRef = matcher.group(1);
        rootText = rootText.replace(rootRef,
            String.format("<a href=\"\" data-ng-click=\"$ctrl.showStrongsDef('%s')\">%s</a>", rootRef, rootRef));
      }
    } else if (rootText.contains("showStrongsDefs")) {
      return rootText.replaceAll("showStrongsDefs", "showStrongsDef");
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
}

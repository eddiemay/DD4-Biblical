package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Arrays.stream;

import com.digitald4.biblical.model.AncientLexicon;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import javax.inject.Inject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;

public class AncientLexiconFetcher {
  private static final String URL_TEMPLATE = "https://www.ancient-hebrew.org/ahlb/%s.html";
  private static final ImmutableList<String> PAGE_NAMES = ImmutableList.of(
      "aleph", "beyt", "gimel", "dalet", "hey", "vav", "zayin", "hhet", "tet", "yud", "kaph",
      "lamed", "mem", "nun", "samehh", "ayin", "pey", "tsade", "quph", "resh", "shin", "tav",
      "ghayin", "adopted", "4letter", "fixes");
  private final APIConnector apiConnector;

  @Inject
  public AncientLexiconFetcher(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  public ImmutableList<AncientLexicon> fetch() {
    return IntStream.range(0, PAGE_NAMES.size()).boxed()
        .flatMap(page -> fetch(page).stream()).collect(toImmutableList());
  }

  public ImmutableList<AncientLexicon> fetch(int page) {
    String url = String.format(URL_TEMPLATE, PAGE_NAMES.get(page));
    String htmlResult = apiConnector.sendGet(url);
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.htmlParser());

    return doc.getElementsByAttributeValue("align", "justify").stream()
        .map(element -> {
          ImmutableList<String> words = element.getElementsByTag("font").stream()
              .map(Element::text).map(String::trim).collect(toImmutableList());
          AncientLexicon ancientLexicon = new AncientLexicon().setWord(words.get(0)).setWords(words);
          AtomicReference<String> property = new AtomicReference<>();
          element.childNodes().forEach(node -> {
            if (node instanceof TextNode) {
              if (property.get() != null) {
                setProperty(ancientLexicon, property.get(), ((TextNode) node).text().trim());
              }
            } else if (((Element) node).tagName().equals("b")) {
              property.set(((Element) node).text().trim().replaceAll(":", ""));
            }
          });
          return ancientLexicon;
        })
        .collect(toImmutableList());
  }

  private static void setProperty(AncientLexicon ancientLexicon, String prop, String value) {
    switch (prop) {
      case "Translation" -> ancientLexicon.setTranslation(value);
      case "Definition" -> ancientLexicon.setDefinition(value);
      case "Strong's Hebrew #", "Strong's Aramaic #" -> ancientLexicon.addStrongIds(
          stream(value.split(", ")).map(id -> id.toUpperCase().replaceAll("\\.", "")).collect(toImmutableSet()));
      case "KJV Translations" -> ancientLexicon.setKjvTranslations(ImmutableSet.copyOf(value.split(", ")));
    }
  }
}

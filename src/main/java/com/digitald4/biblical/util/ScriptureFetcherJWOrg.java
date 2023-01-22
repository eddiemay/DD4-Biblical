package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import javax.inject.Inject;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class ScriptureFetcherJWOrg implements ScriptureFetcher {
  public static final String URL = "https://www.jw.org/en/library/bible/study-bible/books/%s/%d/";

  private final APIConnector apiConnector;

  @Inject
  public ScriptureFetcherJWOrg(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    String htmlResult = apiConnector.sendGet(String.format(URL, formatBookForUrl(book.getName()), chapter));
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    Elements verses = doc.getElementsByClass("verse");
    if (verses.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    return verses.stream()
        .peek(verse -> {
          verse.getElementsByClass("newblock").forEach(element -> element.replaceWith(new TextNode(" ", "")));
          verse.getElementsByClass("xrefLink").forEach(Element::remove);
          verse.getElementsByClass("chapterNum").forEach(Element::remove);
          verse.getElementsByClass("verseNum").forEach(Element::remove);
          verse.getElementsByClass("footnoteLink").forEach(Element::remove);
        })
        .map(
            verse -> new Scripture()
                .setVersion(version)
                .setBook(book.getName())
                .setChapter(chapter)
                .setVerse(Integer.parseInt(verse.id().substring(verse.id().length() - 3)))
                .setText(new StringBuilder(ScriptureFetcher.trim(verse.text()))))
        .collect(toImmutableList());
  }

  private static String formatBookForUrl(String book) {
    return book.toLowerCase().replace(" ", "-");
  }
}

package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import javax.inject.Inject;

public class ScriptureFetcherBibleCom  implements ScriptureFetcher {
  public static final String URL = "https://www.bible.com/bible/316/%s.%d.%s";

  private final APIConnector apiConnector;

  @Inject
  public ScriptureFetcherBibleCom(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, String language, BibleBook book, int chapter) {
    String htmlResult = apiConnector.sendGet(String.format(URL, formatBookForUrl(book.name()), chapter, version));
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    Elements wrappers = doc.getElementsByClass("chapter");
    if (wrappers.isEmpty()) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    return wrappers.get(0).getElementsByClass("verse").stream()
        .map(verse -> new Scripture()
            .setVersion(version)
            .setBook(book.name())
            .setChapter(chapter)
            .setVerse(Integer.parseInt(verse.getElementsByClass("label").get(0).ownText()))
            .setText(new StringBuilder(ScriptureFetcher.trim(verse.getElementsByClass("content").get(0).text()))))
        .collect(toImmutableList());
  }

  private static String formatBookForUrl(String book) {
    return book.replace(" ", "").substring(0, 3).toUpperCase();
  }
}

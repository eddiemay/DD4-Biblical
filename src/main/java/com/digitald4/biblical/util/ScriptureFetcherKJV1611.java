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

public class ScriptureFetcherKJV1611 implements ScriptureFetcher {
  private static final String URL = "https://www.kingjamesbibleonline.org/1611_%s-Chapter-%d/";

  private final APIConnector apiConnector;

  @Inject
  public ScriptureFetcherKJV1611(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter) {
    String htmlResult = apiConnector.sendGet(String.format(URL + "/", formatBookForUrl(book.getName()), chapter));
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    Elements wrappers = doc.getElementsByClass("bx-wrapper");
    if (wrappers.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    return wrappers.get(0).getElementsByTag("p").stream()
        .filter(p -> p.getElementsByTag("span").size() > 0)
        .map(
            p -> new Scripture()
                .setVersion(version)
                .setBook(book.getName())
                .setChapter(chapter)
                .setVerse(Integer.parseInt(p.getElementsByTag("span").get(0).ownText()))
                .setText(new StringBuilder(p.ownText().trim())))
        .collect(toImmutableList());
  }

  private static String formatBookForUrl(String book) {
    if (book.equals(BibleBook.SIRACH.getName())) {
      return "Ecclesiasticus";
    }

    return book.replace(" ", "-");
  }
}

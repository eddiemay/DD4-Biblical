package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;

public class ScriptureFetcherOneOff implements ScriptureFetcher {
  private final APIConnector apiConnector;

  @Inject
  public ScriptureFetcherOneOff(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public synchronized ImmutableList<Scripture> fetch(String version, String language, BibleBook book, int chapter) {
    if (book.name().equals(BibleBook.ISAIAH)) {
      return language.equals(Language.HEBREW)
          ? fetchDSSIsaiahHe(version, book) : fetchDSSIsaiahEn(version, book, chapter);
    }

    // Temporary code until we can find Hebrews sources for the other DSS books.
    if (Language.HEBREW.equals(language)) {
      return ImmutableList.of();
    }

    if (book.name().equals(BibleBook.COMMUNITY_RULE)) {
      return fetchCommunityRule(version, book);
    } else if (book.name().equals(BibleBook.WAR_SCROLL)) {
      return fetchWarScroll(version, book);
    } else if (book.name().equals(BibleBook.GIANTS)) {
      return fetchGiants(version, book);
    } else if (book.name().equals(BibleBook.JOSEPHUS)) {
      return fetchJosephus(version, book, chapter);
    } else if (book.name().equals(BibleBook.TESTAMENT_OF_JOB)) {
      return fetchTestamentOfJob(version, book);
    } else if (book.name().equals(BibleBook.ENOCH_3)) {
      return fetch3Enoch(version, book);
    } else if (book.name().equals(BibleBook.GAD_THE_SEER)) {
      return fetchGadTheSeer(version, book, chapter);
    } else if (book.name().equals(BibleBook.LIVES_OF_THE_PROPHETS)) {
      return fetchLivesOfTheProphets(version, book);
    } else if (book.name().equals(BibleBook.BARUCH_2)) {
      return fetch2Baruch(version, book);
    } else if (book.name().equals(BibleBook.CLEMENT_1)) {
      return fetch1Clem(version, book);
    } else if (book.name().equals(BibleBook.ODES_OF_PEACE)) {
      return fetchOdesOfPeace(version, book);
    } else if (book.name().equals(BibleBook.JUBILEES)) {
      return fetchJubileesOpenSiddur(version, book, language);
    }

    throw new DD4StorageException("Unknown oneoff fetch request for book: " + book);
  }

  private synchronized ImmutableList<Scripture> fetchCommunityRule(String version, BibleBook book) {
    String htmlResult = apiConnector.sendGet("https://www.essene.com/History&Essenes/md.htm");
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    Elements paragraphs = doc.getElementsByTag("p");
    if (paragraphs.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    AtomicInteger verse = new AtomicInteger();
    return paragraphs.stream()
        .filter(p -> {
          Elements fonts = p.getElementsByTag("font");
          return fonts.size() == 0 || fonts.get(0).attr("size").isEmpty();
        })
        .map(Element::text)
        .map(String::trim)
        .filter(text -> !text.isEmpty())
        .map(text -> new Scripture()
            .setVersion(version)
            .setBook(book.name())
            .setChapter(1)
            .setVerse(verse.incrementAndGet())
            .setText(text))
        .collect(toImmutableList());
  }

  private synchronized ImmutableList<Scripture> fetchWarScroll(String version, BibleBook book) {
    final Pattern versePattern = Pattern.compile("\\((\\d+)\\) ([^<]+)");
    String htmlResult = apiConnector.sendGet("https://www.qumran.org/js/qumran/hss/1qm");
    Document doc = Jsoup.parse(htmlResult.trim());
    Elements rows = doc.getElementsByTag("tr");
    if (rows.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    AtomicInteger chapter = new AtomicInteger();
    return rows.stream()
        .map(Element::text)
        .map(versePattern::matcher)
        .filter(Matcher::matches)
        .map(
            matcher -> {
              int verse = Integer.parseInt(matcher.group(1));
              if (verse == 1) {
                chapter.incrementAndGet();
              }

              return new Scripture()
                  .setVersion(version)
                  .setBook(book.name())
                  .setChapter(chapter.get())
                  .setVerse(verse)
                  .setText(new StringBuilder(matcher.group(2)));
            })
        .collect(toImmutableList());
  }

  private synchronized ImmutableList<Scripture> fetch3Enoch(String version, BibleBook book) {
    final Pattern versePattern = Pattern.compile("\\((\\d+)\\) ([^(]+)");
    final Pattern chapterPattern = Pattern.compile("(.*) (Chapter|CHAPTER) ([IVXL]+) (.*)");
    String htmlResult =
        apiConnector.sendGet("https://rejectedscriptures.weebly.com/the-third-book-of-enoch.html");
    Document doc = Jsoup.parse(htmlResult.trim());
    Elements divs = doc.getElementsByTag("div");
    if (divs.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    AtomicInteger chapter = new AtomicInteger();
    return divs.stream()
        .filter(div -> div.hasClass("paragraph"))
        .map(Element::text)
        .map(versePattern::matcher)
        .flatMap(
            matcher -> {
              ImmutableList.Builder<Scripture> builder = ImmutableList.builder();
              while (matcher.find()) {
                int verse = Integer.parseInt(matcher.group(1));
                if (verse == 1) {
                  chapter.incrementAndGet();
                }

                String text = matcher.group(2).trim();
                Matcher chapterMatcher = chapterPattern.matcher(text);
                if (chapterMatcher.matches()) {
                  text = chapterMatcher.group(1).trim();
                }

                builder.add(new Scripture()
                    .setVersion(version)
                    .setBook(book.name())
                    .setChapter(chapter.get())
                    .setVerse(verse)
                    .setText(new StringBuilder(text)));
              }

              return builder.build().stream();
            })
        .collect(toImmutableList());
  }

  private synchronized ImmutableList<Scripture> fetchGiants(String version, BibleBook book) {
    final Pattern versePattern = Pattern.compile("(\\d+) ([^<]+)");
    String htmlResult = apiConnector.sendGet("http://www.gnosis.org/library/dss/dss_book_of_giants.htm");
    Document doc = Jsoup.parse(htmlResult.trim());
    Elements blockquotes = doc.getElementsByTag("blockquote").get(1).getElementsByTag("blockquote");
    if (blockquotes.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    AtomicInteger chapter = new AtomicInteger();
    AtomicInteger verse = new AtomicInteger();

    return blockquotes.stream()
        .skip(1)
        .peek(element -> {
          chapter.incrementAndGet();
          verse.set(0);
        })
        .flatMap(element -> element.getElementsByTag("p").stream())
        .map(Element::text)
        .map(ScriptureFetcher::trim)
        .filter(text -> !text.isEmpty())
        .map(text -> new Scripture()
            .setVersion(version)
            .setBook(book.name())
            .setChapter(chapter.get())
            .setVerse(verse.incrementAndGet())
            .setText(new StringBuilder(text)))
        .collect(toImmutableList());
  }

  private synchronized ImmutableList<Scripture> fetchJosephus(
      String version, BibleBook book, int chapter) {
    String urlTemplate = "http://penelope.uchicago.edu/josephus/ant-%d.html";
    final Pattern isNumber = Pattern.compile("(\\d+)");

    String htmlResult = apiConnector.sendGet(String.format(urlTemplate, chapter));
    Document doc = Jsoup.parse(htmlResult.trim());
    Elements paragraphs = doc.getElementsByTag("p");
    if (paragraphs.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    AtomicInteger verse = new AtomicInteger();
    return paragraphs.stream()
        .filter(p -> p.hasClass("indent") || p.hasClass("noindent"))
        .peek(p -> p.getElementsByTag("a").stream()
            .filter(a -> isNumber.matcher(a.text()).find())
            .forEach(Element::remove))
        .map(Element::text)
        .map(ScriptureFetcher::trim)
        .map(text -> new Scripture()
            .setVersion(version)
            .setBook(book.name())
            .setChapter(chapter)
            .setVerse(verse.incrementAndGet())
            .setText(new StringBuilder(text)))
        .collect(toImmutableList());
  }

  private synchronized ImmutableList<Scripture> fetchTestamentOfJob(String version, BibleBook book) {
    String urlTemplate = "http://dd4-biblical.appspot.com/books/testament_of_job.html";
    final Pattern versePattern = Pattern.compile("(\\d+)\\.? (.+)");
    AtomicInteger chapter = new AtomicInteger();

    String htmlResult = apiConnector.sendGet(urlTemplate);
    Document doc = Jsoup.parse(htmlResult.trim());
    Elements paragraphs = doc.getElementsByTag("p");
    if (paragraphs.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    return paragraphs.stream()
        .map(Element::text)
        .map(versePattern::matcher)
        .filter(Matcher::matches)
        .map(matcher -> {
          int verse = Integer.parseInt(matcher.group(1));
          if (verse == 1) {
            chapter.incrementAndGet();
          }
          return new Scripture()
              .setVersion(version)
              .setBook(book.name())
              .setChapter(chapter.get())
              .setVerse(verse)
              .setText(new StringBuilder(matcher.group(2)));
        })
        .collect(toImmutableList());
  }

  private synchronized ImmutableList<Scripture> fetchGadTheSeer(String version, BibleBook book, int chapter) {
    Pattern chapterPattern = Pattern.compile("(\\d+)\\. ([\\w’,\\- ]+)");
    Pattern versePattern = Pattern.compile("(\\d+)\\s+(\\D+)");
    Pattern referencePattern = Pattern.compile("(\\w+) (\\d+):(\\d+)");
    String response = apiConnector.sendGet("http://dd4-biblical.appspot.com/books/gad_the_seer.txt");
    try(BufferedReader reader = new BufferedReader(new StringReader(response))) {
      String line;
      int skipLines = 40;
      int skipped = 0;
      while (skipped++ < skipLines) {
        reader.readLine();
      }
      int pageChapter = 0;
      String chapterTitle = null;
      do {
        line = reader.readLine();
        Matcher matcher = chapterPattern.matcher(line);
        if (matcher.find()) {
          pageChapter = Integer.parseInt(matcher.group(1));
          chapterTitle = matcher.group(2);
        }
      } while (pageChapter < chapter);

      StringBuilder content = new StringBuilder();
      do {
        Matcher reference = referencePattern.matcher(line);
        if (reference.find()) {
          System.out.println("Found reference: " + reference.group(0));
          line = new StringBuilder(line).replace(reference.start(), reference.end(), "").toString();
        }
        if (!line.isEmpty()) {
          content.append(" ").append(line);
        }
        // System.out.println(line);
        line = reader.readLine();
      } while (!line.contains(" ––"));

      // System.out.println(content);
      System.out.println("Chapter title: " + chapterTitle);
      reader.close();
      Matcher matcher = versePattern.matcher(content);
      ImmutableList.Builder<Scripture> scriptures = ImmutableList.builder();
      while (matcher.find()) {
        String text = matcher.group(2).trim().replaceAll("\n", "");
        if (!text.isEmpty()) {
          scriptures.add(new Scripture()
              .setVersion(version)
              .setBook(book.name())
              .setLanguage("en")
              .setChapter(chapter)
              .setVerse(Integer.parseInt(matcher.group(1)))
              .setText(text));
        }
      }
      return scriptures.build();
    } catch (IOException ioe) {
      throw new DD4StorageException(
          "Error reading book" + book, ioe, ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  public synchronized ImmutableList<Scripture> fetchLivesOfTheProphets(String version, BibleBook book) {
    final Pattern versePattern = Pattern.compile("(\\d+)\\s+(\\D+)");
    String htmlResult = apiConnector.sendGet("http://dd4-biblical.appspot.com/books/lives_of_the_prophets.html");
    Document doc = Jsoup.parse(htmlResult.trim());
    Elements divs = doc.getElementsByTag("div");
    if (divs.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }

    AtomicInteger chapter = new AtomicInteger();

    return divs.stream()
        .map(div -> div.getElementsByTag("p").get(0))
        .map(Element::text)
        .flatMap(text -> {
          chapter.incrementAndGet();
          Matcher matcher = versePattern.matcher(text);
          ImmutableList.Builder<Scripture> scriptures = ImmutableList.builder();
          while (matcher.find()) {
            scriptures.add(new Scripture()
                .setVersion(version)
                .setBook(book.name())
                .setChapter(chapter.get())
                .setVerse(Integer.parseInt(matcher.group(1)))
                .setText(new StringBuilder(matcher.group(2).trim())));
          }
          return scriptures.build().stream();
        })
        .collect(toImmutableList());
  }

  private synchronized ImmutableList<Scripture> fetch2Baruch(String version, BibleBook book) {
    final Pattern versePattern = Pattern.compile("2 BARUCH (\\d+):(\\d+)\\s+(\\D+)");
    String text = apiConnector.sendGet("http://dd4-biblical.appspot.com/books/2_baruch.txt");
    Matcher matcher = versePattern.matcher(text);
    ImmutableList.Builder<Scripture> scriptures = ImmutableList.builder();
    while (matcher.find()) {
      scriptures.add(new Scripture()
          .setVersion(version)
          .setBook(book.name())
          .setChapter(Integer.parseInt(matcher.group(1)))
          .setVerse(Integer.parseInt(matcher.group(2)))
          .setText(matcher.group(3).trim()));
    }

    return scriptures.build();
  }

  private synchronized ImmutableList<Scripture> fetch1Clem(String version, BibleBook book) {
    final Pattern versePattern = Pattern.compile("1 Clem\\. (\\d+):(\\d+)\\s+(\\D+)");
    String text = apiConnector.sendGet("http://dd4-biblical.appspot.com/books/1_clem.txt");
    Matcher matcher = versePattern.matcher(text);
    ImmutableList.Builder<Scripture> scriptures = ImmutableList.builder();
    while (matcher.find()) {
      scriptures.add(new Scripture()
          .setVersion(version)
          .setBook(book.name())
          .setChapter(Integer.parseInt(matcher.group(1)))
          .setVerse(Integer.parseInt(matcher.group(2)))
          .setText(matcher.group(3).trim()));
    }

    return scriptures.build();
  }

  private synchronized ImmutableList<Scripture> fetchOdesOfPeace(String version, BibleBook book) {
    final Pattern chapterPattern = Pattern.compile("ODE (\\d+)\\s+(.+)");
    final Pattern versePattern = Pattern.compile("(\\d+)\\s+(\\D+)");
    String[] lines = apiConnector.sendGet("http://dd4-biblical.appspot.com/books/odes_of_peace.txt")
        .replaceAll("\u00a0", " ").split("\n");
    ImmutableList.Builder<Scripture> scriptures = ImmutableList.builder();
    for (String line : lines) {
      Matcher matcher = chapterPattern.matcher(line);
      if (matcher.matches()) {
        int chapter = Integer.parseInt(matcher.group(1));
        Matcher verseMatcher = versePattern.matcher(matcher.group(2));
        while (verseMatcher.find()) {
          scriptures.add(new Scripture()
              .setVersion(version)
              .setBook(book.name())
              .setChapter(chapter)
              .setVerse(Integer.parseInt(verseMatcher.group(1)))
              .setText(verseMatcher.group(2).trim()));
        }
      }
    }
    return scriptures.build();
  }

  private synchronized ImmutableList<Scripture> fetchDSSIsaiahHe(String version, BibleBook book) {
    final Pattern rangeMultiChapterPattern = // Col. XX, Isaiah 25:6–26:18
        Pattern.compile("Col\\. (\\w+), Isaiah (\\d+):(\\d+)–(\\d+):(\\d+)");
    final Pattern rangeSingleChapterPattern = // Col. I, Isaiah 1:1–26
        Pattern.compile("Col\\. (\\w+), Isaiah (\\d+):(\\d+)–(\\d+)");
    final Pattern verseLinePattern = Pattern.compile("(\\d+)\\s+(.+)");
    final Pattern chapterPattern = Pattern.compile("(\\d+):(\\d+)(.+)");
    final Pattern versePattern = Pattern.compile("(\\d+)\\s+(\\D+)");
    String[] lines = apiConnector.sendGet("http://dss-images-dot-dd4-biblical.appspot.com/books/1Q_Isaiah_a.txt")
        .replaceAll("\u00a0", " ").split("\n");
    final String fragment = "1QIsaA";
    ImmutableList.Builder<Scripture> scriptures = ImmutableList.builder();
    String col = null;
    int chapterStart;
    int verseStart;
    int chapterEnd;
    int verseEnd;
    int chapter = 0;
    Scripture lastScripture = null;
    for (String line : lines) {
      // System.out.println(line);
      Matcher rangeMatcher = rangeMultiChapterPattern.matcher(line);
      if (!rangeMatcher.matches()) {
        rangeMatcher = rangeSingleChapterPattern.matcher(line);
      }
      if (rangeMatcher.matches()) {
        col = rangeMatcher.group(1);
        chapterStart = Integer.parseInt(rangeMatcher.group(2));
        verseStart = Integer.parseInt(rangeMatcher.group(3));
        chapterEnd = rangeMatcher.groupCount() == 5 ? Integer.parseInt(rangeMatcher.group(4)) : chapterStart;
        verseEnd = rangeMatcher.groupCount() == 5
            ? Integer.parseInt(rangeMatcher.group(5)) : Integer.parseInt(rangeMatcher.group(4));
        chapter = chapterStart;

        // System.out.printf("Isaiah %d:%d-%d:%d\n", chapterStart, verseStart, chapterEnd, verseEnd);
      } else {
        Matcher verseLineMatcher = verseLinePattern.matcher(line);
        if (verseLineMatcher.matches()) {
          int lineNumber = Integer.parseInt(verseLineMatcher.group(1));
          String versePart = verseLineMatcher.group(2);
          Matcher chapterMatcher = chapterPattern.matcher(versePart);
          if (chapterMatcher.find()) {
            chapter = Integer.parseInt(chapterMatcher.group(1));
            // System.out.println(chapterMatcher.group(0));
            // System.out.println("Chapter: " + chapter);

            if (chapterMatcher.start() > 2) {
              lastScripture.getText().append(" ")
                  .append(ScriptureFetcher.trim(versePart.substring(0, chapterMatcher.start())));
            }
            versePart = chapterMatcher.group(3);

            int verse = Integer.parseInt(chapterMatcher.group(2));
            if (chapter == 8 && verse == 23) {
              chapter = 9;
              verse = 0;
            }
            scriptures.add(
                lastScripture = new Scripture()
                    .setVersion(version)
                    .setBook(book.name())
                    .setLanguage(Language.HEBREW)
                    .setChapter(chapter)
                    .setVerse(chapter == 9 ? verse + 1 : verse)
                    .setText("")
                    .setLocation(String.format("%s-%s-%d", fragment, col, lineNumber)));
          }
          Matcher verseMatcher = versePattern.matcher(versePart);
          if (!verseMatcher.find()) {
            lastScripture.getText().append(" ").append(ScriptureFetcher.trim(versePart));
          } else {
            if (verseMatcher.start() > 2) {
              lastScripture.getText().append(" ")
                  .append(ScriptureFetcher.trim(versePart.substring(0, verseMatcher.start())));
            }
            do {
              int verse = Integer.parseInt(verseMatcher.group(1));
              scriptures.add(
                  lastScripture = new Scripture()
                      .setVersion(version)
                      .setBook(book.name())
                      .setLanguage(Language.HEBREW)
                      .setChapter(chapter)
                      .setVerse(chapter == 9 ? verse + 1 : verse)
                      .setText(ScriptureFetcher.trim(verseMatcher.group(2)))
                      .setLocation(String.format("%s-%s-%d", fragment, col, lineNumber)));
            } while (verseMatcher.find());
          }
        }
      }
    }

    return scriptures.build().stream()
        .filter(scripture -> !scripture.getText().toString().isEmpty())
        .peek(scripture -> scripture.setText(scripture.getText().toString().trim()))
        .peek(s -> {
          if (s.getChapter() == 8 && s.getVerse() == 23) {
            s.setChapter(9).setVerse(1);
          }
        })
        .collect(toImmutableList());
  }

  private synchronized ImmutableList<Scripture> fetchDSSIsaiahEn(String version, BibleBook book, int chapter) {
    final String URL = "http://dss.collections.imj.org.il/api/get_translation?id=%d:%d&lang=en";
    ImmutableList.Builder<Scripture> scriptures = ImmutableList.builder();
    for (int verse = 1; verse <= 38; verse++) {
      JSONObject json = new JSONObject(apiConnector.sendGet(String.format(URL, chapter, verse)));
      if (json.has("text")) {
        scriptures.add(
            new Scripture()
                .setVersion(version)
                .setBook(book.name())
                .setLanguage("en")
                .setChapter(chapter)
                .setVerse(verse)
                .setText(json.getString("text")));
      }
    }
    return scriptures.build();
  }

  private synchronized ImmutableList<Scripture> fetchJubileesOpenSiddur(String version, BibleBook book, String lang) {
    final Pattern versePattern = Pattern.compile("(\\d+):(\\d+)(\\W+)");
    Document doc = Jsoup.parse(apiConnector.sendGet(
        "https://opensiddur.org/readings-and-sourcetexts/festival-and-fast-day-readings/jewish-readings/shavuot-readings/sefer-hayovelim-jubilees-preserved-in-geez/").trim());
    Elements divs = doc.getElementsByTag("div");
    if (divs.size() == 0) {
      throw new DD4StorageException("Unable to find scripture content");
    }
    return divs.stream()
        .filter(div -> div.attr("lang").equals(lang))
        .map(Element::text)
        .map(text -> text.replaceAll("&nbsp;", " "))
        .flatMap(text -> {
          ImmutableList.Builder<Scripture> scriptures = ImmutableList.builder();
          Matcher matcher = versePattern.matcher(text);
          while (matcher.find()) {
            scriptures.add(
                new Scripture()
                    .setVersion(version)
                    .setLanguage(lang)
                    .setBook(book.name())
                    .setChapter(Integer.parseInt(matcher.group(1)))
                    .setVerse(Integer.parseInt(matcher.group(2)))
                    .setText(matcher.group(3).trim()));
          }
          return scriptures.build().stream();
        })
        .collect(toImmutableList());
  }
}

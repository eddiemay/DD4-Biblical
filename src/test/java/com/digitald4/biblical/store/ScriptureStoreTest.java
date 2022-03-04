package com.digitald4.biblical.store;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.util.ScriptureFetcher;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.VerseRange;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import com.google.appengine.api.search.Index;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ScriptureStoreTest {
  private static final String VERSION = "ISR";

  @Mock private final DAO dao = mock(DAO.class);
  @Mock private final Index searchIndex = mock(Index.class);
  @Mock private final ScriptureFetcher scriptureFetcher = mock(ScriptureFetcher.class);

  private final ScriptureReferenceProcessor scriptureRefProcessor = new ScriptureReferenceProcessorSplitImpl();
  private ScriptureStore scriptureStore;

  @Before
  public void setup() {
    scriptureStore = new ScriptureStore(() -> dao, searchIndex, scriptureRefProcessor, scriptureFetcher);

    when(dao.list(eq(Scripture.class), any(Query.List.class))).then(
        i -> getScriptures(i.getArgumentAt(1, Query.List.class)));
    when(dao.create(anyCollectionOf(Scripture.class))).then(i -> i.getArgumentAt(0, ImmutableList.class));
    when(scriptureFetcher.fetch(anyString(), any(BibleBook.class), anyInt())).then(i ->
        fetchFromWeb(
            i.getArgumentAt(0, String.class), i.getArgumentAt(1, BibleBook.class), i.getArgumentAt(2, int.class)));
    when(scriptureFetcher.getChapterUrl(anyString(), any(VerseRange.class)))
        .then(i -> getChapterUrl(i.getArgumentAt(0, String.class), i.getArgumentAt(1, VerseRange.class)));
    when(scriptureFetcher.getVerseUrl(any(Scripture.class)))
        .then(i -> getVerseUrl(i.getArgumentAt(0, Scripture.class)));
  }

  private static QueryResult<Scripture> getScriptures(Query.List query) {
    BibleBook book = BibleBook.get(query.getFilters().get(0).getVal());
    int chapter = query.getFilters().get(1).getVal();
    int startVerse = query.getFilters().get(2).getVal();
    int endVerse = (int) query.getFilters().stream()
        .filter(filter -> filter.getColumn().equals("verse") && filter.getOperator().equals("<="))
        .map(Query.Filter::getVal)
        .findFirst()
        .orElse(startVerse);
    String version = (String) query.getFilters().stream()
        .filter(filter -> filter.getColumn().equals("version"))
        .map(Query.Filter::getVal)
        .findFirst()
        .orElse(null);
    endVerse = Math.min(endVerse, 37);

    if (chapter != 23) {
      return QueryResult.of(ImmutableList.of(), 0, query);
    }

    return QueryResult.of(
        IntStream.range(startVerse, endVerse + 1)
            .boxed()
            .flatMap(verse -> createScriptures(version, book.getName(), chapter, verse, "[Scripture Placeholder]"))
            .collect(toImmutableList()),
        endVerse + 1 - startVerse, query);
  }

  private static ImmutableList<Scripture> fetchFromWeb(String version, BibleBook book, int chapter) {
    return IntStream.range(1, 38)
        .mapToObj(verse -> createScripture(version, book.getName(), chapter, verse, "[Fetched From Web]"))
        .collect(toImmutableList());
  }

  private static String getChapterUrl(String version, VerseRange verseRange) {
    String book = verseRange.getBook().getName().toLowerCase().replace(" ", "_");
    return String.format("https://chapterlink/%s/%s/%d.html", version.toLowerCase(), book, verseRange.getChapter());
  }

  private static String getVerseUrl(Scripture scripture) {
    String book = scripture.getBook().toLowerCase().replace(" ", "_");
    return String.format("https://verselink/%s/%d-%d.html", book, scripture.getChapter(), scripture.getVerse());
  }

  private static Stream<Scripture> createScriptures(String version, String book, int chapter, int verse, String text) {
    if (version == null) {
      return Stream.of(
          createScripture("ISR", book, chapter, verse, text),
          createScripture("KJV", book, chapter, verse, text));
    }

    return Stream.of(createScripture(version, book, chapter, verse, text));
  }

  private static Scripture createScripture(String version, String book, int chapter, int verse, String text) {
    return new Scripture().setVersion(version).setBook(book).setChapter(chapter).setVerse(verse).setText(text);
  }

  @Test
  public void getScriptures() {
    assertThat(scriptureStore.getScriptures(VERSION, "2 Kings 23:5")).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, "2 Kings 23:5-7")).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(6).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(7).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, "2 Kings 23:5,7")).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(7).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, "2 Kings 23:5-7,9")).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(6).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(7).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(9).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, "2 Kings 23:5,9-11")).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(9).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(10).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(11).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, "2 Kings 23:5-7,9-11")).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(6).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(7).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(9).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(10).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(11).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, "2 Kings 23:35-24:3")).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(35).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(36).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(37).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(24).setVerse(1).setText("[Fetched From Web]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(24).setVerse(2).setText("[Fetched From Web]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(24).setVerse(3).setText("[Fetched From Web]"));

    assertThat(scriptureStore.getScriptures(VERSION, "2 Kings 23")).hasSize(37);
  }

  @Test
  public void getScriptureText() {
    assertThat(scriptureStore.getScripturesText("ISR", "2 Kings 23:5"))
        .isEqualTo("2 Kings 23:5 [Scripture Placeholder]");
    assertThat(scriptureStore.getScripturesText("ISR", "2 Kings 23:5-7"))
        .isEqualTo("2 Kings 23:5 [Scripture Placeholder] 6 [Scripture Placeholder] 7 [Scripture Placeholder]");
    assertThat(scriptureStore.getScripturesText("ISR", "2 Kings 23:5,7"))
        .isEqualTo("2 Kings 23:5 [Scripture Placeholder]\n\n2 Kings 23:7 [Scripture Placeholder]");
  }

  @Test
  public void getScriptureTextAllVersions() {
    assertThat(scriptureStore.getScripturesTextAllVersions("2 Kings 23:5"))
        .isEqualTo("(ISR) 2 Kings 23:5 [Scripture Placeholder]\n(KJV) 2 Kings 23:5 [Scripture Placeholder]");
    assertThat(scriptureStore.getScripturesTextAllVersions("2 Kings 23:5-7"))
        .isEqualTo("(ISR) 2 Kings 23:5 [Scripture Placeholder]\n(KJV) 2 Kings 23:5 [Scripture Placeholder]\n" +
            "(ISR) 2 Kings 23:6 [Scripture Placeholder]\n(KJV) 2 Kings 23:6 [Scripture Placeholder]\n" +
            "(ISR) 2 Kings 23:7 [Scripture Placeholder]\n(KJV) 2 Kings 23:7 [Scripture Placeholder]");
    assertThat(scriptureStore.getScripturesTextAllVersions("2 Kings 23:5,7"))
        .isEqualTo("(ISR) 2 Kings 23:5 [Scripture Placeholder]\n(KJV) 2 Kings 23:5 [Scripture Placeholder]\n\n" +
            "(ISR) 2 Kings 23:7 [Scripture Placeholder]\n(KJV) 2 Kings 23:7 [Scripture Placeholder]");
  }

  @Test
  public void getScriptures_verseOutOfBounds() {
    try {
      scriptureStore.getScriptures(VERSION, "2 Kings 23:42-46");
      fail("Should not have got here");
    } catch (DD4StorageException e) {
      assertThat(e).hasMessageThat().contains("Verse 42 out of bounds for: (ISR) 2 Kings 23");
    }
  }

  @Test
  public void expandScriptures() {
    assertThat(scriptureStore.expandScriptures(VERSION,"<div scripture-reference=\"Genesis 1:1\"></div>", true)).isEqualTo(
        "<div><p><a href=\"https://chapterlink/isr/genesis/1.html\" target=\"scripture\">Genesis 1</a>:" +
            "<a href=\"https://verselink/genesis/1-1.html\" target=\"scripture\">1</a> [Fetched From Web]</p></div>");
    assertThat(scriptureStore.expandScriptures(VERSION,"<div scripture-reference=\"2 Kings 23:15\"></div>", false)).isEqualTo(
        "<div scripture-reference=\"2 Kings 23:15\"><p>2 Kings 23:15 [Scripture Placeholder]</p></div>");
    assertThat(scriptureStore.expandScriptures(VERSION,"<div scripture-reference=\"Job 20:15-17\"></div>", true)).isEqualTo(
        "<div><p><a href=\"https://chapterlink/isr/job/20.html\" target=\"scripture\">Job 20</a>:" +
            "<a href=\"https://verselink/job/20-15.html\" target=\"scripture\">15</a> [Fetched From Web] " +
            "<a href=\"https://verselink/job/20-16.html\" target=\"scripture\">16</a> [Fetched From Web] " +
            "<a href=\"https://verselink/job/20-17.html\" target=\"scripture\">17</a> [Fetched From Web]</p></div>");
    assertThat(scriptureStore.expandScriptures(VERSION,"<div scripture-reference=\"Job 23:15,17\"></div>", false)).isEqualTo(
        "<div scripture-reference=\"Job 23:15,17\"><p>Job 23:15 [Scripture Placeholder]</p><p>Job 23:17 [Scripture Placeholder]</p></div>");
  }

  @Test
  public void expandScriptures_fallback() {
    assertThat(scriptureStore.expandScriptures(VERSION,"<div scripture-reference=\"1 Maccabees 4:28\"></div>", true)).isEqualTo(
        "<div><p><a href=\"https://chapterlink/nrsv/1_maccabees/4.html\" target=\"scripture\">1 Maccabees 4</a>:" +
            "<a href=\"https://verselink/1_maccabees/4-28.html\" target=\"scripture\">28</a> [Fetched From Web]</p></div>");
  }

  @Test
  public void getChapterLink() {
    assertThat(scriptureStore.getChapterLink(VERSION, new ScriptureReferenceProcessor.VerseRange(BibleBook.Genesis, 2, 3, 3)))
        .isEqualTo("<a href=\"https://chapterlink/isr/genesis/2.html\" target=\"scripture\">Genesis 2</a>");
  }

  @Test
  public void getVerseLink() {
    assertThat(scriptureStore.getVerseLink(new Scripture().setVersion(VERSION).setBook("Genesis").setChapter(2).setVerse(3)))
        .isEqualTo("<a href=\"https://verselink/genesis/2-3.html\" target=\"scripture\">3</a>");
  }
}

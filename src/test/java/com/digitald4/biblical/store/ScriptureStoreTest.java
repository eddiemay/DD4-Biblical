package com.digitald4.biblical.store;

import static com.digitald4.biblical.model.BibleBook.EN;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.truth.Truth.assertThat;
import static java.util.function.Function.identity;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.util.ScriptureFetcher;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SearchIndexer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ScriptureStoreTest {
  private static final String VERSION = "ISR";

  @Mock private final DAO dao = mock(DAO.class);
  @Mock private final SearchIndexer searchIndexer = mock(SearchIndexer.class);
  @Mock private final ScriptureFetcher scriptureFetcher = mock(ScriptureFetcher.class);

  private final ScriptureReferenceProcessor scriptureRefProcessor = new ScriptureReferenceProcessorSplitImpl();
  private ScriptureStore scriptureStore;

  @Before
  public void setup() {
    scriptureStore =
        new ScriptureStore(() -> dao, searchIndexer, scriptureRefProcessor, scriptureFetcher);

    when(dao.list(eq(Scripture.class), any(Query.List.class))).then(
        i -> getScriptures(i.getArgument(1)));
    when(dao.create(anyCollectionOf(Scripture.class))).then(i -> i.getArgument(0));
    when(scriptureFetcher.fetch(anyString(), anyString(), any(BibleBook.class), anyInt())).then(i ->
        fetchFromWeb(i.getArgument(0), i.getArgument(2), i.getArgument(3)));
  }

  private static QueryResult<Scripture> getScriptures(Query.List query) {
    ImmutableMap<String, Filter> filtersByColumnOp =
        query.getFilters().stream().collect(toImmutableMap(filter -> filter.getColumn() + filter.getOperator(), identity()));
    BibleBook book = BibleBook.get(filtersByColumnOp.get("book=").getVal());
    int chapter = filtersByColumnOp.get("chapter=").getVal();
    int startVerse = filtersByColumnOp.get("verse>=").getVal();
    int endVerse = filtersByColumnOp.getOrDefault("verse<=", Filter.of("verse<=", startVerse)).getVal();
    String version = filtersByColumnOp.getOrDefault("version=", Filter.of("version=", null)).getVal();
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

  private static Stream<Scripture> createScriptures(String version, String book, int chapter, int verse, String text) {
    if (version == null) {
      return Stream.of(
          createScripture("ISR", book, chapter, verse, text),
          createScripture("RSKJ", book, chapter, verse, text));
    }

    return Stream.of(createScripture(version, book, chapter, verse, text));
  }

  private static Scripture createScripture(String version, String book, int chapter, int verse, String text) {
    return new Scripture().setVersion(version).setBook(book).setChapter(chapter).setVerse(verse).setText(text);
  }

  @Test
  public void getScriptures() {
    assertThat(scriptureStore.getScriptures(VERSION, EN, "2 Kings 23:5").getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, EN,  "2 Kings 23:5-7").getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(6).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(7).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, EN,  "2 Kings 23:5,7").getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(7).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, EN,  "2 Kings 23:5-7,9").getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(6).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(7).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(9).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, EN,  "2 Kings 23:5,9-11").getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(9).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(10).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(11).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, EN,  "2 Kings 23:5-7,9-11").getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(6).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(7).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(9).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(10).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(11).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, EN,  "2 Kings 23:35-24:3").getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(35).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(36).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(37).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(24).setVerse(1).setText("[Fetched From Web]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(24).setVerse(2).setText("[Fetched From Web]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(24).setVerse(3).setText("[Fetched From Web]"));

    assertThat(scriptureStore.getScriptures(VERSION, EN,  "2 Kings 23").getItems()).hasSize(37);
  }

  @Test
  public void getScriptureTextAllVersions() {
    assertThat(scriptureStore.getScripturesTextAllVersions(EN, "2 Kings 23:5"))
        .isEqualTo("(ISR) 2 Kings 23:5 [Scripture Placeholder]\n(RSKJ) 2 Kings 23:5 [Scripture Placeholder]");
    assertThat(scriptureStore.getScripturesTextAllVersions(EN, "2 Kings 23:5-7"))
        .isEqualTo("(ISR) 2 Kings 23:5 [Scripture Placeholder]\n(RSKJ) 2 Kings 23:5 [Scripture Placeholder]\n" +
            "(ISR) 2 Kings 23:6 [Scripture Placeholder]\n(RSKJ) 2 Kings 23:6 [Scripture Placeholder]\n" +
            "(ISR) 2 Kings 23:7 [Scripture Placeholder]\n(RSKJ) 2 Kings 23:7 [Scripture Placeholder]");
    assertThat(scriptureStore.getScripturesTextAllVersions(EN, "2 Kings 23:5,7"))
        .isEqualTo("(ISR) 2 Kings 23:5 [Scripture Placeholder]\n(RSKJ) 2 Kings 23:5 [Scripture Placeholder]\n\n" +
            "(ISR) 2 Kings 23:7 [Scripture Placeholder]\n(RSKJ) 2 Kings 23:7 [Scripture Placeholder]");
  }

  @Test
  public void getScriptures_verseOutOfBounds() {
    try {
      scriptureStore.getScriptures(VERSION, EN, "2 Kings 23:42-46");
      fail("Should not have got here");
    } catch (DD4StorageException e) {
      assertThat(e).hasMessageThat().contains("Verse 42 out of bounds for: (ISR) 2 Kings 23");
    }
  }
}

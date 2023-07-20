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
        fetchFromWeb(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3)));
  }

  private static QueryResult<Scripture> getScriptures(Query.List query) {
    ImmutableMap<String, Object> filterValueByColumnOp =
        query.getFilters().stream().collect(
            toImmutableMap(filter -> filter.getColumn() + filter.getOperator(), Filter::getVal));
    BibleBook book = BibleBook.get((String) filterValueByColumnOp.get("book="));
    int chapter = (Integer) filterValueByColumnOp.get("chapter=");
    int startVerse = (Integer) filterValueByColumnOp.get("verse>=");
    int endVerse = (Integer) filterValueByColumnOp.getOrDefault("verse<=", startVerse);
    String version = (String) filterValueByColumnOp.getOrDefault("version=", null);
    String locale = (String) filterValueByColumnOp.getOrDefault("locale=", "en");
    endVerse = Math.min(endVerse, 37);

    if (chapter != 23) {
      return QueryResult.of(ImmutableList.of(), 0, query);
    }

    return QueryResult.of(
        IntStream.range(startVerse, endVerse + 1)
            .boxed()
            .flatMap(verse -> createScriptures(version, locale, book.getName(), chapter, verse, "[Scripture Placeholder]"))
            .collect(toImmutableList()),
        endVerse + 1 - startVerse, query);
  }

  private static ImmutableList<Scripture> fetchFromWeb(
      String version, String locale, BibleBook book, int chapter) {
    return IntStream.range(1, 38)
        .mapToObj(verse -> createScripture(version, locale, book.getName(), chapter, verse, "[Fetched From Web]"))
        .collect(toImmutableList());
  }

  private static Stream<Scripture> createScriptures(
      String version, String locale, String book, int chapter, int verse, String text) {
    if (version == null) {
      return Stream.of(
          createScripture("ISR", locale, book, chapter, verse, text),
          createScripture("RSKJ", locale, book, chapter, verse, text));
    }

    return Stream.of(createScripture(version, locale, book, chapter, verse, text));
  }

  private static Scripture createScripture(String version, String locale, String book, int chapter, int verse, String text) {
    return new Scripture().setVersion(version).setLocale(locale).setBook(book).setChapter(chapter).setVerse(verse).setText(text);
  }

  @Test
  public void getScriptures() {
    assertThat(scriptureStore.getScriptures(VERSION, EN, "2 Kings 23:5").getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"));
    assertThat(scriptureStore.getScriptures(VERSION, null,  "2 Kings 23:5-7").getItems()).containsExactly(
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
  public void getScriptureFiltersDuplicates() {
    assertThat(scriptureStore.getScriptures(VERSION, EN, "2 Kings 23:5,5").getItems()).containsExactly(
       new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"));
  }

  @Test
  public void getScriptures_interlaced() {
    assertThat(
        scriptureStore.getScriptures(VERSION, BibleBook.INTERLACED, "2 Kings 23:5").getItems()).containsExactly(
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
            new Scripture().setVersion("WLCO").setLocale(BibleBook.HEBREW).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"));
    assertThat(
        scriptureStore.getScriptures(VERSION, BibleBook.INTERLACED, "2 Kings 23:3-5").getItems()).containsExactly(
            new Scripture().setVersion("WLCO").setLocale(BibleBook.HEBREW).setBook("2 Kings").setChapter(23).setVerse(3).setText("[Scripture Placeholder]"),
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(3).setText("[Scripture Placeholder]"),
            new Scripture().setVersion("WLCO").setLocale(BibleBook.HEBREW).setBook("2 Kings").setChapter(23).setVerse(4).setText("[Scripture Placeholder]"),
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(4).setText("[Scripture Placeholder]"),
            new Scripture().setVersion("WLCO").setLocale(BibleBook.HEBREW).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]")).inOrder();
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

  @Test
  public void uploadScripture() {
    String text = "\n"
        + "\t1 “And I will bind up the lame persons, and I will heal the sick, and I will bring back the wandering. 2 And I will feed them on My holy mountain, and I will be their Shepherd, and I will be closer to them than a garment is to their skin. 3 They will call Me, and I will say, Behold, here I am. And if they cross, they will not slip,” says the Lord.";
    assertThat(text.replaceAll("\u00a0", " ")).isEqualTo("\n\t1 “And I will bind up the lame persons, and I will heal the sick, and I will bring back the wandering. 2 And I will feed them on My holy mountain, and I will be their Shepherd, and I will be closer to them than a garment is to their skin. 3 They will call Me, and I will say, Behold, here I am. And if they cross, they will not slip,” says the Lord.");
    assertThat(scriptureStore.uploadScripture("CCC", "en", "APOCRYPHON OF EZEKIEL", 5, text, true)).containsExactly(
        new Scripture().setVersion("CCC").setLocale("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(1).setText(
            "“And I will bind up the lame persons, and I will heal the sick, and I will bring back the wandering."),
        new Scripture().setVersion("CCC").setLocale("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(2).setText(
            "And I will feed them on My holy mountain, and I will be their Shepherd, and I will be closer to them than a garment is to their skin."),
        new Scripture().setVersion("CCC").setLocale("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(3).setText(
            "They will call Me, and I will say, Behold, here I am. And if they cross, they will not slip,” says the Lord."));
  }

  @Test
  public void uploadScripture_withChapterText() {
    String text = "CHAPTER 5\n"
        + "\t1 “And I will bind up the lame persons, and I will heal the sick, and I will bring back the wandering. 2 And I will feed them on My holy mountain, and I will be their Shepherd, and I will be closer to them than a garment is to their skin. 3 They will call Me, and I will say, Behold, here I am. And if they cross, they will not slip,” says the Lord.";
    assertThat(scriptureStore.uploadScripture("CCC", "en", "APOCRYPHON OF EZEKIEL", 1, text, true)).containsExactly(
        new Scripture().setVersion("CCC").setLocale("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(1).setText(
            "“And I will bind up the lame persons, and I will heal the sick, and I will bring back the wandering."),
        new Scripture().setVersion("CCC").setLocale("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(2).setText(
            "And I will feed them on My holy mountain, and I will be their Shepherd, and I will be closer to them than a garment is to their skin."),
        new Scripture().setVersion("CCC").setLocale("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(3).setText(
            "They will call Me, and I will say, Behold, here I am. And if they cross, they will not slip,” says the Lord."));
  }
}

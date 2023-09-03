package com.digitald4.biblical.store;

import static com.digitald4.biblical.util.Language.EN;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import com.digitald4.biblical.util.Language;
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

  private static final StaticDataDAO staticDataDAO = new StaticDataDAO();
  private static final BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
  private final ScriptureReferenceProcessor scriptureRefProcessor =
      new ScriptureReferenceProcessorSplitImpl(bibleBookStore);
  private ScriptureStore scriptureStore;

  @Before
  public void setup() {
    scriptureStore = new ScriptureStore(
        () -> dao, searchIndexer, bibleBookStore, scriptureRefProcessor, scriptureFetcher);

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
    BibleBook book = bibleBookStore.get((String) filterValueByColumnOp.get("book="));
    int chapter = (Integer) filterValueByColumnOp.get("chapter=");
    int startVerse = (Integer) filterValueByColumnOp.get("verse>=");
    int endVerse = (Integer) filterValueByColumnOp.getOrDefault("verse<=", startVerse);
    String version = (String) filterValueByColumnOp.getOrDefault("version=", null);
    String language = (String) filterValueByColumnOp.getOrDefault("language=", "en");
    endVerse = Math.min(endVerse, 37);

    if (chapter != 23) {
      return QueryResult.of(ImmutableList.of(), 0, query);
    }

    return QueryResult.of(
        IntStream.range(startVerse, endVerse + 1)
            .boxed()
            .flatMap(verse -> createScriptures(version, language, book.name(), chapter, verse, "[Scripture Placeholder]"))
            .collect(toImmutableList()),
        endVerse + 1 - startVerse, query);
  }

  private static ImmutableList<Scripture> fetchFromWeb(
      String version, String language, BibleBook book, int chapter) {
    return IntStream.range(1, 38)
        .mapToObj(
            verse ->
                createScripture(version, language, book.name(), chapter, verse, "[Fetched " + version + " From Web]"))
        .collect(toImmutableList());
  }

  private static Stream<Scripture> createScriptures(
      String version, String language, String book, int chapter, int verse, String text) {
    if (version == null) {
      return Stream.of(
          createScripture("ISR", language, book, chapter, verse, text),
          createScripture("RSKJ", language, book, chapter, verse, text));
    }

    return Stream.of(createScripture(version, language, book, chapter, verse, text));
  }

  private static Scripture createScripture(String version, String language, String book, int chapter, int verse, String text) {
    return new Scripture().setVersion(version).setLanguage(language).setBook(book)
        .setChapter(chapter).setVerse(verse).setText(text);
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
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(24).setVerse(1).setText("[Fetched " + VERSION + " From Web]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(24).setVerse(2).setText("[Fetched " + VERSION + " From Web]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(24).setVerse(3).setText("[Fetched " + VERSION + " From Web]"));

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
        scriptureStore.getScriptures(VERSION, Language.INTERLACED, "2 Kings 23:5").getItems()).containsExactly(
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
            new Scripture().setVersion("WLCO").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"));
    assertThat(
        scriptureStore.getScriptures(VERSION, Language.INTERLACED, "2 Kings 23:3-5").getItems()).containsExactly(
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(3).setText("[Scripture Placeholder]"),
        new Scripture().setVersion("WLCO").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(3).setText("[Scripture Placeholder]"),
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(4).setText("[Scripture Placeholder]"),
        new Scripture().setVersion("WLCO").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(4).setText("[Scripture Placeholder]"),
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
    new Scripture().setVersion("WLCO").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]")).inOrder();
  }

  @Test
  public void getScriptures_interlacedNotSupported() {
    assertThat(scriptureStore.getScriptures(VERSION, Language.INTERLACED, "Matt 1:1").getItems())
        .containsExactly(
            new Scripture().setVersion(VERSION).setBook("Matthew").setChapter(1).setVerse(1).setText("[Fetched ISR From Web]"));
    assertThat(scriptureStore.getScriptures(VERSION, Language.INTERLACED, "Matt 5:17-19").getItems())
        .containsExactly(
            new Scripture().setVersion(VERSION).setBook("Matthew").setChapter(5).setVerse(17).setText("[Fetched ISR From Web]"),
            new Scripture().setVersion(VERSION).setBook("Matthew").setChapter(5).setVerse(18).setText("[Fetched ISR From Web]"),
            new Scripture().setVersion(VERSION).setBook("Matthew").setChapter(5).setVerse(19).setText("[Fetched ISR From Web]"));
  }

  @Test
  public void getScriptures_comparison() {
    assertThat(scriptureStore.getScriptures(VERSION, Language.INTERLACED, "Isa 56:6").getItems())
        .containsExactly(
            new Scripture().setVersion(VERSION).setBook("Isaiah").setChapter(56).setVerse(6).setText("[Fetched " + VERSION + " From Web]"),
            new Scripture().setVersion("WLCO").setBook("Isaiah").setChapter(56).setVerse(6).setLanguage(
                Language.HEBREW).setText("[Fetched WLCO From Web]"),
            new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(56).setVerse(6).setLanguage(
                Language.HEBREW).setText("[Fetched DSS From Web]"),
            new Scripture().setVersion("Audit").setBook("Isaiah").setChapter(56).setVerse(6).setLanguage(
                    Language.HEBREW)
                .setText("[Fetched <span class=\"diff-delete\">DSS</span><span class=\"diff-insert\">WLCO</span> From Web] LD: 4"),
            new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(56).setVerse(6).setLanguage(
                Language.EN).setText("[Fetched DSS From Web]")).inOrder();
    assertThat(scriptureStore.getScriptures(VERSION, Language.INTERLACED, "Isa 24:3-5").getItems())
        .containsExactlyElementsIn(
            IntStream.range(3, 6)
                .boxed()
                .flatMap(
                    verse -> Stream.of(
                        new Scripture().setVersion(VERSION).setBook("Isaiah").setChapter(24).setVerse(verse).setText("[Fetched " + VERSION + " From Web]"),
                        new Scripture().setVersion("WLCO").setBook("Isaiah").setChapter(24).setVerse(verse).setLanguage(
                            Language.HEBREW).setText("[Fetched WLCO From Web]"),
                        new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(24).setVerse(verse).setLanguage(
                            Language.HEBREW).setText("[Fetched DSS From Web]"),
                        new Scripture().setVersion("Audit").setBook("Isaiah").setChapter(24).setVerse(verse).setLanguage(
                                Language.HEBREW)
                            .setText("[Fetched <span class=\"diff-delete\">DSS</span><span class=\"diff-insert\">WLCO</span> From Web] LD: 4"),
                        new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(24).setVerse(verse).setLanguage(
                            Language.EN).setText("[Fetched DSS From Web]")))
                .collect(toImmutableList())).inOrder();
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
        new Scripture().setVersion("CCC").setLanguage("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(1).setText(
            "“And I will bind up the lame persons, and I will heal the sick, and I will bring back the wandering."),
        new Scripture().setVersion("CCC").setLanguage("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(2).setText(
            "And I will feed them on My holy mountain, and I will be their Shepherd, and I will be closer to them than a garment is to their skin."),
        new Scripture().setVersion("CCC").setLanguage("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(3).setText(
            "They will call Me, and I will say, Behold, here I am. And if they cross, they will not slip,” says the Lord."));
  }

  @Test
  public void uploadScripture_withChapterText() {
    String text = "CHAPTER 5\n"
        + "\t1 “And I will bind up the lame persons, and I will heal the sick, and I will bring back the wandering. 2 And I will feed them on My holy mountain, and I will be their Shepherd, and I will be closer to them than a garment is to their skin. 3 They will call Me, and I will say, Behold, here I am. And if they cross, they will not slip,” says the Lord.";
    assertThat(scriptureStore.uploadScripture("CCC", "en", "APOCRYPHON OF EZEKIEL", 1, text, true)).containsExactly(
        new Scripture().setVersion("CCC").setLanguage("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(1).setText(
            "“And I will bind up the lame persons, and I will heal the sick, and I will bring back the wandering."),
        new Scripture().setVersion("CCC").setLanguage("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(2).setText(
            "And I will feed them on My holy mountain, and I will be their Shepherd, and I will be closer to them than a garment is to their skin."),
        new Scripture().setVersion("CCC").setLanguage("en").setBook("Apocryphon of Ezekiel").setChapter(5).setVerse(3).setText(
            "They will call Me, and I will say, Behold, here I am. And if they cross, they will not slip,” says the Lord."));
  }
}

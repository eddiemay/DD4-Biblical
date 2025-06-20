package com.digitald4.biblical.store;

import static com.digitald4.biblical.model.ScriptureVersion.INTERLINEAR;
import static com.digitald4.biblical.util.Language.EN;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.Scripture.AuditScripture;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.biblical.util.Language;
import com.digitald4.biblical.util.MachineTranslator;
import com.digitald4.biblical.util.ScriptureFetcher;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.VerseRange;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.View;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.*;
import com.digitald4.common.storage.Query.Filter;
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
  @Mock private final InterlinearStore interlinearStore = mock(InterlinearStore.class);
  @Mock private final MachineTranslator machineTranslator = mock(MachineTranslator.class);

  private static final ChangeTracker changeTracker = new ChangeTracker(null, null, null, null);
  private static DAOFileDBImpl daoFileDB  = new DAOFileDBImpl(changeTracker);
  private static final BibleBookStore bibleBookStore = new BibleBookStore(() -> daoFileDB);
  private final ScriptureReferenceProcessor scriptureRefProcessor =
      new ScriptureReferenceProcessorSplitImpl(bibleBookStore);
  private ScriptureStore scriptureStore;

  @Before
  public void setup() {
    scriptureStore = new ScriptureStore(() -> dao, searchIndexer, bibleBookStore,
        scriptureRefProcessor, scriptureFetcher, interlinearStore, machineTranslator);

    when(dao.list(eq(Scripture.class), any(Query.List.class))).then(i -> getScriptures(i.getArgument(1)));
    when(dao.persist(any())).then(i -> i.getArgument(0));
    when(scriptureFetcher.fetch(anyString(), anyString(), any(BibleBook.class), anyInt())).then(i ->
        fetchFromWeb(i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3)));
  }

  private static QueryResult<Scripture> getScriptures(Query.List query) {
    ImmutableMap<String, Object> filterValueByColumnOp = query.getFilters().stream()
        .collect(toImmutableMap(filter -> filter.getColumn() + filter.getOperator(), Filter::getVal));
    BibleBook book = bibleBookStore.get((String) filterValueByColumnOp.get("book="));
    int chapter = (Integer) filterValueByColumnOp.get("chapter=");
    int startVerse = (Integer) filterValueByColumnOp.get("verse>=");
    int endVerse = (Integer) filterValueByColumnOp.getOrDefault("verse<=", startVerse);
    String version = (String) filterValueByColumnOp.getOrDefault("version=", null);
    String language = (String) filterValueByColumnOp.getOrDefault("language=", "en");
    endVerse = Math.min(endVerse, 37);

    if (chapter != 23) {
      return QueryResult.of(Scripture.class, ImmutableList.of(), 0, query);
    }

    return QueryResult.of(Scripture.class,
        IntStream.range(startVerse, endVerse + 1)
            .boxed()
            .flatMap(verse -> createScriptures(version, language, book.name(), chapter, verse, "[Scripture Placeholder]"))
            .collect(toImmutableList()),
        endVerse + 1 - startVerse, query);
  }

  private static ImmutableList<Scripture> fetchFromWeb(
      String version, String language, BibleBook book, int chapter) {
    return IntStream.range(1, 38)
        .mapToObj(verse ->
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

  private static Scripture createScripture(
      String version, String language, String book, int chapter, int verse, String text) {
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
        scriptureStore.getScriptures(VERSION, EN, "2 Kings 23:5", View.Interlaced).getItems()).containsExactly(
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
            new Scripture().setVersion("WLC").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"));
    assertThat(
        scriptureStore.getScriptures(VERSION, EN, "2 Kings 23:3-5", View.Interlaced).getItems()).containsExactly(
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(3).setText("[Scripture Placeholder]"),
            new Scripture().setVersion("WLC").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(3).setText("[Scripture Placeholder]"),
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(4).setText("[Scripture Placeholder]"),
            new Scripture().setVersion("WLC").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(4).setText("[Scripture Placeholder]"),
            new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
            new Scripture().setVersion("WLC").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]")).inOrder();
  }

  @Test
  public void getScriptures_interlaced_geez() {
    assertThat(
        scriptureStore.getScriptures(VERSION, Language.GEEZ, "2 Kings 23:5", View.Interlaced).getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion("GzExp").setLanguage(Language.GEEZ).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"));
    assertThat(
        scriptureStore.getScriptures(VERSION, Language.GEEZ, "2 Kings 23:3-5", View.Interlaced).getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(3).setText("[Scripture Placeholder]"),
        new Scripture().setVersion("GzExp").setLanguage(Language.GEEZ).setBook("2 Kings").setChapter(23).setVerse(3).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(4).setText("[Scripture Placeholder]"),
        new Scripture().setVersion("GzExp").setLanguage(Language.GEEZ).setBook("2 Kings").setChapter(23).setVerse(4).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion("GzExp").setLanguage(Language.GEEZ).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]")).inOrder();
  }

  @Test
  public void getScriptures_interlacedNotSupported() {
    assertThat(scriptureStore.getScriptures(VERSION, EN, "Jasher 1:1", View.Interlaced).getItems())
        .containsExactly(
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(1).setVerse(1).setText("[Fetched OXFORD From Web]"));
    assertThat(scriptureStore.getScriptures("OXFORD", EN, "Jasher 5:17-19", View.Interlaced).getItems())
        .containsExactly(
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(17).setText("[Fetched OXFORD From Web]"),
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(18).setText("[Fetched OXFORD From Web]"),
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(19).setText("[Fetched OXFORD From Web]"));
  }

  @Test
  public void getScriptures_interlaced_legacy() {
    assertThat(
        scriptureStore.getScriptures(VERSION, Language.INTERLACED, "2 Kings 23:5").getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion("WLC").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"));
    assertThat(
        scriptureStore.getScriptures(VERSION, Language.INTERLACED, "2 Kings 23:3-5").getItems()).containsExactly(
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(3).setText("[Scripture Placeholder]"),
        new Scripture().setVersion("WLC").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(3).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(4).setText("[Scripture Placeholder]"),
        new Scripture().setVersion("WLC").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(4).setText("[Scripture Placeholder]"),
        new Scripture().setVersion(VERSION).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]"),
        new Scripture().setVersion("WLC").setLanguage(Language.HEBREW).setBook("2 Kings").setChapter(23).setVerse(5).setText("[Scripture Placeholder]")).inOrder();
  }

  @Test
  public void getScriptures_interlacedNotSupported_legacy() {
    assertThat(scriptureStore.getScriptures(VERSION, Language.INTERLACED, "Jasher 1:1").getItems())
        .containsExactly(
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(1).setVerse(1).setText("[Fetched OXFORD From Web]"));
    assertThat(scriptureStore.getScriptures("OXFORD", Language.INTERLACED, "Jasher 5:17-19").getItems())
        .containsExactly(
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(17).setText("[Fetched OXFORD From Web]"),
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(18).setText("[Fetched OXFORD From Web]"),
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(19).setText("[Fetched OXFORD From Web]"));
  }

  @Test
  public void getScriptures_interlinear() {
    ImmutableList<Interlinear> interlinears = ImmutableList.of(
        new Interlinear().setBook("Gen").setChapter(1).setVerse(1).setStrongsId("H7225").setWord("בְּרֵאשִׁ֖ית"),
        new Interlinear().setBook("Gen").setChapter(1).setVerse(1).setStrongsId("H1254").setWord("בָּרָ֣א"),
        new Interlinear().setBook("Gen").setChapter(1).setVerse(1).setStrongsId("H0430").setWord("אֱלֹהִ֑ים"));
    when(interlinearStore.getInterlinear(any(VerseRange.class))).thenReturn(interlinears);
    assertThat(scriptureStore.getScriptures(VERSION, "en", "Gen 1:1", View.Interlinear).getItems())
        .containsExactly(new InterlinearScripture(interlinears));
  }

  @Test
  public void getScriptures_interlinearNotSupported() {
    assertThat(scriptureStore.getScriptures(VERSION, "en", "Jasher 1:1", View.Interlinear).getItems())
        .containsExactly(
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(1).setVerse(1).setText("[Fetched OXFORD From Web]"));
    assertThat(scriptureStore.getScriptures(VERSION, "en", "Jasher 5:17-19", View.Interlinear).getItems())
        .containsExactly(
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(17).setText("[Fetched OXFORD From Web]"),
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(18).setText("[Fetched OXFORD From Web]"),
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(19).setText("[Fetched OXFORD From Web]"));
  }

  @Test
  public void getScriptures_interlinear_derived() {
    ImmutableList<Interlinear> interlinears = ImmutableList.of(
        createInterlinear("Jub", 6, 45, 1, "וכל", null),
        createInterlinear("Jub", 6, 45, 2, "הימים", null),
        createInterlinear("Jub", 6, 45, 3, "אשר", null),
        createInterlinear("Jub", 6, 45, 4, "נועדו", null),
        createInterlinear("Jub", 6, 45, 5, "הם", null),
        createInterlinear("Jub", 6, 45, 6, "שתים", null),
        createInterlinear("Jub", 6, 45, 7, "וחמישים", null),
        createInterlinear("Jub", 6, 45, 8, "שבתות", null),
        createInterlinear("Jub", 6, 45, 9, "ימים", null),
        createInterlinear("Jub", 6, 45, 10, "עד", null),
        createInterlinear("Jub", 6, 45, 11, "מלאת", null),
        createInterlinear("Jub", 6, 45, 12, "שנה", null),
        createInterlinear("Jub", 6, 45, 13, "תמימה", null));

    when(dao.list(eq(Scripture.class), any(Query.List.class))).thenReturn(
        QueryResult.of(Scripture.class,
            ImmutableList.of(
                new Scripture().setBook("Jub").setChapter(6).setVerse(45)
                    .setText("וכל הימים אשר נועדו הם שתים וחמישים שבתות ימים עד מלאת שנה תמימה:")),
            1, null));

    assertThat(scriptureStore.getScriptures(VERSION, "en", "Jub 6:45", View.Interlinear).getItems())
        .containsExactly(new InterlinearScripture(interlinears));
  }

  @Test
  public void getScriptures_interlinear_gez() {
    ImmutableList<Interlinear> interlinears = ImmutableList.of(
        createInterlinear("Gen", 2, 3, 1, "ኣምላኽ", null),
        createInterlinear("Gen", 2, 3, 2, "ከኣ", null),
        createInterlinear("Gen", 2, 3, 3, "ኻብቲ", null),
        createInterlinear("Gen", 2, 3, 4, "ዝፈጠሮን", null),
        createInterlinear("Gen", 2, 3, 5, "ዝገበሮን", null),
        createInterlinear("Gen", 2, 3, 6, "ኵሉ", null),
        createInterlinear("Gen", 2, 3, 7, "ግብሩ", null),
        createInterlinear("Gen", 2, 3, 8, "ብእኣ", null),
        createInterlinear("Gen", 2, 3, 9, "ስለ", null),
        createInterlinear("Gen", 2, 3, 10, "ዝዐረፈ", null),
        createInterlinear("Gen", 2, 3, 11, "ነታ", null),
        createInterlinear("Gen", 2, 3, 12, "ሳብዐይቲ", null),
        createInterlinear("Gen", 2, 3, 13, "መዓልቲ", null),
        createInterlinear("Gen", 2, 3, 14, "ባረኻን", null),
        createInterlinear("Gen", 2, 3, 15, "ቀደሳን", null));

    when(dao.list(eq(Scripture.class), any(Query.List.class))).thenReturn(
        QueryResult.of(Scripture.class,
            ImmutableList.of(
                new Scripture().setBook("Gen").setChapter(2).setVerse(3).setLanguage(Language.GEEZ)
                    .setText("ኣምላኽ ከኣ ኻብቲ ዝፈጠሮን ዝገበሮን ኵሉ ግብሩ ብእኣ ስለ ዝዐረፈ፡ ነታ ሳብዐይቲ መዓልቲ ባረኻን ቀደሳን።")),
            1, null));

    assertThat(scriptureStore.getScriptures(VERSION, Language.GEEZ, "Gen 2:3", View.Interlinear).getItems())
        .containsExactly(new InterlinearScripture(interlinears).setLanguage(Language.GEEZ));
  }

  @Test
  public void getScriptures_interlinear_legacy() {
    ImmutableList<Interlinear> interlinears = ImmutableList.of(
        new Interlinear().setBook("Gen").setChapter(1).setVerse(1).setStrongsId("H7225").setWord("בְּרֵאשִׁ֖ית"),
        new Interlinear().setBook("Gen").setChapter(1).setVerse(1).setStrongsId("H1254").setWord("בָּרָ֣א"),
        new Interlinear().setBook("Gen").setChapter(1).setVerse(1).setStrongsId("H0430").setWord("אֱלֹהִ֑ים"));
    when(interlinearStore.getInterlinear(any(VerseRange.class))).thenReturn(interlinears);
    assertThat(scriptureStore.getScriptures(VERSION, INTERLINEAR, "Gen 1:1").getItems())
        .containsExactly(new InterlinearScripture(interlinears));
  }

  @Test
  public void getScriptures_interlinearNotSupported_legacy() {
    assertThat(scriptureStore.getScriptures(VERSION, INTERLINEAR, "Jasher 1:1").getItems())
        .containsExactly(
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(1).setVerse(1).setText("[Fetched OXFORD From Web]"));
    assertThat(scriptureStore.getScriptures(INTERLINEAR, "en", "Jasher 5:17-19").getItems())
        .containsExactly(
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(17).setText("[Fetched OXFORD From Web]"),
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(18).setText("[Fetched OXFORD From Web]"),
            new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(5).setVerse(19).setText("[Fetched OXFORD From Web]"));
  }

  @Test
  public void getScriptures_interlinear_derived_legacy() {
    ImmutableList<Interlinear> interlinears = ImmutableList.of(
        createInterlinear("Jub", 6, 45, 1, "וכל", null),
        createInterlinear("Jub", 6, 45, 2, "הימים", null),
        createInterlinear("Jub", 6, 45, 3, "אשר", null),
        createInterlinear("Jub", 6, 45, 4, "נועדו", null),
        createInterlinear("Jub", 6, 45, 5, "הם", null),
        createInterlinear("Jub", 6, 45, 6, "שתים", null),
        createInterlinear("Jub", 6, 45, 7, "וחמישים", null),
        createInterlinear("Jub", 6, 45, 8, "שבתות", null),
        createInterlinear("Jub", 6, 45, 9, "ימים", null),
        createInterlinear("Jub", 6, 45, 10, "עד", null),
        createInterlinear("Jub", 6, 45, 11, "מלאת", null),
        createInterlinear("Jub", 6, 45, 12, "שנה", null),
        createInterlinear("Jub", 6, 45, 13, "תמימה", null));

    when(dao.list(eq(Scripture.class), any(Query.List.class))).thenReturn(
        QueryResult.of(Scripture.class,
            ImmutableList.of(
                new Scripture().setBook("Jub").setChapter(6).setVerse(45)
                    .setText("וכל הימים אשר נועדו הם שתים וחמישים שבתות ימים עד מלאת שנה תמימה:")),
            1, null));

    assertThat(scriptureStore.getScriptures(VERSION, INTERLINEAR, "Jub 6:45").getItems())
        .containsExactly(new InterlinearScripture(interlinears));
  }

  @Test
  public void getScriptures_comparison() {
    assertThat(scriptureStore.getScriptures(VERSION, Language.INTERLACED, "Isa 56:6").getItems())
        .containsExactly(
            new Scripture().setVersion(VERSION).setBook("Isaiah").setChapter(56).setVerse(6)
                .setText("[Fetched " + VERSION + " From Web]"),
            new Scripture().setVersion("WLC").setBook("Isaiah").setChapter(56).setVerse(6).setLanguage(Language.HEBREW_HASER)
                .setText("Fetched WLC From Web"),
            new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(56).setVerse(6).setLanguage(Language.HEBREW)
                .setText("[Fetched DSS From Web]"),
            new AuditScripture("Isaiah", 56, 6,
                "Fetched <span class=\"diff-delete\">DSS</span><span class=\"diff-insert\">WLC</span> From Web", 3, 0.85),
            new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(56).setVerse(6).setLanguage(Language.EN)
                .setText("[Fetched DSS From Web]")).inOrder();
    assertThat(scriptureStore.getScriptures(VERSION, Language.INTERLACED, "Isa 24:3-5").getItems())
        .containsExactlyElementsIn(
            IntStream.range(3, 6)
                .boxed()
                .flatMap(
                    verse -> Stream.of(
                        new Scripture().setVersion(VERSION).setBook("Isaiah").setChapter(24).setVerse(verse).setText("[Fetched " + VERSION + " From Web]"),
                        new Scripture().setVersion("WLC").setBook("Isaiah").setChapter(24).setVerse(verse).setLanguage(Language.HEBREW_HASER)
                            .setText("Fetched WLC From Web"),
                        new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(24).setVerse(verse).setLanguage(Language.HEBREW)
                            .setText("[Fetched DSS From Web]"),
                        new AuditScripture("Isaiah", 24, verse,
                            "Fetched <span class=\"diff-delete\">DSS</span><span class=\"diff-insert\">WLC</span> From Web", 3, 0.85),
                        new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(24).setVerse(verse).setLanguage(Language.EN)
                            .setText("[Fetched DSS From Web]")))
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

  public static Interlinear createInterlinear(
      String book, int chapter, int verse, int index, String word, String strongsId) {
    return new Interlinear().setBook(book).setChapter(chapter).setVerse(verse)
        .setIndex(index).setWord(word).setStrongsId(strongsId)
        .setConstantsOnly(HebrewConverter.toConstantsOnly(word));
  }
}

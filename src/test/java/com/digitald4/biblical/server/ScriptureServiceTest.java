package com.digitald4.biblical.server;

import static com.digitald4.biblical.util.Language.EN;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.server.ScriptureService.StringList;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.tools.TranslationTool;
import com.digitald4.biblical.util.HebrewTokenizer;
import com.digitald4.biblical.util.MachineTranslator;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.View;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.storage.ChangeTracker;
import com.digitald4.common.storage.DAOFileDBImpl;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SessionStore;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

public class ScriptureServiceTest {
  private static final ChangeTracker changeTracker = new ChangeTracker(null, null, null, null);
  private static final DAOFileDBImpl daoFileDB = new DAOFileDBImpl(changeTracker);
  private static final BibleBookStore bibleBookStore = new BibleBookStore(() -> daoFileDB);
  private static final ScriptureReferenceProcessor scriptureRefProcessor =
      new ScriptureReferenceProcessorSplitImpl(bibleBookStore);
  @Mock private final SessionStore<BasicUser> sessionStore = mock(SessionStore.class);
  @Mock private final ScriptureStore scriptureStore = mock(ScriptureStore.class);
  private static final TokenWordStore tokenWordStore =
      new TokenWordStore(TranslationTool::tokenWordProvider, TranslationTool::lexiconProvider);
  private static final MachineTranslator machineTranslator =
      new MachineTranslator(tokenWordStore, new HebrewTokenizer(tokenWordStore));
  private ScriptureService scriptureService;

  @Before
  public void setup() {
    scriptureService = new ScriptureService(scriptureStore, sessionStore, scriptureRefProcessor, machineTranslator);
  }

  @Test
  public void getOrSearch_scriptureSingle() throws Exception {
    when(scriptureStore.getScriptures("ISR", EN, "Genesis 2:3")).thenReturn(
        ScriptureStore.GetOrSearchResponse.getResult(
            ImmutableList.of(new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2)
                .setVerse(3).setText("Elohim blessed the seventh day.")),
            null, null));

    QueryResult<Scripture> result = scriptureService.fetch(
        "Genesis 2:3", "ISR", EN, null, 200, 1, null);

    assertThat(result.getTotalSize()).isEqualTo(1);
  }

  @Test
  public void getOrSearch_scriptureRange() throws Exception {
    when(scriptureStore.getScriptures("ISR", EN, "Genesis 2:2-3", View.Text)).thenReturn(
        ScriptureStore.GetOrSearchResponse.getResult(
            ImmutableList.of(
                new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(2)
                    .setText("And on the 7th day he rested"),
                new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(3)
                    .setText("Elohim blessed the seventh day.")),
            null, null));

    QueryResult<Scripture> result = scriptureService.fetch("Genesis 2:2-3", "ISR", EN, View.Text, 200, 1, null);

    assertThat(result.getTotalSize()).isEqualTo(2);
  }

  @Test
  public void getOrSearch_scriptureMultiBooks() throws Exception {
    when(scriptureStore.getScriptures("ISR", EN, "Genesis 2:3, Exodus 20:8", View.Text)).thenReturn(
        ScriptureStore.GetOrSearchResponse.getResult(
            ImmutableList.of(
                new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(3)
                    .setText("Elohim blessed the seventh day."),
                new Scripture().setVersion("ISR").setBook("Exodus").setChapter(20).setVerse(8)
                    .setText("Remember the sabbath day, keep it holy.")),
            null, null));

    QueryResult<Scripture> result = scriptureService.fetch("Genesis 2:3, Exodus 20:8", "ISR", EN, View.Text, 200, 1, null);

    assertThat(result.getTotalSize()).isEqualTo(2);
  }

  @Test
  public void getOrSearch_search() throws Exception {
    when(scriptureStore.search(any())).thenAnswer(i ->
        QueryResult.of(Scripture.class,
            ImmutableList.of(
                new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(3)
                    .setText("Elohim blessed the seventh day."),
                new Scripture().setVersion("RSKJ").setBook("Genesis").setChapter(2).setVerse(3)
                    .setText("Elohim blessed the seventh day and made it holy.")),
            2, i.getArgument(0)));

    QueryResult<Scripture> result = scriptureService.fetch(
        "Elohim blessed seventh", null, EN, View.Text, 200, 1, ScriptureStore.DEFAULT_ORDER_BY);

    assertThat(result.getOrderBy()).isEqualTo(ScriptureStore.DEFAULT_ORDER_BY);
    assertThat(((Query.Search) result.query()).getSearchText()).isEqualTo("Elohim blessed seventh");
  }

  @Test @Ignore
  public void getOrSearch_searchWithVersion() throws Exception {
    when(scriptureStore.search(any())).thenAnswer(i ->
        QueryResult.of(Scripture.class,
            ImmutableList.of(
                new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(3)
                    .setText("Elohim blessed the seventh day.")),
            1, i.getArgument(0)));

    QueryResult<Scripture> result = scriptureService.fetch("Elohim blessed seventh", "ISR", EN, View.Text, 200, 1, null);

    assertThat(result.getOrderBy()).isEqualTo(ScriptureStore.DEFAULT_ORDER_BY);
    assertThat(((Query.Search) result.query()).getSearchText()).isEqualTo(
        "Elohim blessed seventh version=ISR");
  }

  @Test
  public void translate() throws Exception {
    ImmutableList<Interlinear> translation = scriptureService.translate("זכור את המצוה אשר נתן יה לך");
    assertThat(translation.stream().flatMap(i -> i.getSubTokens().stream()).collect(toImmutableList())).containsExactly(
        new SubToken().setWord("זכור").setTranslation("remember").setStrongsId("H2142").setTransliteration("zacur"),
        new SubToken().setWord("את").setTranslation("you").setStrongsId("H0853").setTransliteration("at"),
        new SubToken().setWord("ה").setTranslation("the ").setTransliteration("ha"),
        new SubToken().setWord("מצוה").setTranslation("commandment").setStrongsId("H4687").setTransliteration("matzuh"),
        new SubToken().setWord("אשר").setTranslation("which").setStrongsId("H0834").setTransliteration("ashar"),
        new SubToken().setWord("נתנ").setTranslation("given").setStrongsId("H5414").setTransliteration("natan"),
        new SubToken().setWord("יה").setTranslation("Yah").setStrongsId("H3050").setTransliteration("yah"),
        new SubToken().setWord("לכ").setTranslation("for yourself").setStrongsId("H0853").setTransliteration("lac"));
  }

  @Test
  public void bulkTranslate() throws Exception {
    ImmutableList<InterlinearScripture> translation = scriptureService.bulkTranslate(
        new StringList().setItems(ImmutableList.of("זכור את המצוה אשר נתן יה לך", "", "תֹּורָ֥ה צִוָּה־לָ֖נוּ מֹשֶׁ֑ה מֹורָשָׁ֖ה קְהִלַּ֥ת יַעֲקֹֽב׃")));
    assertThat(translation.get(0).getInterlinears().stream().flatMap(i -> i.getSubTokens().stream()).collect(toImmutableList())).containsExactly(
        new SubToken().setWord("זכור").setTranslation("remember").setStrongsId("H2142").setTransliteration("zacur"),
        new SubToken().setWord("את").setTranslation("you").setStrongsId("H0853").setTransliteration("at"),
        new SubToken().setWord("ה").setTranslation("the ").setTransliteration("ha"),
        new SubToken().setWord("מצוה").setTranslation("commandment").setStrongsId("H4687").setTransliteration("matzuh"),
        new SubToken().setWord("אשר").setTranslation("which").setStrongsId("H0834").setTransliteration("ashar"),
        new SubToken().setWord("נתנ").setTranslation("given").setStrongsId("H5414").setTransliteration("natan"),
        new SubToken().setWord("יה").setTranslation("Yah").setStrongsId("H3050").setTransliteration("yah"),
        new SubToken().setWord("לכ").setTranslation("for yourself").setStrongsId("H0853").setTransliteration("lac"));
    assertThat(translation.get(1).getInterlinears().stream().flatMap(i -> i.getSubTokens().stream()).collect(toImmutableList()))
        .containsExactly(new SubToken().setWord("").setTranslation("[UNK]").setStrongsId(null).setTransliteration(""));
    assertThat(translation.get(2).getInterlinears().stream().flatMap(i -> i.getSubTokens().stream()).collect(toImmutableList())).containsExactly(
        new SubToken().setWord("תורה").setTranslation("law").setStrongsId("H8451").setTransliteration("turah"),
        new SubToken().setWord("צוה").setTranslation("command").setStrongsId("H6680").setTransliteration("tzuh"),
        new SubToken().setWord("ל").setTranslation("to ").setTransliteration("la"),
        new SubToken().setWord("נו").setTranslation("of us").setTransliteration("nu"),
        new SubToken().setWord("מושה").setTranslation("Moses").setStrongsId("H4872").setTransliteration("mushah"),
        new SubToken().setWord("מורשה").setTranslation("possession").setStrongsId("H4181").setTransliteration("murashah"),
        new SubToken().setWord("קהלת").setStrongsId("H6953").setTranslation("preacher").setTransliteration("qahalat"),
        new SubToken().setWord("יעקוב").setStrongsId("H3290").setTranslation("Jacob").setTransliteration("yiqub"));

    assertThat(translation.get(0).toString()).isNotNull();
    assertThat(translation.get(0).reference()).isEqualTo("null null:null");
    assertThat(translation.get(0).getId()).isEqualTo("Interlinear-he-null-null-null");
    assertThat(translation.get(0).hashCode()).isGreaterThan(0);
    assertThat(translation.get(1).getInterlinears().get(0).ancient()).isNull();
  }
}

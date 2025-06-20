package com.digitald4.biblical.server;

import static com.digitald4.biblical.util.Language.EN;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.ScriptureStore;
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
  private ScriptureService scriptureService;

  @Before
  public void setup() {
    scriptureService = new ScriptureService(scriptureStore, sessionStore, scriptureRefProcessor, null);
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
}

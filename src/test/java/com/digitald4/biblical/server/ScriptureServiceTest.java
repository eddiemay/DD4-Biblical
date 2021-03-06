package com.digitald4.biblical.server;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SessionStore;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

public class ScriptureServiceTest {
  private static final ScriptureReferenceProcessor scriptureReferenceProcessor = new ScriptureReferenceProcessorSplitImpl();
  @Mock private final SessionStore<BasicUser> sessionStore = mock(SessionStore.class);
  @Mock private final ScriptureStore scriptureStore = mock(ScriptureStore.class);
  private ScriptureService scriptureService;

  @Before
  public void setup() {
    scriptureService = new ScriptureService(scriptureStore, sessionStore, scriptureReferenceProcessor);
  }

  @Test
  public void getOrSearch_scriptureSingle() throws Exception {
    when(scriptureStore.getScriptures("ISR", "Genesis 2:3")).thenReturn(
        ImmutableList.of(
            new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(3)
                .setText("Elohim blessed the seventh day.")));

    QueryResult<Scripture> result = scriptureService.search("Genesis 2:3", "ISR", null, 200, 1);

    assertThat(result.getTotalSize()).isEqualTo(1);
  }

  @Test
  public void getOrSearch_scriptureRange() throws Exception {
    when(scriptureStore.getScriptures("ISR", "Genesis 2:2-3")).thenReturn(
        ImmutableList.of(
            new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(2)
                .setText("And on the 7th day he rested"),
            new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(3)
                .setText("Elohim blessed the seventh day.")));

    QueryResult<Scripture> result = scriptureService.search("Genesis 2:2-3", "ISR", null, 200, 1);

    assertThat(result.getTotalSize()).isEqualTo(2);
  }

  @Test
  public void getOrSearch_scriptureMultiBooks() throws Exception {
    when(scriptureStore.getScriptures("ISR", "Genesis 2:3, Exodus 20:8")).thenReturn(
        ImmutableList.of(
            new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(3)
                .setText("Elohim blessed the seventh day."),
            new Scripture().setVersion("ISR").setBook("Exodus").setChapter(20).setVerse(8)
                .setText("Remember the sabbath day, keep it holy.")));

    QueryResult<Scripture> result = scriptureService.search("Genesis 2:3, Exodus 20:8", "ISR", null, 200, 1);

    assertThat(result.getTotalSize()).isEqualTo(2);
  }

  @Test
  public void getOrSearch_search() throws Exception {
    when(scriptureStore.search(any())).thenAnswer(i ->
        QueryResult.of(
            ImmutableList.of(
                new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(3)
                    .setText("Elohim blessed the seventh day."),
                new Scripture().setVersion("RSKJ").setBook("Genesis").setChapter(2).setVerse(3)
                    .setText("Elohim blessed the seventh day and made it holy.")),
            2, i.getArgumentAt(0, Query.class)));

    QueryResult<Scripture> result = scriptureService.search(
        "Elohim blessed seventh", null, ScriptureStore.DEFAULT_ORDER_BY, 200, 1);

    assertThat(result.getOrderBy()).isEqualTo(ScriptureStore.DEFAULT_ORDER_BY);
    assertThat(((Query.Search) result.query()).getSearchText()).isEqualTo("Elohim blessed seventh");
  }

  @Test @Ignore
  public void getOrSearch_searchWithVersion() throws Exception {
    when(scriptureStore.search(any())).thenAnswer(i ->
        QueryResult.of(
            ImmutableList.of(
                new Scripture().setVersion("ISR").setBook("Genesis").setChapter(2).setVerse(3)
                    .setText("Elohim blessed the seventh day.")),
            1, i.getArgumentAt(0, Query.class)));

    QueryResult<Scripture> result = scriptureService.search("Elohim blessed seventh", "ISR", null, 200, 1);

    assertThat(result.getOrderBy()).isEqualTo(ScriptureStore.DEFAULT_ORDER_BY);
    assertThat(((Query.Search) result.query()).getSearchText()).isEqualTo("Elohim blessed seventh version=ISR");
  }
}

package com.digitald4.biblical.store;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.util.HebrewTokenizer;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.QueryResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class TokenWordStoreTest {

  @Mock
  private final DAO dao = mock(DAO.class);

  @Before
  public void setup() {
    when(dao.list(eq(HebrewTokenizer.TokenWord.class), any())).thenReturn(
        QueryResult.of(TokenWord.class, ImmutableList.of(), 0, null));
  }

  @Test
  public void createsCapitalEntryForGreek() {
    TokenWordStore tokenWordStore = new TokenWordStore(
        () -> dao,
        () -> ImmutableList.of(new HebrewTokenizer.TokenWord().setWord("μη").setTranslation("not")),
        ImmutableMap::of);
    assertThat(tokenWordStore.getAll()).containsExactly(
        new HebrewTokenizer.TokenWord().setWord("μη").setTranslation("not"),
        new HebrewTokenizer.TokenWord().setWord("Μη").setTranslation("Not"));
  }
}

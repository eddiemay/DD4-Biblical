package com.digitald4.biblical.store;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BiblicalEvent;
import com.digitald4.common.storage.DAO;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import java.util.function.UnaryOperator;

public class BiblicalEventStoreTest {
  private static final long GEN_1_1_ID = 11;
  private static final String GEN_1_1_FULL =
      "In the beginning <div scripture-reference=\"Genesis 1:1\">Genesis 1:1 In the beginning Elohim created...</div>";
  private static final String GEN_1_1_STORAGE =
      "In the beginning <div scripture-reference=\"Genesis 1:1\"><p>Genesis 1:1</p></div>";
  private static final String GEN_2_3_FULL =
      "<div scripture-reference=\"Genesis 2:3\">Genesis 2:3 And Elohim blessed the 7th day and set it apart...</div>";
  private static final String GEN_2_3_STORAGE = "<div scripture-reference=\"Genesis 2:3\"><p>Genesis 2:3</p></div>";

  private DAO dao = mock(DAO.class);
  private BiblicalEventStore biblicalEventStore;

  @Before
  public void setup() {
    biblicalEventStore = new BiblicalEventStore(() -> dao);
    when(dao.create(any(BiblicalEvent.class))).thenAnswer(i -> i.getArgumentAt(0, BiblicalEvent.class));
    when(dao.create(any(ImmutableList.class))).thenAnswer(i -> i.getArgumentAt(0, ImmutableList.class));
  }

  @Test
  public void create_clearsScriptureReference() {
    BiblicalEvent biblicalEvent = biblicalEventStore.create(new BiblicalEvent().setSummary(GEN_1_1_FULL));

    assertThat(biblicalEvent.getSummary()).isEqualTo(GEN_1_1_STORAGE);
  }

  @Test
  public void multiCreate_clearsScriptureReference() {
    ImmutableList<BiblicalEvent> biblicalEvents = biblicalEventStore.create(
        ImmutableList.of(new BiblicalEvent().setSummary(GEN_1_1_FULL), new BiblicalEvent().setSummary(GEN_2_3_FULL)));

    assertThat(biblicalEvents.get(0).getSummary()).isEqualTo(GEN_1_1_STORAGE);
    assertThat(biblicalEvents.get(1).getSummary()).isEqualTo(GEN_2_3_STORAGE);
  }

  @Test
  public void update_clearsScriptureReference() {
    when(dao.update(eq(BiblicalEvent.class), eq(GEN_1_1_ID), any(UnaryOperator.class)))
        .then(i -> i.getArgumentAt(2, UnaryOperator.class).apply(new BiblicalEvent().setSummary(GEN_1_1_STORAGE)));

    BiblicalEvent biblicalEvent = biblicalEventStore.update(GEN_1_1_ID, current -> current.setSummary(GEN_1_1_FULL));

    assertThat(biblicalEvent.getSummary()).isEqualTo(GEN_1_1_STORAGE);
  }
}

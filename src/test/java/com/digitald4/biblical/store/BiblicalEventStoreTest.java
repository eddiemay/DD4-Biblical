package com.digitald4.biblical.store;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BiblicalEvent;
import com.digitald4.biblical.model.BiblicalEvent.Dependency.Relationship;
import com.digitald4.biblical.model.BiblicalEvent.Duration;
import com.digitald4.biblical.util.ScriptureMarkupProcessor;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.Transaction;
import org.junit.Before;
import org.junit.Test;

public class BiblicalEventStoreTest {
  private static final ScriptureMarkupProcessor SCRIPTURE_MARKUP_PROCESSOR = new ScriptureMarkupProcessor();
  private final DAO dao = mock(DAO.class);
  private BiblicalEventStore biblicalEventStore;

  @Before
  public void setup() {
    biblicalEventStore = new BiblicalEventStore(() -> dao, SCRIPTURE_MARKUP_PROCESSOR, null);
    when(dao.persist(any())).thenAnswer(i -> i.getArgument(0, Transaction.class).prePersist());
    // when(dao.create(any(ImmutableList.class))).thenAnswer(i -> i.getArgumentAt(0, ImmutableList.class));
  }

  @Test
  public void create_setsYears() {
    BiblicalEvent event = biblicalEventStore.create(
        new BiblicalEvent()
            .setTitle("Offset Fixed")
            .setOffset(new BiblicalEvent.Duration().setYears(10))
            .setDuration(new BiblicalEvent.Duration().setYears(2)));

    assertThat(event.getTitle()).isEqualTo("Offset Fixed");
    assertThat(event.getYear()).isEqualTo(10);
    assertThat(event.getEndYear()).isEqualTo(12);
  }

  @Test
  public void setYearsCorrectly() {
    BiblicalEvent pred = new BiblicalEvent().setId(100L).setYear(100).setDuration(new Duration().setYears(50));
    when(dao.get(eq(BiblicalEvent.class), eq(100L))).thenReturn(pred);

    long depEventId = 100L;
    BiblicalEvent event = new BiblicalEvent();

    // No offset no, duration should result in 0,0
    biblicalEventStore.preprocess(event);
    assertThat(event.getYear()).isEqualTo(0);
    assertThat(event.getEndYear()).isEqualTo(0);

    // With offset should result in 0 + offset for both
    biblicalEventStore.preprocess(event.setOffset(new Duration().setYears(5)));
    assertThat(event.getYear()).isEqualTo(5);
    assertThat(event.getEndYear()).isEqualTo(5);

    // Duration should add to start for finish.
    biblicalEventStore.preprocess(event.setDuration(new Duration().setYears(30)));
    assertThat(event.getYear()).isEqualTo(5);
    assertThat(event.getEndYear()).isEqualTo(35);

    // A start to start dependency should move the start to offset passed the dependency start
    biblicalEventStore.preprocess(event.setDepEventId(depEventId).setDepRelationship(Relationship.START_TO_START));
    assertThat(event.getYear()).isEqualTo(105);
    assertThat(event.getEndYear()).isEqualTo(135);

    biblicalEventStore.preprocess(event.setDepRelationship(Relationship.FINISH_TO_START));
    assertThat(event.getYear()).isEqualTo(155);
    assertThat(event.getEndYear()).isEqualTo(185);

    biblicalEventStore.preprocess(event.setDepRelationship(Relationship.FINISH_TO_FINISH));
    assertThat(event.getYear()).isEqualTo(125);
    assertThat(event.getEndYear()).isEqualTo(155);

    biblicalEventStore.preprocess(event.setDepRelationship(Relationship.START_TO_FINISH));
    assertThat(event.getYear()).isEqualTo(75);
    assertThat(event.getEndYear()).isEqualTo(105);
  }
}

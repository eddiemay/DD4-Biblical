package com.digitald4.biblical.store;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Lesson;
import com.digitald4.biblical.model.Lesson.LessonVersion;
import com.digitald4.biblical.util.ScriptureMarkupProcessor;
import com.digitald4.common.storage.ChangeTracker;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

public class LessonStoreTest {
  private static final String TITLE = "Lesson Title";
  private static final String THEME_TEXT = "[Job 1:1]";
  private static final String THEME_TEXT_EXPANDED = "<inline-scripture ref=\"Job 1:1\"/>";
  private static final String PREPROCESSED_CONTENT = "The seventh day was blessed (Gen 2:3)." +
      " It is a rest for man (<scripture ref=\"Hebrew 10:4\"/>)." +
      " Moses told us (as part of the ten commandments) to keep it holy (Exo 20:8)." +
      " Even the Messiah spoke about the Sabbath (Mark 10:12, Matt 4:19-21)." +
      " To not keep it is sin [1 John 2:3,3:4].";
  private static final String POSTPROCESSED_CONTENT = "The seventh day was blessed (<scripture ref=\"Gen 2:3\"/>)." +
      " It is a rest for man (<scripture ref=\"Hebrew 10:4\"/>)." +
      " Moses told us (as part of the ten commandments) to keep it holy (<scripture ref=\"Exo 20:8\"/>)." +
      " Even the Messiah spoke about the Sabbath (<scripture ref=\"Mark 10:12, Matt 4:19-21\"/>)." +
      " To not keep it is sin <inline-scripture ref=\"1 John 2:3,3:4\"/>.";

  private final static ScriptureMarkupProcessor SCRIPTURE_MARKUP_PROCESSOR = new ScriptureMarkupProcessor();
  @Mock private final Clock clock = mock(Clock.class);
  private DAO dao;
  private LessonStore lessonStore;
  private LessonStore.LessonVersionStore versionStore;

  @Before
  public void setup() {
    AtomicLong TIME = new AtomicLong(60000L);
    dao = new DAOTestingImpl(new ChangeTracker(() -> dao, null, null, clock));
    lessonStore = new LessonStore(() -> dao);
    versionStore = new LessonStore.LessonVersionStore(() -> dao, lessonStore, SCRIPTURE_MARKUP_PROCESSOR);
    when(clock.millis()).thenAnswer(i -> TIME.incrementAndGet());
  }

  @Test
  public void testCreate() {
    Lesson lesson = lessonStore.create(new Lesson().setTitle(TITLE));

    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().toEpochMilli()).isEqualTo(60001L);
    assertThat(lesson.getLastModifiedTime().toEpochMilli()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isNull();
    assertThat(lesson.getLatestPublishedVersionId()).isNull();
  }

  @Test
  public void testCreateFromVersion() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getId()).isEqualTo(5002L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().toEpochMilli()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().toEpochMilli()).isEqualTo(60002L);

    Lesson lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().toEpochMilli()).isEqualTo(60001L);
    assertThat(lesson.getLastModifiedTime().toEpochMilli()).isEqualTo(60003L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLatestPublishedVersionId()).isNull();
  }

  @Test
  public void testCreateVersionAfterLesson() {
    Lesson lesson = lessonStore.create(new Lesson().setTitle(TITLE));
    LessonVersion version = versionStore.create(
        new LessonVersion().setLessonId(lesson.getId()).setTitle(TITLE).setContent("words 123"));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getId()).isEqualTo(5002L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().toEpochMilli()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().toEpochMilli()).isEqualTo(60002L);
    assertThat(version.getContent().toString()).isEqualTo("words 123");

    lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().toEpochMilli()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLatestPublishedVersionId()).isNull();
    assertThat(lesson.getLastModifiedTime().toEpochMilli()).isEqualTo(60003L);
  }

  @Test
  public void testCanUpdateDraftVersion() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE));
    version = versionStore.update(version.getLessonId(), v -> v.setContent("words 123"));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getId()).isEqualTo(5002L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().toEpochMilli()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().toEpochMilli()).isEqualTo(60004L);
    assertThat(version.getContent().toString()).isEqualTo("words 123");

    Lesson lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().toEpochMilli()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLatestPublishedVersionId()).isNull();
    assertThat(lesson.getLastModifiedTime().toEpochMilli()).isEqualTo(60005L);
  }

  @Test
  public void testCanPublishVersion() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE));
    version = versionStore.update(
        version.getLessonId(), v -> v.setContent("words 123").setPublished(true));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getId()).isEqualTo(5002L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().toEpochMilli()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().toEpochMilli()).isEqualTo(60004L);
    assertThat(version.getContent().toString()).isEqualTo("words 123");
    assertThat(version.isPublished()).isTrue();

    Lesson lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().toEpochMilli()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLatestPublishedVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLastModifiedTime().toEpochMilli()).isEqualTo(60005L);
  }

  @Test
  public void testUpdatePublishVersionCreatesNew() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE).setPublished(true));
    version = versionStore.update(version.getLessonId(), v -> v.setContent("words 123"));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getId()).isEqualTo(5003L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().toEpochMilli()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().toEpochMilli()).isEqualTo(60004L);
    assertThat(version.getContent().toString()).isEqualTo("words 123");
    assertThat(version.isPublished()).isFalse();

    Lesson lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().toEpochMilli()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5003L);
    assertThat(lesson.getLatestPublishedVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLastModifiedTime().toEpochMilli()).isEqualTo(60005L);
  }

  @Test
  public void titleChangeOnPublish() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE));
    version = versionStore.update(version.getLessonId(), v -> v.setTitle("New Title"));

    assertThat(version.getTitle()).isEqualTo("New Title");
    assertThat(version.getId()).isEqualTo(5002L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().toEpochMilli()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().toEpochMilli()).isEqualTo(60004L);
    assertThat(version.isPublished()).isFalse();

    Lesson lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().toEpochMilli()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLatestPublishedVersionId()).isNull();
    assertThat(lesson.getLastModifiedTime().toEpochMilli()).isEqualTo(60005L);

    versionStore.update(version.getLessonId(), v -> v.setPublished(true));

    lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo("New Title");
  }

  @Test
  public void create_replacesScriptures() {
    LessonVersion version = versionStore.create(
        new LessonVersion().setTitle(TITLE).setThemeText(THEME_TEXT).setContent(PREPROCESSED_CONTENT));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getThemeText()).isEqualTo(THEME_TEXT_EXPANDED);
    assertThat(version.getContent().toString()).isEqualTo(POSTPROCESSED_CONTENT);
  }

  @Test
  public void update_replacesScriptures() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE).setContent(""));
    version = versionStore.update(version.getLessonId(), current -> current.setContent(PREPROCESSED_CONTENT));

    assertThat(version.getContent().toString()).isEqualTo(POSTPROCESSED_CONTENT);
  }
}

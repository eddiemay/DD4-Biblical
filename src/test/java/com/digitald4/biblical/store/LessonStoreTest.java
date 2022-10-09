package com.digitald4.biblical.store;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Lesson;
import com.digitald4.biblical.model.Lesson.LessonVersion;
import com.digitald4.common.storage.testing.DAOTestingImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

public class LessonStoreTest {
  private static final String TITLE = "Lesson Title";
  @Mock private final Clock clock = mock(Clock.class);
  private DAOTestingImpl dao;
  private LessonStore lessonStore;
  private LessonStore.LessonVersionStore versionStore;

  @Before
  public void setup() {
    AtomicLong TIME = new AtomicLong(60000L);
    dao = new DAOTestingImpl(clock);
    lessonStore = new LessonStore(() -> dao);
    versionStore = new LessonStore.LessonVersionStore(() -> dao, lessonStore);
    when(clock.millis()).thenAnswer(i -> TIME.incrementAndGet());
  }

  @Test
  public void testCreate() {
    Lesson lesson = lessonStore.create(new Lesson().setTitle(TITLE));

    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().getMillis()).isEqualTo(60001L);
    assertThat(lesson.getLastModifiedTime().getMillis()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(0L);
    assertThat(lesson.getLatestPublishedVersionId()).isEqualTo(0L);
  }

  @Test
  public void testCreateFromVersion() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getId()).isEqualTo(5002L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().getMillis()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().getMillis()).isEqualTo(60002L);

    Lesson lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().getMillis()).isEqualTo(60001L);
    assertThat(lesson.getLastModifiedTime().getMillis()).isEqualTo(60003L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLatestPublishedVersionId()).isEqualTo(0L);
  }

  @Test
  public void testCreateVersionAfterLesson() {
    Lesson lesson = lessonStore.create(new Lesson().setTitle(TITLE));
    LessonVersion version =
        versionStore.create(new LessonVersion().setLessonId(lesson.getId()).setTitle(TITLE).setYoutubeId("youtube123"));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getId()).isEqualTo(5002L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().getMillis()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().getMillis()).isEqualTo(60002L);
    assertThat(version.getYoutubeId()).isEqualTo("youtube123");

    lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().getMillis()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLatestPublishedVersionId()).isEqualTo(0L);
    assertThat(lesson.getLastModifiedTime().getMillis()).isEqualTo(60003L);
  }

  @Test
  public void testCanUpdateDraftVersion() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE));
    version = versionStore.update(version.getLessonId(), v -> v.setYoutubeId("youtube123"));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getId()).isEqualTo(5002L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().getMillis()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().getMillis()).isEqualTo(60004L);
    assertThat(version.getYoutubeId()).isEqualTo("youtube123");

    Lesson lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().getMillis()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLatestPublishedVersionId()).isEqualTo(0L);
    assertThat(lesson.getLastModifiedTime().getMillis()).isEqualTo(60005L);
  }

  @Test
  public void testCanPublishVersion() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE));
    version = versionStore.update(version.getLessonId(), v -> v.setYoutubeId("youtube123").setPublished(true));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getId()).isEqualTo(5002L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().getMillis()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().getMillis()).isEqualTo(60004L);
    assertThat(version.getYoutubeId()).isEqualTo("youtube123");
    assertThat(version.isPublished()).isTrue();

    Lesson lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().getMillis()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLatestPublishedVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLastModifiedTime().getMillis()).isEqualTo(60005L);
  }

  @Test
  public void testUpdatePublishVersionCreatesNew() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE).setPublished(true));
    version = versionStore.update(version.getLessonId(), v -> v.setYoutubeId("youtube123"));

    assertThat(version.getTitle()).isEqualTo(TITLE);
    assertThat(version.getId()).isEqualTo(5003L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().getMillis()).isEqualTo(60004L);
    assertThat(version.getLastModifiedTime().getMillis()).isEqualTo(60004L);
    assertThat(version.getYoutubeId()).isEqualTo("youtube123");
    assertThat(version.isPublished()).isFalse();

    Lesson lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().getMillis()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5003L);
    assertThat(lesson.getLatestPublishedVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLastModifiedTime().getMillis()).isEqualTo(60005L);
  }

  @Test
  public void titleChangeOnPublish() {
    LessonVersion version = versionStore.create(new LessonVersion().setTitle(TITLE));
    version = versionStore.update(version.getLessonId(), v -> v.setTitle("New Title"));

    assertThat(version.getTitle()).isEqualTo("New Title");
    assertThat(version.getId()).isEqualTo(5002L);
    assertThat(version.getLessonId()).isEqualTo(5001L);
    assertThat(version.getCreationTime().getMillis()).isEqualTo(60002L);
    assertThat(version.getLastModifiedTime().getMillis()).isEqualTo(60004L);
    assertThat(version.isPublished()).isFalse();

    Lesson lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo(TITLE);
    assertThat(lesson.getId()).isEqualTo(5001L);
    assertThat(lesson.getCreationTime().getMillis()).isEqualTo(60001L);
    assertThat(lesson.getLatestVersionId()).isEqualTo(5002L);
    assertThat(lesson.getLatestPublishedVersionId()).isEqualTo(0L);
    assertThat(lesson.getLastModifiedTime().getMillis()).isEqualTo(60005L);

    versionStore.update(version.getLessonId(), v -> v.setPublished(true));

    lesson = lessonStore.get(version.getLessonId());
    assertThat(lesson.getTitle()).isEqualTo("New Title");
  }
}

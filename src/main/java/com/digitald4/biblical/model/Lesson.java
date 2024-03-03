package com.digitald4.biblical.model;

import com.digitald4.common.model.HasModificationTimes;
import java.time.Instant;

public class Lesson implements HasModificationTimes {
  private long id;
  private String title;
  private Long latestVersionId;
  private Long latestPublishedVersionId;
  private Instant creationTime;
  private Instant lastModifiedTime;
  private Instant deletionTime;

  public static Lesson create(LessonVersion lessonVersion) {
    return new Lesson().setId(lessonVersion.getLessonId()).setTitle(lessonVersion.getTitle());
  }

  public long getId() {
    return id;
  }

  public Lesson setId(long id) {
    this.id = id;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public Lesson setTitle(String title) {
    this.title = title;
    return this;
  }

  public Long getLatestVersionId() {
    return latestVersionId;
  }

  public Lesson setLatestVersionId(Long latestDraftVersionId) {
    this.latestVersionId = latestDraftVersionId;
    return this;
  }

  public Long getLatestPublishedVersionId() {
    return latestPublishedVersionId;
  }

  public Lesson setLatestPublishedVersionId(Long latestPublishedVersionId) {
    this.latestPublishedVersionId = latestPublishedVersionId == 0 ? null : latestPublishedVersionId;
    return this;
  }

  @Override
  public Instant getCreationTime() {
    return creationTime;
  }

  @Override
  public Lesson setCreationTime(long millis) {
    this.creationTime = Instant.ofEpochMilli(millis);
    return this;
  }

  @Override
  public Instant getLastModifiedTime() {
    return lastModifiedTime;
  }

  @Override
  public Lesson setLastModifiedTime(long millis) {
    this.lastModifiedTime = Instant.ofEpochMilli(millis);
    return this;
  }

  @Override
  public Instant getDeletionTime() {
    return deletionTime;
  }

  @Override
  public Lesson setDeletionTime(long millis) {
    this.deletionTime = Instant.ofEpochMilli(millis);
    return this;
  }

  public static class LessonVersion implements HasModificationTimes {
    private long id;
    private long lessonId;
    private String title;
    private String themeText;
    private StringBuilder content;
    private boolean published;
    private Instant creationTime;
    private Instant lastModifiedTime;
    private Instant deletionTime;

    public long getId() {
      return id;
    }

    public LessonVersion setId(long id) {
      this.id = id;
      return this;
    }

    public long getLessonId() {
      return lessonId;
    }

    public LessonVersion setLessonId(long lessonId) {
      this.lessonId = lessonId;
      return this;
    }

    public String getTitle() {
      return title;
    }

    public LessonVersion setTitle(String title) {
      this.title = title;
      return this;
    }

    public String getThemeText() {
      return themeText;
    }

    public LessonVersion setThemeText(String themeText) {
      this.themeText = themeText;
      return this;
    }

    public StringBuilder getContent() {
      return content;
    }

    public LessonVersion setContent(StringBuilder content) {
      this.content = content;
      return this;
    }

    public LessonVersion setContent(String text) {
      return setContent(new StringBuilder(text));
    }

    public boolean isPublished() {
      return published;
    }

    public LessonVersion setPublished(boolean published) {
      this.published = published;
      return this;
    }

    @Override
    public Instant getCreationTime() {
      return creationTime;
    }

    @Override
    public LessonVersion setCreationTime(long millis) {
      this.creationTime = Instant.ofEpochMilli(millis);
      return this;
    }

    @Override
    public Instant getLastModifiedTime() {
      return lastModifiedTime;
    }

    @Override
    public LessonVersion setLastModifiedTime(long millis) {
      this.lastModifiedTime = Instant.ofEpochMilli(millis);
      return this;
    }

    @Override
    public Instant getDeletionTime() {
      return deletionTime;
    }

    @Override
    public LessonVersion setDeletionTime(long millis) {
      this.deletionTime = Instant.ofEpochMilli(millis);
      return this;
    }
  }
}

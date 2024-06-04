package com.digitald4.biblical.model;

import com.digitald4.common.model.ModelObjectModTime;

public class Lesson extends ModelObjectModTime<Long> {
  private String title;
  private Long latestVersionId;
  private Long latestPublishedVersionId;

  public static Lesson create(LessonVersion lessonVersion) {
    return new Lesson().setId(lessonVersion.getLessonId()).setTitle(lessonVersion.getTitle());
  }

  public Lesson setId(Long id) {
    super.setId(id);
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

  public static class LessonVersion extends ModelObjectModTime<Long> {
    private long lessonId;
    private String title;
    private String themeText;
    private StringBuilder content;
    private boolean published;

    public LessonVersion setId(Long id) {
      super.setId(id);
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
  }
}

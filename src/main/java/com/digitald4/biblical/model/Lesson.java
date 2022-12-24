package com.digitald4.biblical.model;

import com.digitald4.common.model.HasModificationTimes;
import org.joda.time.DateTime;

public class Lesson implements HasModificationTimes {
  private long id;
  private String title;
  private long latestVersionId;
  private long latestPublishedVersionId;
  private DateTime creationTime;
  private DateTime lastModifiedTime;
  private DateTime deletionTime;

  public static Lesson create(LessonVersion lessonVersion) {
    return new Lesson()
        .setId(lessonVersion.getLessonId())
        .setTitle(lessonVersion.getTitle());
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

  public long getLatestVersionId() {
    return latestVersionId;
  }

  public Lesson setLatestVersionId(long latestDraftVersionId) {
    this.latestVersionId = latestDraftVersionId;
    return this;
  }

  public long getLatestPublishedVersionId() {
    return latestPublishedVersionId;
  }

  public Lesson setLatestPublishedVersionId(long latestPublishedVersionId) {
    this.latestPublishedVersionId = latestPublishedVersionId;
    return this;
  }

  @Override
  public DateTime getCreationTime() {
    return creationTime;
  }

  @Override
  public Lesson setCreationTime(DateTime createdAt) {
    this.creationTime = createdAt;
    return this;
  }

  @Override
  public DateTime getLastModifiedTime() {
    return lastModifiedTime;
  }

  @Override
  public Lesson setLastModifiedTime(DateTime modifiedAt) {
    this.lastModifiedTime = modifiedAt;
    return this;
  }

  @Override
  public DateTime getDeletionTime() {
    return deletionTime;
  }

  @Override
  public Lesson setDeletionTime(DateTime deletionTime) {
    this.deletionTime = deletionTime;
    return this;
  }

  public static class LessonVersion implements HasModificationTimes {
    private long id;
    private long lessonId;
    private String title = "";
    private String themeText;
    private Theme theme;
    private String youtubeId = "";
    private StringBuilder content;
    private boolean published;
    private DateTime creationTime;
    private DateTime lastModifiedTime;
    private DateTime deletionTime;

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
      if (themeText == null && theme != null) {
        return theme.getScripture() == null
            ? theme.getText() : String.format("<inline-scripture ref=\"%s\" />", theme.getScripture());
      }

      return themeText;
    }

    public LessonVersion setThemeText(String themeText) {
      this.themeText = themeText;
      return this;
    }

    @Deprecated
    public Theme getTheme() {
      return theme;
    }

    @Deprecated
    public LessonVersion setTheme(Theme theme) {
      this.theme = theme;
      return this;
    }

    public String getYoutubeId() {
      return youtubeId;
    }

    public LessonVersion setYoutubeId(String youtubeId) {
      this.youtubeId = youtubeId;
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
    public DateTime getCreationTime() {
      return creationTime;
    }

    @Override
    public LessonVersion setCreationTime(DateTime creationTime) {
      this.creationTime = creationTime;
      return this;
    }

    @Override
    public DateTime getLastModifiedTime() {
      return lastModifiedTime;
    }

    @Override
    public LessonVersion setLastModifiedTime(DateTime lastModifiedTime) {
      this.lastModifiedTime = lastModifiedTime;
      return this;
    }

    @Override
    public DateTime getDeletionTime() {
      return deletionTime;
    }

    @Override
    public LessonVersion setDeletionTime(DateTime deletionTime) {
      this.deletionTime = deletionTime;
      return this;
    }

    public static class Theme {
      private String scripture;
      private String text;

      public String getScripture() {
        return scripture;
      }

      public Theme setScripture(String scripture) {
        this.scripture = scripture;
        if (scripture != null) {
          text = null;
        }
        return this;
      }

      public String getText() {
        return text;
      }

      public Theme setText(String text) {
        this.text = text;
        if (text != null) {
          scripture = null;
        }
        return this;
      }
    }
  }
}

package com.digitald4.biblical.model;

import com.digitald4.common.model.Searchable;

public class Commandment implements Searchable {
  private long id;
  private String scriptures;
  private String summary;
  private String tags;

  public long getId() {
    return id;
  }

  public Commandment setId(long id) {
    this.id = id;
    return this;
  }

  public String getSummary() {
    return summary;
  }

  public Commandment setSummary(String summary) {
    this.summary = summary;
    return this;
  }

  public String getScriptures() {
    return scriptures;
  }

  public Commandment setScriptures(String scriptures) {
    this.scriptures = scriptures;
    return this;
  }

  public String getTags() {
    return tags;
  }

  public Commandment setTags(String tags) {
    this.tags = tags;
    return this;
  }
}

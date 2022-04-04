package com.digitald4.biblical.model;

public class BiblicalEvent {
  private long id;
  private int month;
  private int day;
  private int year = 4000;
  private String title;
  private String summary;
  private String references;

  public long getId() {
    return id;
  }

  public BiblicalEvent setId(long id) {
    this.id = id;
    return this;
  }

  public int getMonth() {
    return month;
  }

  public BiblicalEvent setMonth(int month) {
    this.month = month;
    return this;
  }

  public int getDay() {
    return day;
  }

  public BiblicalEvent setDay(int day) {
    this.day = day;
    return this;
  }

  public int getYear() {
    return year;
  }

  public BiblicalEvent setYear(int year) {
    this.year = year;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public BiblicalEvent setTitle(String title) {
    this.title = title;
    return this;
  }

  public String getSummary() {
    return summary;
  }

  public BiblicalEvent setSummary(String summary) {
    this.summary = summary;
    return this;
  }

  public String getReferences() {
    return references;
  }

  public BiblicalEvent setReferences(String references) {
    this.references = references;
    return this;
  }
}

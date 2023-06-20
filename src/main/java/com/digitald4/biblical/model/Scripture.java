package com.digitald4.biblical.model;

import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.Searchable;

// Dave Nelson, Coonley,
public class Scripture extends ModelObject<String> implements Searchable {
  private String version;
  private String locale = BibleBook.EN;
  private String book;
  private int chapter;
  private int verse;
  private StringBuilder text;

  public String getId() {
    return String.format("%s-%s-%d-%d", getVersion(), getBook(), getChapter(), getVerse())
        .replace(" ", "_");
  }

  public Scripture setId(String id) {
    return this;
  }

  public String getVersion() {
    return version;
  }

  public Scripture setVersion(String version) {
    this.version = version;
    return this;
  }

  public String getLocale() {
    return locale;
  }

  public Scripture setLocale(String locale) {
    this.locale = locale;
    return this;
  }

  public String getBook() {
    return book;
  }

  public Scripture setBook(String book) {
    this.book = book;
    return this;
  }

  public int getChapter() {
    return chapter;
  }

  public Scripture setChapter(int chapter) {
    this.chapter = chapter;
    return this;
  }

  public int getVerse() {
    return verse;
  }

  public Scripture setVerse(int verse) {
    this.verse = verse;
    return this;
  }

  public StringBuilder getText() {
    return text;
  }

  public Scripture setText(StringBuilder text) {
    this.text = text;
    return this;
  }

  public Scripture setText(String text) {
    return setText(new StringBuilder(text));
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Scripture)) {
      return false;
    }
    Scripture other = (Scripture) obj;

    return version.equals(other.version) && locale.equals(other.locale) && book.equals(other.book)
        && chapter == other.chapter && verse == other.verse && text.toString().contentEquals(other.text);
  }

  @Override
  public String toString() {
    return String.format(
        "(%s) %s %d:%d %s", getVersion(), getBook(), getChapter(), getVerse(), getText());
  }
}

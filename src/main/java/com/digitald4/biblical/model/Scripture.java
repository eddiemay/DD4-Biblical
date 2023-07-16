package com.digitald4.biblical.model;

import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.Searchable;
import java.util.Objects;

public class Scripture extends ModelObject<String> implements Searchable {
  private String id;
  private String version;
  private String locale = BibleBook.EN;
  private String book;
  private int chapter;
  private int verse;
  private StringBuilder text;

  public String getId() {
    return id != null ? id
        : String
            .format("%s-%s-%s-%d-%d", getVersion(), getLocale(), getBook(), getChapter(), getVerse())
            .replace(" ", "_");
  }

  public Scripture setId(String id) {
    this.id = id;
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
  public int hashCode() {
    return Objects.hash(version, locale, book, chapter, verse, text.toString());
  }

  @Override
  public String toString() {
    return String.format("(%s:%s) %s %d:%d %s",
        getVersion(), getLocale(), getBook(), getChapter(), getVerse(), getText());
  }
}

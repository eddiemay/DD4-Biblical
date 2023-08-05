package com.digitald4.biblical.model;

import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.Searchable;
import java.util.Objects;

public class Scripture extends ModelObject<String> implements Searchable {
  private String version;
  private String language = BibleBook.EN;
  private String book;
  private int chapter;
  private int verse;
  private StringBuilder text;
  private String location;

  public String getId() {
    return String.format("%s-%s-%s-%d-%d", version, language, book, chapter, verse).replace(" ", "_");
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

  public String getLanguage() {
    return language;
  }

  public Scripture setLanguage(String language) {
    this.language = language;
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

  public String getLocation() {
    return location;
  }

  public Scripture setLocation(String location) {
    this.location = location;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, language, book, chapter, verse, text.toString());
  }

  @Override
  public String toString() {
    return String.format("(%s:%s%s) %s %d:%d %s", version, language,
        location == null ? "" : ":" + location,
        book, chapter, verse, text);
  }
}

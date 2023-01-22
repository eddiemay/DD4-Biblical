package com.digitald4.biblical.model;

// Dave Nelson, Coonley,
public class Scripture {
  private String version;
  private String book;
  private int chapter;
  private int verse;
  private StringBuilder text;

  public String getId() {
    return String.format("%s-%s-%d-%d", getVersion(), getBook().replace(" ", "_"), getChapter(), getVerse());
  }

  public String getVersion() {
    return version;
  }

  public Scripture setVersion(String version) {
    this.version = version;
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

    return version.equals(other.version) && book.equals(other.book) && chapter == other.chapter && verse == other.verse
        && text.toString().equals(other.text.toString());
  }

  @Override
  public String toString() {
    return String.format("(%s) %s %d:%d %s", getVersion(), getBook(), getChapter(), getVerse(), getText());
  }
}

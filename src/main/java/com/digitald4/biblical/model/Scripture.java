package com.digitald4.biblical.model;

import com.digitald4.biblical.model.Lexicon.Interlinear;
import com.digitald4.biblical.util.Language;
import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.Searchable;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.collect.ImmutableList;
import java.util.Objects;

public class Scripture extends ModelObject<String> implements Searchable {
  private String version;
  private String language = Language.EN;
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

  @ApiResourceProperty
  public String reference() {
    return String.format("%s %d:%d", getBook(), getChapter(), getVerse());
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, language, book, chapter, verse, String.valueOf(text));
  }

  @Override
  public String toString() {
    return String.format("(%s:%s%s) %s %d:%d %s", version, language,
        location == null ? "" : ":" + location, book, chapter, verse, text);
  }

  public static class AuditScripture extends Scripture {
    private final int ld;

    public AuditScripture(String book, int chapter, int verse, String text, int ld) {
      this.ld = ld;
      setVersion("Audit");
      setBook(book);
      setChapter(chapter);
      setVerse(verse);
      setText(text);
      setLanguage(Language.HEBREW);
    }

    public int getLd() {
      return ld;
    }

    @Override
    public String toString() {
      return super.toString() + " LD: " + ld;
    }
  }

  public static class InterlinearScripture extends Scripture {
    private final ImmutableList<Interlinear> interlinears;

    public InterlinearScripture(Iterable<Interlinear> interlinears) {
      this.interlinears = ImmutableList.copyOf(interlinears);
      setLanguage(Language.INTERLINEAR);
      Interlinear interlinear = interlinears.iterator().next();
      setVersion(interlinear.getVersion());
      setBook(interlinear.getBook());
      setChapter(interlinear.getChapter());
      setVerse(interlinear.getVerse());
    }

    public ImmutableList<Interlinear> getInterlinears() {
      return interlinears;
    }

    @Override
    public String toString() {
      return super.toString() + " " + interlinears;
    }
  }
}

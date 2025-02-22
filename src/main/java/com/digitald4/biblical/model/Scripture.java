package com.digitald4.biblical.model;

import static com.google.common.collect.Streams.stream;

import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.biblical.util.Language;
import com.digitald4.common.model.ModelObject;
import com.digitald4.common.model.Searchable;
import com.digitald4.common.util.Calculate;
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
    this.book =  book;
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
    return String.format("(%s:%s%s) %s %d:%d %s",
        version, language, location == null ? "" : ":" + location, book, chapter, verse, text);
  }

  public static class AuditScripture extends Scripture {
    private final int ld;
    private final double percentMatch;

    public AuditScripture(String book, int chapter, int verse, String text, int ld, double percentMatch) {
      this.ld = ld;
      this.percentMatch = Double.isNaN(percentMatch) ? 0 : percentMatch;
      setVersion("Audit");
      setBook(book);
      setChapter(chapter);
      setVerse(verse);
      setText(text);
      setLanguage(Language.HEBREW);
    }

    public static AuditScripture of(String book, int chapter, int verse, String original, String revised) {
      original = HebrewConverter.removePunctuation(original);
      revised = HebrewConverter.removePunctuation(revised);

      int ld = Calculate.LD(original, revised);
      double len = Math.max(original.length(), revised.length());
      return new AuditScripture(book, chapter, verse, Calculate.getDiffHtml(original, revised), ld, (len - ld) / len);
    }

    public int getLd() {
      return ld;
    }

    public double getPercentMatch() {
      return percentMatch;
    }

    @Override
    public String toString() {
      return super.toString() + " LD: " + ld + " Percent Match: " + percentMatch;
    }
  }

  public static class InterlinearScripture extends Scripture {
    private final ImmutableList<Interlinear> interlinears;

    public InterlinearScripture(Iterable<Interlinear> interlinears) {
      this.interlinears = ImmutableList.copyOf(interlinears);
      String strongsId =
          stream(interlinears).map(Interlinear::getStrongsId).filter(Objects::nonNull).findAny().orElse("");
      setLanguage(strongsId.startsWith("G") ? Language.GK : Language.HEBREW);
      Interlinear interlinear = interlinears.iterator().next();
      setVersion(ScriptureVersion.INTERLINEAR);
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

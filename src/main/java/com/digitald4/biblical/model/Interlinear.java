package com.digitald4.biblical.model;

import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.common.storage.Annotations.NonIndexed;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class Interlinear {
  // Id values
  private String version = "WLC";
  private String book;
  private int chapter;
  private int verse;
  private int index;

  // Indexed values
  private String strongsId;
  private String word;
  private String constantsOnly;

  // Non indexed values
  private String transliteration;
  private String morphology;
  private String morphologyAbb;
  private int bookNumber; // For sorting by bible book number.
  private String translation;

  // FE only values
  private int matchValue;
  private String dss;
  private String dssDiff;
  private List<SubToken> subTokens;

  public String getId() {
    return String.format("%s-%s-%d-%d-%d",
        getVersion(), getBook().replaceAll(" ", "_"), getChapter(), getVerse(), getIndex());
  }

  public String getVersion() {
    return version;
  }

  public Interlinear setVersion(String version) {
    this.version = version;
    return this;
  }

  public String getBook() {
    return book;
  }

  public Interlinear setBook(String book) {
    this.book = book;
    return this;
  }

  public int getChapter() {
    return chapter;
  }

  public Interlinear setChapter(int chapter) {
    this.chapter = chapter;
    return this;
  }

  public int getVerse() {
    return verse;
  }

  public Interlinear setVerse(int verse) {
    this.verse = verse;
    return this;
  }

  @NonIndexed
  public int getIndex() {
    return index;
  }

  public Interlinear setIndex(int index) {
    this.index = index;
    return this;
  }

  @ApiResourceProperty
  public String reference() {
    return String.format("%s %d:%d", getBook(), getChapter(), getVerse());
  }

  public String getWord() {
    return word;
  }

  public Interlinear setWord(String word) {
    this.word = word;
    return this;
  }

  @NonIndexed
  public String getTransliteration() {
    return transliteration;
  }

  public Interlinear setTransliteration(String transliteration) {
    this.transliteration = transliteration;
    return this;
  }

  public String getStrongsId() {
    return strongsId;
  }

  public Interlinear setStrongsId(String strongsId) {
    this.strongsId = strongsId;
    return this;
  }

  public String getConstantsOnly() {
    return constantsOnly;
  }

  public Interlinear setConstantsOnly(String constantsOnly) {
    this.constantsOnly = constantsOnly;
    return this;
  }

  @ApiResourceProperty
  public String ancient() {
    return HebrewConverter.toAncientRtl(constantsOnly);
  }

  @NonIndexed
  public String getMorphology() {
    return morphologyAbb != null ? morphologyAbb : morphology;
  }

  public Interlinear setMorphology(String morphology) {
    this.morphology = "".equals(morphology) ? null : morphology;
    return this;
  }

  @NonIndexed
  @Deprecated
  public String getMorphologyAbb() {
    return null;
  }

  @Deprecated
  public Interlinear setMorphologyAbb(String morphologyAbb) {
    this.morphologyAbb = "".equals(morphologyAbb) ? null : morphologyAbb;
    return this;
  }

  @NonIndexed
  public int getBookNumber() {
    return bookNumber;
  }

  public Interlinear setBookNumber(int bookNumber) {
    this.bookNumber = bookNumber;
    return this;
  }

  @NonIndexed
  public String getTranslation() {
    return translation;
  }

  public Interlinear setTranslation(String translation) {
    this.translation = translation;
    return this;
  }

  public List<SubToken> getSubTokens() {
    return subTokens;
  }

  public Interlinear setSubtokens(Iterable<SubToken> subTokens) {
    this.subTokens = ImmutableList.copyOf(subTokens);
    return this;
  }

  @Deprecated
  public ImmutableList<Translation> getTranslations() {
    return null;
  }

  @Deprecated
  public Interlinear setTranslations(Iterable<Translation> translations) {
    this.translation = translations.iterator().next().getTranslation();
    return this;
  }

  public int matchValue() {
    return matchValue;
  }

  public Interlinear setMatchValue(int matchValue) {
    this.matchValue = matchValue;
    return this;
  }

  public Interlinear setDss(String dss) {
    this.dss = dss;
    return this;
  }

  public String getDss() {
    return dss;
  }

  public Interlinear setDssDiff(String dssDiff) {
    this.dssDiff = dssDiff;
    return this;
  }

  @ApiResourceProperty
  public String dssDiff() {
    return dssDiff;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Interlinear && toString().equals(obj.toString());
  }

  @Override
  public String toString() {
    return String.format("%s - %s - %s - %s - %s%s - %s", getId(), getStrongsId(),
        getTransliteration(), getWord(), getConstantsOnly(), dss != null ? " - " + dss : "",
        getTranslation());
  }

  @Deprecated
  public static class Translation {
    private String version;
    private String translation;

    public String getVersion() {
      return version;
    }

    public Translation setVersion(String version) {
      this.version = version;
      return this;
    }

    public String getTranslation() {
      return translation;
    }

    public Translation setTranslation(String translation) {
      this.translation = translation;
      return this;
    }

    @Override
    public String toString() {
      return version + "-" + translation;
    }
  }

  public static class SubToken {
    private String word;
    private String translation;
    private String strongsId;

    public String getWord() {
      return word;
    }

    public SubToken setWord(String word) {
      this.word = word;
      return this;
    }

    public String getTranslation() {
      return translation;
    }

    public SubToken setTranslation(String translation) {
      this.translation = translation;
      return this;
    }

    public String getStrongsId() {
      return strongsId;
    }

    public SubToken setStrongsId(String strongsId) {
      this.strongsId = strongsId;
      return this;
    }

    @Override
    public String toString() {
      return String.format("%s-%s-%s", word, translation, strongsId);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof SubToken && toString().equals(obj.toString());
    }
  }
}

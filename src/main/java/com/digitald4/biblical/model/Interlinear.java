package com.digitald4.biblical.model;

import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord.TokenType;
import com.digitald4.common.storage.Annotations.NonIndexed;
import com.digitald4.common.util.FormatText;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.collect.ImmutableList;
// This has to be List or the compiler throws an error for some reason during JSON parsing.
import java.util.List;

public class Interlinear {
  // Id values
  private String version;
  private String book;
  private Integer chapter;
  private Integer verse;
  private Integer index;

  // Indexed values
  private String strongsId;
  private String word;
  private String constantsOnly;

  // Non indexed values
  private String transliteration;
  private String morphology;
  private Integer bookNumber; // For sorting by bible book number.
  private String translation;

  // FE only values
  private int matchValue;
  private String dss;
  private String dssDiff;
  private List<SubToken> subTokens;

  public String getId() {
    return book == null ? null : String.format("%s%s-%d-%d-%d",
        version == null ? "" : version + "-", getBook().replaceAll(" ", "_"), getChapter(), getVerse(), getIndex());
  }

  @Deprecated
  public String getVersion() {
    return  version;
  }

  @Deprecated
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

  public Integer getChapter() {
    return chapter;
  }

  public Interlinear setChapter(Integer chapter) {
    this.chapter = chapter;
    return this;
  }

  public Integer getVerse() {
    return verse;
  }

  public Interlinear setVerse(Integer verse) {
    this.verse = verse;
    return this;
  }

  @NonIndexed
  public Integer getIndex() {
    return index;
  }

  public Interlinear setIndex(Integer index) {
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
    this.constantsOnly = HebrewConverter.toConstantsOnly(constantsOnly);
    return this;
  }

  @ApiResourceProperty
  public String ancient() {
    return HebrewConverter.toAncientRtl(constantsOnly);
  }

  @NonIndexed
  public String getMorphology() {
    return morphology;
  }

  public Interlinear setMorphology(String morphology) {
    this.morphology = "".equals(morphology) ? null : morphology;
    return this;
  }

  @NonIndexed
  public Integer getBookNumber() {
    return bookNumber;
  }

  public Interlinear setBookNumber(Integer bookNumber) {
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

  public Interlinear setSubTokens(Iterable<SubToken> subTokens) {
    this.subTokens = ImmutableList.copyOf(subTokens);
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

  public static class SubToken {
    private String word;
    private String translation;
    private String strongsId;
    private String transliteration = "";
    private TokenType tokenType;

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

    public String getTransliteration() {
      return transliteration;
    }

    public SubToken setTransliteration(String transliteration) {
      this.transliteration = transliteration;
      return this;
    }

    @Override
    public String toString() {
      return String.format("%s-%s-%s-%s", word, translation, strongsId, transliteration);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof SubToken && toString().equals(obj.toString());
    }

    public TokenType getTokenType() {
      return tokenType;
    }

    public SubToken setTokenType(TokenType tokenType) {
      this.tokenType = tokenType;
      return this;
    }

    public boolean isWord() {
      return tokenType == TokenType.WORD || getTokenType() == TokenType.WORD_STRONGS_MATCH_ONLY;
    }
  }
}

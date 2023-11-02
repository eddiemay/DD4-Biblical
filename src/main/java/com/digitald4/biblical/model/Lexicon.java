package com.digitald4.biblical.model;

import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.common.model.ModelObject;
import com.digitald4.common.storage.Annotations.NonIndexed;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.collect.ImmutableList;

public class Lexicon extends ModelObject<String> {
  // Indexed values
  private String word;
  private String constantsOnly;

  // Non Indexed values
  private String transliteration;
  private String pronunciation;
  private String partOfSpeech;
  private String rootWord;
  private String dictionaryAid;
  private ImmutableList<TranslationCount> translationCounts;
  private ImmutableList<Node> outline;
  private StringBuilder strongsDefinition;

  public Lexicon setId(String id) {
    super.setId(id);
    return this;
  }

  public String getWord() {
    return word;
  }

  public Lexicon setWord(String word) {
    this.word = word;
    return this;
  }

  public String getConstantsOnly() {
    return constantsOnly;
  }

  public Lexicon setConstantsOnly(String constantsOnly) {
    this.constantsOnly = constantsOnly;
    return this;
  }

  @ApiResourceProperty
  public String ancient() {
    return HebrewConverter.toAncient(constantsOnly);
  }

  @NonIndexed
  public String getTransliteration() {
    return transliteration;
  }

  public Lexicon setTransliteration(String transliteration) {
    this.transliteration = transliteration;
    return this;
  }

  @NonIndexed
  public String getPronunciation() {
    return pronunciation;
  }

  public Lexicon setPronunciation(String pronunciation) {
    this.pronunciation = pronunciation;
    return this;
  }

  @NonIndexed
  public String getPartOfSpeech() {
    return partOfSpeech;
  }

  public Lexicon setPartOfSpeech(String partOfSpeech) {
    this.partOfSpeech = partOfSpeech;
    return this;
  }

  @NonIndexed
  public String getRootWord() {
    return rootWord;
  }

  public Lexicon setRootWord(String rootWord) {
    this.rootWord = rootWord;
    return this;
  }

  @NonIndexed
  public String getDictionaryAid() {
    return dictionaryAid;
  }

  public Lexicon setDictionaryAid(String dictionaryAid) {
    this.dictionaryAid = dictionaryAid;
    return this;
  }

  public ImmutableList<TranslationCount> getTranslationCounts() {
    return translationCounts;
  }

  public Lexicon setTranslationCounts(Iterable<TranslationCount> translationCounts) {
    this.translationCounts = ImmutableList.copyOf(translationCounts);
    return this;
  }

  public ImmutableList<Node> getOutline() {
    return outline;
  }

  public Lexicon setOutline(Iterable<Node> outline) {
    this.outline = ImmutableList.copyOf(outline);
    return this;
  }

  public StringBuilder getStrongsDefinition() {
    return strongsDefinition;
  }

  public Lexicon setStrongsDefinition(StringBuilder strongsDefinition) {
    this.strongsDefinition = strongsDefinition;
    return this;
  }

  public Lexicon setStrongsDefinition(String strongsDefinition) {
    return setStrongsDefinition(new StringBuilder(strongsDefinition));
  }

  @Deprecated
  public StringBuilder getBrownDriverBriggs() {
    return null;
  }

  @Deprecated
  public Lexicon setBrownDriverBriggs(StringBuilder brownDriverBriggs) {
    return this;
  }

  @Deprecated
  public Lexicon setBrownDriverBriggs(String brownDriverBriggs) {
    return this;
  }

  @Deprecated
  public ImmutableList<String> getBriggsReferences() {
    return null;
  }

  @Deprecated
  public Lexicon setBriggsReferences(Iterable<String> briggsReferences) {
    return this;
  }

  public String toString() {
    return String.format(
        "Strong's %s - %s - %s - %s - %s - %s - %s - %s - %s - %s",
        getId(), getWord(), getConstantsOnly(), getTransliteration(), getPronunciation(),
        getPartOfSpeech(), getRootWord(), getDictionaryAid(), getTranslationCounts(), getOutline());
  }

  public static class TranslationCount {
    private String word;
    private int count;

    public String getWord() {
      return word;
    }

    public TranslationCount setWord(String word) {
      this.word = word;
      return this;
    }

    public int getCount() {
      return count;
    }

    public TranslationCount setCount(int count) {
      this.count = count;
      return this;
    }

    public String toString() {
      return String.format("%s (%dx)", getWord(), getCount());
    }
  }

  public static class Node {
    private String value;
    private ImmutableList<Node> children;

    public String getValue() {
      return value;
    }

    public Node setValue(String value) {
      this.value = value;
      return this;
    }

    public ImmutableList<Node> getChildren() {
      return children;
    }

    public Node setChildren(Iterable<Node> children) {
      this.children = children == null ? null : ImmutableList.copyOf(children);
      return this;
    }

    public String toString() {
      return getValue() + (getChildren() == null ? "" : getChildren());
    }
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

  public static class Interlinear {
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
    private int bookNumber; // For sorting by Bible book number.
    private String translation;

    // FE only values
    private int matchValue;
    private String dss;
    private String dssDiff;

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
      return HebrewConverter.toAncient(constantsOnly);
    }

    @NonIndexed
    public String getMorphology() {
      return morphologyAbb != null ? morphologyAbb : morphology;
    }

    public Interlinear setMorphology(String morphology) {
      this.morphology = "".equals(morphology) ? null :morphology;
      return this;
    }

    @NonIndexed @Deprecated
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
          getTransliteration(), getWord(), getConstantsOnly(), dss != null ? " - " + dss : "", getTranslation());
    }
  }
}

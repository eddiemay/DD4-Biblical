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
  private String strongsId;

  // Non Indexed values
  private String transliteration;
  private String pronunciation;
  private String partOfSpeech;
  private String rootWord;
  private String dictionaryAid;
  private ImmutableList<TranslationCount> translationCounts;
  private ImmutableList<Node> outline;
  private StringBuilder strongsDefinition;
  private String translation;
  private Integer referenceCount;

  public Lexicon setId(String id) {
    super.setId(switch (id.length()) {
      case 2 -> id.charAt(0) + "000" + id.substring(1);
      case 3 -> id.charAt(0) + "00" + id.substring(1);
      case 4 -> id.charAt(0) + "0" + id.substring(1);
      default -> id;
    });
    return this;
  }

  public String getStrongsId() {
    return strongsId != null ? strongsId : getId();
  }

  public Lexicon setStrongsId(String strongsId) {
    this.strongsId = strongsId;
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
    return HebrewConverter.toAncientRtl(constantsOnly);
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

  public String getTranslation() {
    return null;
  }

  public Lexicon setTranslation(String translation) {
    this.translation = translation;
    return this;
  }

  public Integer getReferenceCount() {
    return referenceCount;
  }

  public Lexicon setReferenceCount(Integer referenceCount) {
    this.referenceCount = referenceCount;
    return this;
  }

  @ApiResourceProperty
  public String translation() {
    if (translation == null) {
      if (translationCounts != null && !translationCounts.isEmpty()) {
        return getTranslationCounts().get(0).getWord();
      } else if (getOutline() != null && !getOutline().isEmpty()) {
        String translation = getOutline().get(0).getValue();
        if (translation.contains(",")) {
          translation = translation.substring(0, translation.indexOf(','));
        }
        if (translation.contains(" = ")) {
          translation = translation.substring(0, translation.indexOf(" = "));
        }
        return translation;
      }
    }

    return translation;
  }

  public String toString() {
    return String.format("Strong's %s - %s - %s - %s - %s - %s - %s - %s - %s - %s",
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
}

package com.digitald4.biblical.model;

import com.google.common.collect.ImmutableSet;
import org.json.JSONObject;

public class AncientLexicon {
  private String word;
  private String definition;
  private String translation;
  private ImmutableSet<String> words;
  private ImmutableSet<String> kjvTranslations;
  private ImmutableSet<String> strongIds;

  public String getWord() {
    return word;
  }

  public AncientLexicon setWord(String word) {
    this.word = word;
    return this;
  }

  public ImmutableSet<String> getWords() {
    return words;
  }

  public AncientLexicon setWords(Iterable<String> words) {
    this.words = ImmutableSet.copyOf(words);
    return this;
  }

  public String getDefinition() {
    return definition;
  }

  public AncientLexicon setDefinition(String definition) {
    this.definition = definition;
    return this;
  }

  public String getTranslation() {
    return translation;
  }

  public AncientLexicon setTranslation(String translation) {
    this.translation = translation;
    return this;
  }

  public ImmutableSet<String> getKjvTranslations() {
    return kjvTranslations;
  }

  public AncientLexicon setKjvTranslations(Iterable<String> kjvTranslations) {
    this.kjvTranslations = ImmutableSet.copyOf(kjvTranslations);
    return this;
  }

  public ImmutableSet<String> getStrongIds() {
    return strongIds;
  }

  public AncientLexicon setStrongIds(Iterable<String> strongIds) {
    this.strongIds = ImmutableSet.copyOf(strongIds);
    return this;
  }

  public AncientLexicon addStrongIds(Iterable<String> strongIds) {
    if (this.strongIds == null) {
      return setStrongIds(strongIds);
    }

    this.strongIds =
        ImmutableSet.<String>builder().addAll(this.strongIds).addAll(strongIds).build();
    return this;
  }

  public String translation() {
    return kjvTranslations != null ? kjvTranslations.iterator().next() : translation;
  }

  @Override
  public String toString() {
    return new JSONObject(this).toString();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof AncientLexicon && toString().equals(obj.toString());
  }
}

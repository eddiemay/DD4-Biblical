package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.HebrewConverter.removePunctuation;
import static com.digitald4.biblical.util.HebrewConverter.toConstantsOnly;
import static com.digitald4.biblical.util.HebrewConverter.toFullHebrew;
import static com.digitald4.biblical.util.HebrewConverter.unfinalize;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.stream;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

public class MachineTranslator {
  private final TokenWordStore tokenWordStore;
  private final HebrewTokenizer subwordTokenizer;

  @Inject
  public MachineTranslator(TokenWordStore tokenWordStore, HebrewTokenizer subwordTokenizer) {
    this.tokenWordStore = tokenWordStore;
    this.subwordTokenizer = subwordTokenizer;
  }

  private SubToken getTranslation(String word, String strongsId, boolean isSuffix) {
    if (word.startsWith("##")) {
      word = word.substring(2);
    }

    SubToken subToken = new SubToken().setWord(word);
    ImmutableList<TokenWord> options = tokenWordStore.getOptions(word);
    if (options.isEmpty()) {
      return subToken.setTranslation("[UNK]").setTransliteration(HebrewConverter.transliterate(word, isSuffix));
    }

    TokenWord option = options.stream()
        .filter(o -> Objects.equals(strongsId, o.getStrongsId())).findFirst()
        .orElse(options.get(0));

    return subToken.setTranslation(isSuffix ? option.asSuffix() : option.getTranslation())
        .setStrongsId(option.getStrongsId())
        .setTransliteration(option.getTransliteration() != null ?
            option.getTransliteration() : HebrewConverter.transliterate(word, isSuffix));
  }

  public Interlinear translate(Interlinear interlinear) {
    AtomicBoolean wordFound = new AtomicBoolean();
    return interlinear.setSubTokens(
        subwordTokenizer
            .tokenizeWord(unfinalize(toFullHebrew(interlinear.getWord())), interlinear.getStrongsId())
            .stream()
            .map(subWord -> getTranslation(subWord, interlinear.getStrongsId(), wordFound.get()))
            .peek(subToken -> wordFound.set(wordFound.get() || subToken.getStrongsId() != null))
            .collect(toImmutableList()));
  }

  public ImmutableList<Interlinear> translate(Iterable<Interlinear> interlinears) {
    return stream(interlinears).map(this::translate).collect(toImmutableList());
  }

  public ImmutableList<Interlinear> translate(String text) {
    AtomicInteger index = new AtomicInteger();
    return translate(
        stream(removePunctuation(text).split(" "))
            .map(word -> new Interlinear()
                .setIndex(index.getAndIncrement()).setWord(word).setConstantsOnly(toConstantsOnly(word)))
            .collect(toImmutableList()));
  }

  public ImmutableList<Interlinear> translate(Scripture scripture) {
    if (scripture instanceof InterlinearScripture) {
      return translate(((InterlinearScripture) scripture).getInterlinears());
    }

    return translate(
        stream(removePunctuation(scripture.getText().toString()).split(" "))
            .map(word -> new Interlinear()
                .setBook(scripture.getBook())
                .setChapter(scripture.getChapter()).setVerse(scripture.getVerse())
                .setWord(word).setConstantsOnly(toConstantsOnly(word)))
            .collect(toImmutableList()));
  }
}

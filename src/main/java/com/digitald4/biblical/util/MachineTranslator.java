package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.HebrewConverter.toConstantsOnly;
import static com.digitald4.biblical.util.HebrewConverter.unfinalize;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.stream;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Provider;

public class MachineTranslator {
  private final Provider<Iterable<TokenWord>> tokenWordsProvider;
  private ImmutableListMultimap<String, TokenWord> tokenWordsByWord;
  private HebrewTokenizer subwordTokenizer;

  @Inject
  public MachineTranslator(Provider<Iterable<TokenWord>> tokenWordsProvider) {
    this.tokenWordsProvider = tokenWordsProvider;
  }

  public synchronized void init() {
    if (subwordTokenizer != null) {
      return;
    }

    ImmutableListMultimap.Builder<String, TokenWord> builder = ImmutableListMultimap.builder();
    subwordTokenizer = new HebrewTokenizer(
        stream(tokenWordsProvider.get())
            .flatMap(tw -> tw.getRoot().endsWith("ה")
                ? Stream.of(tw.copy().setRoot(tw.getRoot().substring(0, tw.getRoot().length() - 1)), tw)
                : Stream.of(tw))
            .peek(tokenWord -> builder.put(tokenWord.getRoot(), tokenWord))
            // .flatMap(tokenWord -> Stream.of(tokenWord.getWord(), "##" + tokenWord.getWord()))
            .collect(toImmutableSet()));
    tokenWordsByWord = builder.build();
  }

  public SubToken getTranslation(String word, String strongsId, boolean suffix) {
    if (word.startsWith("##")) {
      word = word.substring(2);
    }

    SubToken subToken = new SubToken().setWord(word);
    ImmutableList<TokenWord> options = tokenWordsByWord.get(word);
    if (options.isEmpty()) {
      return subToken.setTranslation(word);
    }

    TokenWord option = options.size() == 1 ? options.get(0) : options.stream()
        .filter(o -> Objects.equals(strongsId, o.getStrongsId())).findFirst()
        .orElse(options.get(0));

    return subToken.setTranslation(suffix ? option.asSuffix() : option.getTranslation())
        .setStrongsId(option.getStrongsId());
  }

  public Interlinear translate(Interlinear interlinear) {
    if (subwordTokenizer == null) init();

    AtomicBoolean wordFound = new AtomicBoolean();
    interlinear.setSubtokens(
        subwordTokenizer
            .tokenizeWord(unfinalize(interlinear.getConstantsOnly()), interlinear.getStrongsId())
            .stream()
            .map(subWord -> getTranslation(subWord, interlinear.getStrongsId(), wordFound.get()))
            .peek(subToken -> wordFound.set(wordFound.get() || subToken.getStrongsId() != null))
            .collect(toImmutableList()));
    return interlinear;
  }

  public ImmutableList<Interlinear> translate(Iterable<Interlinear> interlinears) {
    return stream(interlinears).map(this::translate).collect(toImmutableList());
  }

  public ImmutableList<Interlinear> translate(String text) {
    return translate(
        stream(text.split(" "))
            .map(word -> new Interlinear().setBook("").setWord(word).setConstantsOnly(toConstantsOnly(word)))
            .collect(toImmutableList()));
  }

  public ImmutableList<Interlinear> translate(Scripture scripture) {
    if (scripture instanceof InterlinearScripture) {
      return translate(((InterlinearScripture) scripture).getInterlinears());
    }

    return translate(
        stream(scripture.getText().toString().split(" "))
            .map(
                word ->
                    new Interlinear()
                        .setBook(scripture.getBook())
                        .setChapter(scripture.getChapter()).setVerse(scripture.getVerse())
                        .setWord(word).setConstantsOnly(toConstantsOnly(word)))
            .collect(toImmutableList()));
  }
}

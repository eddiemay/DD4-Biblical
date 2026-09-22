package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.HebrewConverter.removePunctuation;
import static com.digitald4.biblical.util.HebrewConverter.toConstantsOnly;
import static com.digitald4.biblical.util.HebrewConverter.toGeezConstants;
import static com.digitald4.biblical.util.HebrewConverter.toRestored;
import static com.digitald4.biblical.util.HebrewConverter.transliterate;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.stream;
import static java.util.Comparator.reverseOrder;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.store.TokenWordStore.TokenOption;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord.TokenType;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
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

  private ImmutableList<SubToken> getTranslation(String word, ImmutableList<TokenOption> options, String strongsId) {
    if (options.isEmpty()) {
      if (word.length() < 2) {
        return ImmutableList.of();
      }

      return ImmutableList.of(new SubToken().setWord(word).setTranslation("[UNK]")
          .setTransliteration(transliterate(word, false)));
    }

    var strongsMatch = options.stream().filter(o -> Objects.equals(strongsId, o.getStrongsId())).findFirst();
    if (strongsMatch.isPresent()) {
      return strongsMatch.get().toSubTokens();
    }

    var countsByStrongsId = tokenWordStore.getCountsByStrongsId();

    return options.stream()
        .max(Comparator.comparing(tw -> countsByStrongsId.getOrDefault(tw.getStrongsId(), 0)))
        .get().toSubTokens();
  }

  public Interlinear translate(Interlinear interlinear) {
    String strongsId = interlinear.getStrongsId();
    String word = toRestored(interlinear.getWord());
    return interlinear.setSubTokens(
        getTranslation(word, subwordTokenizer.getTokenizeOptions(word, strongsId), strongsId));
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

  public void invalidateCache() {
    tokenWordStore.reset();
  }
}

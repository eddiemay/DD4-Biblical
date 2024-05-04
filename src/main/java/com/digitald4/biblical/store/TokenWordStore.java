package com.digitald4.biblical.store;

import static com.digitald4.common.util.FormatText.toCapitalized;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Streams.stream;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Function.identity;

import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.Query;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Provider;

public class TokenWordStore extends GenericStore<TokenWord, String> {
  private final Provider<Iterable<TokenWord>> tokenWordsProvider;
  private final Provider<Map<String, Lexicon>> lexiconProvider;
  private volatile ImmutableListMultimap<String, TokenWord> tokenWordsByWord;
  private volatile ImmutableListMultimap<String, TokenWord> tokenWordsByStrongsId;

  @Inject
  public TokenWordStore(Provider<DAO> dao, Provider<Iterable<TokenWord>> tokenWordsProvider,
                        Provider<Map<String, Lexicon>> lexiconProvider) {
    super(TokenWord.class, dao);
    this.tokenWordsProvider = tokenWordsProvider;
    this.lexiconProvider = lexiconProvider;
  }

  public synchronized void init() {
    if (tokenWordsByWord != null) {
      return;
    }

    ImmutableMap<String, Lexicon> lexicons = ImmutableMap.copyOf(lexiconProvider.get());
    ImmutableMap<String, Integer> countsByStrongsId =
        lexiconProvider.get().values().stream().collect(toImmutableMap(Lexicon::getId, Lexicon::getReferenceCount));

    tokenWordsByWord = Stream
        .concat(list(Query.forList()).getItems().stream(), stream(tokenWordsProvider.get()))
        .filter(tw -> tw.getWord().length() > 0)
        .flatMap(tw -> {
          try {
            if (tw.getWord().endsWith("ה")) {
              Lexicon lexicon = lexicons.get(tw.getStrongsId());
              // If this words ends with a ה and not a proper noun such as a name, add a second entry without the ה.
              if (lexicon == null || !"proper masculine noun".equals(lexicon.getPartOfSpeech())) {
                return Stream.of(tw.copy().setWord(tw.getWord().substring(0, tw.getWord().length() - 1)), tw);
              }
            } else if (tw.getWord().charAt(0) >= 'α' && tw.getWord().charAt(0) <= 'ω') {
              // For Greek words add a second capitalized entry.
              return Stream.of(
                  tw.copy().setWord(toCapitalized(tw.getWord())).setTranslation(toCapitalized(tw.getTranslation())),
                  tw);
            }
            return Stream.of(tw);
          } catch (StringIndexOutOfBoundsException e) {
            throw new DD4StorageException("Error processing " + tw, e);
          }
        })
        .distinct()
        .peek(tw -> {
          if (tw.getTokenType() == null) {
            Integer referenceCount = countsByStrongsId.get(tw.getStrongsId());
            if (referenceCount != null && referenceCount < 7) {
              // For all these words that have less than a handful of references,
              // we don't want to use them for non-strong's translation.
              tw.setTokenType(TokenWord.TokenType.WORD_STRONGS_MATCH_ONLY);
            }
          }
        })
        .filter(tw -> tw.getTokenType() != TokenWord.TokenType.DISABLED)
        .sorted(comparing(TokenWord::getWord)
            .thenComparing(TokenWord::tokenType)
            .thenComparing(tw -> countsByStrongsId.getOrDefault(tw.getStrongsId(), 0), reverseOrder()))
        .collect(toImmutableListMultimap(TokenWord::getWord, identity()));
    tokenWordsByStrongsId = tokenWordsByWord.entries().stream().map(Entry::getValue)
        .filter(tokenWord -> Objects.nonNull(tokenWord.getStrongsId()))
        .collect(toImmutableListMultimap(TokenWord::getStrongsId, identity()));
  }

  public void reset() {
    tokenWordsByWord = tokenWordsByStrongsId = null;
  }

  public ImmutableList<TokenWord> getOptions(String word) {
    if (tokenWordsByWord == null) {
      init();
    }

    return tokenWordsByWord.get(word);
  }

  public ImmutableList<TokenWord> getTranslations(String strongsId) {
    if (tokenWordsByStrongsId == null) {
      init();
    }

    return tokenWordsByStrongsId.get(strongsId);
  }

  public ImmutableCollection<TokenWord> getAll() {
    if (tokenWordsByWord == null) {
      init();
    }

    return tokenWordsByWord.values();
  }

  @Override
  protected Iterable<TokenWord> postprocess(Iterable<TokenWord> entities) {
    reset();
    return super.postprocess(entities);
  }
}

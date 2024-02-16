package com.digitald4.biblical.store;

import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Streams.stream;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Function.identity;

import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Provider;

public class TokenWordStore extends GenericStore<TokenWord, String> {
  private final Provider<Iterable<TokenWord>> tokenWordsProvider;
  private final LexiconStore lexiconStore;
  private volatile ImmutableListMultimap<String, TokenWord> tokenWordsByWord;
  private volatile ImmutableListMultimap<String, TokenWord> tokenWordsByStrongsId;

  @Inject
  public TokenWordStore(Provider<DAO> dao, Provider<Iterable<TokenWord>> tokenWordsProvider,
      LexiconStore lexiconStore) {
    super(TokenWord.class, dao);
    this.tokenWordsProvider = tokenWordsProvider;
    this.lexiconStore = lexiconStore;
  }

  public synchronized void init() {
    if (tokenWordsByWord != null) {
      return;
    }

    ImmutableMap<String, Integer> countsByStrongsId =
        lexiconStore.list(Query.forList(Filter.of("referenceCount", ">", 0))).getItems().stream()
            .collect(toImmutableMap(Lexicon::getStrongsId, Lexicon::getReferenceCount));

    tokenWordsByWord = Stream
        .concat(list(Query.forList()).getItems().stream(), stream(tokenWordsProvider.get()))
        .flatMap(tw -> tw.getWord().endsWith("ה")
            ? Stream.of(tw.copy().setWord(tw.getWord().substring(0, tw.getWord().length() - 1)), tw)
            : Stream.of(tw))
        .distinct()
        .sorted(comparing(TokenWord::getWord)
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

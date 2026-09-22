package com.digitald4.biblical.store;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.Streams.stream;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Function.identity;

import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord.TokenType;
import com.digitald4.common.exception.DD4StorageException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Provider;

public class TokenWordStore {
  private final Provider<Iterable<TokenWord>> tokenWordsProvider;
  private final Provider<Map<String, Lexicon>> lexiconProvider;
  private volatile ImmutableListMultimap<String, TokenWord> tokenWordsByWord;
  private volatile ImmutableListMultimap<String, TokenWord> tokenWordsByStrongsId;
  private volatile ImmutableMap<String, Integer> countsByStrongsId;

  @Inject
  public TokenWordStore(
      Provider<Iterable<TokenWord>> tokenWordsProvider, Provider<Map<String, Lexicon>> lexiconProvider) {
    this.tokenWordsProvider = tokenWordsProvider;
    this.lexiconProvider = lexiconProvider;
  }

  public synchronized void init() {
    if (tokenWordsByStrongsId != null) {
      return;
    }

    ImmutableMap<String, Lexicon> lexicons = ImmutableMap.copyOf(lexiconProvider.get());
    countsByStrongsId =
        lexiconProvider.get().values().stream().collect(toImmutableMap(Lexicon::getId, Lexicon::getReferenceCount));

    tokenWordsByWord = stream(tokenWordsProvider.get())
        .filter(tw -> !tw.getWord().isEmpty())
        .flatMap(tw -> {
          try {
            Lexicon lexicon = tw.getStrongsId() == null ? null : lexicons.get(tw.getStrongsId());
            if (tw.getWord().length() > 2 && tw.getWord().endsWith("ה")) {
              // If this words ends with a ה and not a proper noun such as a name, add a second entry without the ה.
              if (lexicon == null || !"proper masculine noun".equals(lexicon.getPartOfSpeech())) {
                return Stream.of(tw.copy().setWord(tw.getWord().substring(0, tw.getWord().length() - 1)), tw);
              }
            } else if (tw.getWord().length() == 3 && lexicon != null && "verb".equals(lexicon.getPartOfSpeech())) {
              // For verbs we want to create infixes for "er", "ed" and "ing".
              return Stream.of(
                  tw,
                  tw.copy().setWord(tw.getWord().charAt(0) + "ו" + tw.getWord().substring(1)).setTranslation(tw.getTranslation() + "er").setDerived(true),
                  tw.copy().setWord(tw.getWord().substring(0, 2) + "ו" + tw.getWord().substring(2)).setTranslation(tw.getTranslation() + "ed").setDerived(true),
                  tw.copy().setWord(tw.getWord().substring(0, 2) + "י" + tw.getWord().substring(2)).setTranslation(tw.getTranslation() + "ing").setDerived(true)
              );
            } else if (tw.getWord().length() > 3 && (tw.getWord().endsWith("ς") || tw.getWord().endsWith("ν") || tw.getWord().endsWith("υ"))) {
              return Stream.of(tw, tw.copy().setWord(tw.getWord().substring(0, tw.getWord().length() - 1)).setDerived(true));
            }
            return Stream.of(tw);
          } catch (StringIndexOutOfBoundsException e) {
            throw new DD4StorageException("Error processing " + tw, e);
          }
        })
        // For Greek words add a second capitalized entry.
        .flatMap(tw -> tw.getWord().charAt(0) >= 'α' && tw.getWord().charAt(0) <= 'ω' ? Stream.of(
            tw.copy().setWord(toCapitalized(tw.getWord())).setTranslation(toCapitalized(tw.getTranslation())), tw)
            : Stream.of(tw))
        .distinct()
        .peek(tw -> {
          if (tw.tokenType() == TokenType.WORD) {
            Integer referenceCount = countsByStrongsId.get(tw.getStrongsId());
            if (referenceCount != null && referenceCount < 7) {
              // For all these words that have less than a handful of references,
              // we don't want to use them for non-strong's translation.
              tw.setTokenType(TokenType.WORD_STRONGS_MATCH_ONLY);
            }
          }
        })
        .filter(tw -> tw.tokenType() != TokenWord.TokenType.DISABLED)
        .sorted(comparing(TokenWord::tokenType)
            .thenComparing(TokenWord::isDerived)
            .thenComparing(tw -> countsByStrongsId.getOrDefault(tw.getStrongsId(), 0), reverseOrder())
            .thenComparing(TokenWord::getWord))
        .collect(toImmutableListMultimap(TokenWord::getWord, identity()));

     tokenWordsByStrongsId = tokenWordsByWord.entries().stream().map(Entry::getValue)
        .filter(tokenWord -> Objects.nonNull(tokenWord.getStrongsId()))
        .collect(toImmutableListMultimap(TokenWord::getStrongsId, identity()));
  }

  public ImmutableMap<String, Integer> getCountsByStrongsId() {
    if (countsByStrongsId == null) {
      init();
    }

    return countsByStrongsId;
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

  public ImmutableList<TokenOptions> getOptions(String word, int len) {
    if (tokenWordsByWord == null) {
      init();
    }

    return IntStream.range(0, word.length() - len + 1)
        .mapToObj(start -> new TokenOptions(start, tokenWordsByWord.get(word.substring(start, start + len))))
        .filter(tokenOptions -> !tokenOptions.getWords().isEmpty())
        .collect(toImmutableList());
  }

  public static String toCapitalized(String text) {
    return Strings.isNullOrEmpty(text) ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  public ImmutableList<TokenWord> getTranslations(String strongsId) {
    if (tokenWordsByStrongsId == null) {
      init();
    }

    return tokenWordsByStrongsId.get(strongsId);
  }

  public ImmutableCollection<TokenWord> getAll() {
    if (tokenWordsByStrongsId == null) {
      init();
    }

    return tokenWordsByWord.values();
  }

  public static class TokenOption {
    private final TokenWord root;
    private final ImmutableList<TokenWord> preTokens;
    private final ImmutableList<TokenWord> postTokens;

    public TokenOption(TokenWord root, ImmutableList<TokenWord> preTokens, ImmutableList<TokenWord> postTokens) {
      this.root = root;
      this.preTokens = preTokens;
      this.postTokens = postTokens;
    }

    public TokenWord getRoot() {
      return root;
    }

    public String getStrongsId() {
      return root.getStrongsId();
    }

    public ImmutableList<SubToken> toSubTokens() {
      return ImmutableList.<SubToken>builder()
          .addAll(preTokens.stream().map(tw -> tw.toSubToken(false)).collect(toImmutableList()))
          .add(root.toSubToken(false))
          .addAll(postTokens.stream().map(tw -> tw.toSubToken(true)).collect(toImmutableList()))
          .build();
    }
  }

  public static class TokenOptions {
    private final int start;
    private final ImmutableList<TokenWord> words;
    private ImmutableList<TokenWord> preTokens;
    private ImmutableList<TokenWord> postTokens;

    public TokenOptions(int start, ImmutableList<TokenWord> words) {
      this.start = start;
      this.words = words;
    }

    public int getStart() {
      return start;
    }

    public ImmutableList<TokenWord> getWords() {
      return words;
    }

    public ImmutableList<TokenWord> getPreTokens() {
      return preTokens;
    }

    public TokenOptions setPreTokens(Iterable<TokenWord> preTokens) {
      this.preTokens = ImmutableList.copyOf(preTokens);
      return this;
    }

    public ImmutableList<TokenWord> getPostTokens() {
      return postTokens;
    }

    public TokenOptions setPostTokens(Iterable<TokenWord> postTokens) {
      this.postTokens = ImmutableList.copyOf(postTokens);
      return this;
    }

    public boolean requiresPrefix() {
      return start > 0;
    }

    public boolean requiresPostfix(int strLen) {
      return start + getLength() < strLen;
    }

    public int getLength() {
      return words.get(0).getWord().length();
    }
  }
}

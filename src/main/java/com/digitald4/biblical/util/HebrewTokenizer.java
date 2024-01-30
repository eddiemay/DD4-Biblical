package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.HebrewConverter.unfinalize;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;

import com.digitald4.biblical.model.AncientLexicon;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord.TokenType;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.util.Objects;
import org.json.JSONObject;

public class HebrewTokenizer {
  public static final String UNKNOWN_WORD_DEFAULT = "[UNK]";
  private enum SearchState {PREFIX, WORD_MATCH_STRONGS, WORD, SUFFIX};
  private final ImmutableListMultimap<String, TokenWord> tokenMap;
  private final ImmutableList<String> unknownReturn;

  public HebrewTokenizer(Iterable<TokenWord> tokenList, String unknownWord) {
    tokenMap = stream(tokenList).collect(toImmutableListMultimap(TokenWord::getRoot, identity()));
    this.unknownReturn = unknownWord == null ? null : ImmutableList.of(unknownWord);
  }

  public HebrewTokenizer(Iterable<TokenWord> tokenList) {
    this(tokenList, null);
  }

  public ImmutableList<ImmutableList<String>> tokenize(String sentence) {
    return stream(sentence.split(" ")).map(this::tokenizeWord).collect(toImmutableList());
  }

  public ImmutableList<String> tokenizeWord(String word) {
    ImmutableList<String> tokenized = tokenizeWord(word, null, SearchState.WORD);
    if (tokenized.isEmpty()) {
      return unknownReturn == null ? ImmutableList.of(word) : unknownReturn;
    }

    return tokenized;
  }

  public ImmutableList<String> tokenizeWord(String word, String strongsId) {
    if (word.length() == 1 && strongsId == null) {
      return ImmutableList.of();
    }

    ImmutableList<String> tokenized = tokenizeWord(word, strongsId, SearchState.WORD_MATCH_STRONGS);
    return tokenized.isEmpty() ? tokenizeWord(word) : tokenized;
  }

  private ImmutableList<String> tokenizeWord(String word, String strongsId, SearchState state) {
    int strLen = word.length();
    for (int len = strLen; len > 0; len--) {
      for (int start = 0; start + len <= strLen; start++) {
        String subword = word.substring(start, start + len);
        ImmutableList<TokenWord> options = tokenMap.get(subword);
        if (!options.isEmpty() && hasGoodOption(state, options, strongsId)) {
          ImmutableList<String> pretokens;
          if (start > 0) {
            pretokens = tokenizeWord(word.substring(0, start), strongsId, SearchState.PREFIX);
            if (pretokens.isEmpty()) {
              continue;
            }
          } else {
            pretokens = ImmutableList.of();
          }

          ImmutableList<String> postTokens;
          if (start + len < strLen) {
            postTokens = tokenizeWord(word.substring(start + len), strongsId, SearchState.SUFFIX);
            if (postTokens.isEmpty()) {
              continue;
            }
          } else {
            postTokens = ImmutableList.of();
          }

          return ImmutableList.<String>builder()
              .addAll(pretokens).add(subword).addAll(postTokens).build();
        }
      }
    }

    return ImmutableList.of();
  }

  public static boolean hasGoodOption(SearchState state, ImmutableList<TokenWord> options, String strongsId) {
    switch (state) {
      case WORD_MATCH_STRONGS:
        return options.stream().anyMatch(
            o -> o.tokenType() == TokenType.WORD && Objects.equals(strongsId, o.getStrongsId()));
      case WORD: return options.stream().anyMatch(o -> o.tokenType() == TokenType.WORD);
      case PREFIX:
        return options.stream().map(TokenWord::tokenType)
            .anyMatch(tt -> tt == TokenType.PREFIX || tt == TokenType.PREFIX_ONLY);
      case SUFFIX:
        return options.stream().anyMatch(o ->
            o.getTokenType() == TokenType.SUFFIX || o.getTokenType() == TokenType.SUFFIX_ONLY
                || o.getTokenType() == TokenType.PREFIX && o.asSuffix() != null);
    }
    throw new IllegalArgumentException("How did we get here?");
  }

  public static class TokenWord {
    private String root;
    private String strongsId;
    private String translation;
    private String withSuffix;
    enum TokenType {PREFIX, PREFIX_ONLY, SUFFIX, SUFFIX_ONLY, WORD, WHOLE_WORD_ONLY}
    private TokenType tokenType;

    public String getRoot() {
      return root;
    }

    public TokenWord setRoot(String root) {
      this.root = root;
      return this;
    }

    @Deprecated
    public String getWord() {
      return null;
    }

    @Deprecated
    public TokenWord setWord(String word) {
      this.root = word;
      return this;
    }

    public String getStrongsId() {
      return strongsId;
    }

    public TokenWord setStrongsId(String strongsId) {
      this.strongsId = strongsId;
      return this;
    }

    public String getTranslation() {
      return translation;
    }

    public TokenWord setTranslation(String translation) {
      this.translation = translation;
      return this;
    }

    public String getWithSuffix() {
      return withSuffix;
    }

    public TokenWord setWithSuffix(String withSuffix) {
      this.withSuffix = withSuffix;
      return this;
    }

    @Deprecated
    public String getAsSuffix() {
      return null;
    }

    @Deprecated
    public TokenWord setAsSuffix(String withSuffix) {
      this.withSuffix = withSuffix;
      return this;
    }

    public String asSuffix() {
      return withSuffix == null ? translation : withSuffix;
    }

    public TokenType getTokenType() {
      return tokenType;
    }

    public TokenWord setTokenType(TokenType tokenType) {
      this.tokenType = tokenType;
      return this;
    }

    public TokenType tokenType() {
      return tokenType == null ? TokenType.WORD : tokenType;
    }

    @Override
    public String toString() {
      return String.format(
          "%s,%s,%s%s", root, translation, strongsId, withSuffix == null ? "" : "," + withSuffix);
    }

    public TokenWord copy() {
      return JSONUtil.toObject(TokenWord.class, new JSONObject(this));
    }

    public static TokenWord from(Lexicon lexicon) {
      return new TokenWord()
          .setRoot(unfinalize(lexicon.getConstantsOnly()))
          .setTranslation(lexicon.translation())
          .setStrongsId(lexicon.getStrongsId());
    }

    public static ImmutableList<TokenWord> from(AncientLexicon ancientLexicon) {
      if (ancientLexicon.getStrongIds() == null || ancientLexicon.getTranslation() == null) {
        return ImmutableList.of();
      }

      return ancientLexicon.getWords().stream()
          .flatMap(
              word -> ancientLexicon.getStrongIds().stream().map(
                  strongsId -> new TokenWord()
                      .setRoot(unfinalize(word))
                      .setTranslation(ancientLexicon.translation())
                      .setStrongsId(strongsId.replaceAll("A", "H"))))
          .collect(toImmutableList());
    }
  }
}


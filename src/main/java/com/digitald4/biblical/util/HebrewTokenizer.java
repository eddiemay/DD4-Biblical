package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.HebrewConverter.unfinalize;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

import com.digitald4.biblical.model.AncientLexicon;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord.TokenType;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import javax.inject.Inject;
import org.json.JSONObject;

public class HebrewTokenizer {
  private enum SearchState {PREFIX, WORD_MATCH_STRONGS, WORD, SUFFIX};
  private final TokenWordStore tokenWordStore;

  @Inject
  public HebrewTokenizer(TokenWordStore tokenWordStore) {
    this.tokenWordStore = tokenWordStore;
  }

  public ImmutableList<ImmutableList<String>> tokenize(String sentence) {
    return stream(sentence.split(" ")).map(this::tokenizeWord).collect(toImmutableList());
  }

  public ImmutableList<String> tokenizeWord(String word) {
    ImmutableList<String> tokenized = tokenizeWord(word, null, SearchState.WORD);
    return tokenized.isEmpty() ? ImmutableList.of(word) : tokenized;
  }

  public ImmutableList<String> tokenizeWord(String word, String strongsId) {
    if (word.length() == 1 && strongsId == null) {
      return ImmutableList.of();
    }

    ImmutableList<String> tokenized =
        tokenizeWord(word, strongsId, strongsId != null ? SearchState.WORD_MATCH_STRONGS : SearchState.WORD);
    return tokenized.isEmpty() ? tokenizeWord(word) : tokenized;
  }

  private ImmutableList<String> tokenizeWord(String word, String strongsId, SearchState state) {
    int strLen = word.length();
    for (int len = strLen; len > 0; len--) {
      for (int start = 0; start + len <= strLen; start++) {
        String subword = word.substring(start, start + len);
        ImmutableList<TokenWord> options = tokenWordStore.getOptions(subword);
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

          return ImmutableList.<String>builder().addAll(pretokens).add(subword).addAll(postTokens).build();
        }
      }
    }

    return ImmutableList.of();
  }

  public static boolean hasGoodOption(SearchState state, ImmutableList<TokenWord> options, String strongsId) {
    return switch (state) {
      case WORD_MATCH_STRONGS -> options.stream().anyMatch(o ->
          (o.tokenType() == TokenType.WORD || o.getTokenType() == TokenType.WORD_STRONGS_MATCH_ONLY)
              && Objects.equals(strongsId, o.getStrongsId()));
      case WORD -> options.stream().anyMatch(o -> o.tokenType() == TokenType.WORD);
      case PREFIX -> options.stream().map(TokenWord::tokenType)
          .anyMatch(tt -> tt == TokenType.PREFIX || tt == TokenType.PREFIX_ONLY);
      case SUFFIX -> options.stream().anyMatch(o -> o.getTokenType() == TokenType.SUFFIX
          || o.getTokenType() == TokenType.SUFFIX_ONLY || o.getTokenType() == TokenType.PREFIX && o.asSuffix() != null);
    };
  }

  public static class TokenWord {
    private String word;
    private String strongsId;
    private String translation;
    private String asSuffix;
    private String transliteration;
    public enum TokenType {PREFIX, PREFIX_ONLY, SUFFIX, SUFFIX_ONLY, WORD, WORD_STRONGS_MATCH_ONLY, DISABLED}
    private TokenType tokenType;

    public String getId() {
      return word + (strongsId == null ? "" : "-" + strongsId);
    }

    public TokenWord setId(String id) {
      return this;
    }

    @Deprecated
    private String getRoot() {
      return null;
    }

    @Deprecated
    public TokenWord setRoot(String root) {
      this.word = root;
      return this;
    }

    public String getWord() {
      return word;
    }

    public TokenWord setWord(String word) {
      this.word = word;
      return this;
    }

    public String getStrongsId() {
      return strongsId;
    }

    public String strongsId() {
      return strongsId == null ? "" : strongsId;
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

    @Deprecated
    public String getWithSuffix() {
      return null;
    }

    @Deprecated
    private TokenWord setWithSuffix(String withSuffix) {
      this.asSuffix = withSuffix;
      return this;
    }

    public String getAsSuffix() {
      return asSuffix;
    }

    public TokenWord setAsSuffix(String asSuffix) {
      this.asSuffix = asSuffix;
      return this;
    }

    public String asSuffix() {
      return asSuffix == null ? translation : asSuffix;
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

    public String getTransliteration() {
      return transliteration;
    }

    public TokenWord setTransliteration(String transliteration) {
      this.transliteration = transliteration;
      return this;
    }

    @Override
    public String toString() {
      return String.format("%s,%s,%s%s", word, translation, strongsId, asSuffix == null ? "" : "," + asSuffix);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof TokenWord && getId().equals(((TokenWord) obj).getId());
    }

    @Override
    public int hashCode() {
      return getId().hashCode();
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


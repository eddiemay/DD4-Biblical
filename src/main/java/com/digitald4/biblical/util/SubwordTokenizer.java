package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.stream;

import com.digitald4.biblical.util.MachineTranslator.TokenWord;
import com.digitald4.biblical.util.MachineTranslator.TokenWord.TokenType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

public class SubwordTokenizer {
  public static final String UNKNOWN_WORD_DEFAULT = "[UNK]";
  private final ImmutableListMultimap<String, TokenType> tokenMap;
  private final ImmutableList<String> unknownReturn;

  public SubwordTokenizer(Iterable<TokenWord> tokenList, String unknownWord) {
    this.tokenMap = stream(tokenList)
        .collect(toImmutableListMultimap(TokenWord::getWord, TokenWord::tokenType));
    this.unknownReturn = unknownWord == null ? null : ImmutableList.of(unknownWord);
  }

  public SubwordTokenizer(Iterable<TokenWord> tokenList) {
    this(tokenList, UNKNOWN_WORD_DEFAULT);
  }

  public ImmutableList<ImmutableList<String>> tokenize(String sentence) {
    return stream(sentence.split(" ")).map(this::tokenizeWord).collect(toImmutableList());
  }

  public ImmutableList<String> tokenizeWord(String word) {
    ImmutableList<String> tokenized = tokenizeWord(word, false);
    if (tokenized.isEmpty()) {
      return unknownReturn == null ? ImmutableList.of(word) : unknownReturn;
    }

    return tokenized;
  }

  private ImmutableList<String> tokenizeWord(String word, boolean isSuffix) {
    int strLen = word.length();
    for (int len = strLen; len > 0; len--) {
      for (int start = 0; start + len <= strLen; start++) {
        String subword = word.substring(start, start + len);
        ImmutableList<TokenType> tokenTypes = tokenMap.get(subword);
        if (!tokenTypes.isEmpty()) {
          if (isSuffix && tokenTypes.stream().allMatch(tt -> tt == TokenType.PREFIX_ONLY)) {
            continue;
          }
          ImmutableList<String> pretokens;
          if (start > 0) {
            pretokens = tokenizeWord(word.substring(0, start), isSuffix);
            if (pretokens.isEmpty()) {
              continue;
            }
          } else {
            pretokens = ImmutableList.of();
          }

          ImmutableList<String> postTokens;
          if (start + len < strLen) {
            postTokens = tokenizeWord(word.substring(start + len), true);
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
}

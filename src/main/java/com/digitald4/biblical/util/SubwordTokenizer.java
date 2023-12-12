package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class SubwordTokenizer {
  public static final String UNKNOWN_WORD_DEFAULT = "[UNK]";
  private final ImmutableSet<String> tokenList;
  private final ImmutableList<String> unknownReturn;

  public SubwordTokenizer(Iterable<String> tokenList, String unknownWord) {
    this.tokenList = ImmutableSet.copyOf(tokenList);
    this.unknownReturn = unknownWord == null ? null : ImmutableList.of(unknownWord);
  }

  public SubwordTokenizer(Iterable<String> tokenList) {
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
        String subword = (start > 0 || isSuffix ? "##" : "") + word.substring(start, start + len);
        if (tokenList.contains(subword)) {
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

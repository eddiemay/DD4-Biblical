package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;

public class BPETokenizer {
  private final ImmutableList.Builder<String> tokens = ImmutableList.builder();

  public ImmutableList<BPEToken> tokenize(String text) {
    ImmutableList.Builder<String> tokens = ImmutableList.builder();
    stream(text.split(" ")).forEach(word -> {
      int strLen = word.length();
      for (int len = strLen; len > 0; len--) {
        for (int start = 0; start + len <= strLen; start++) {
          String token = word.substring(start, start + len);
          tokens.add(token);
          this.tokens.add(token);
        }
      }
    });

    return toMap(tokens);
  }

  public ImmutableList<BPEToken> getTokens() {
    return toMap(tokens);
  }

  private ImmutableList<BPEToken> toMap(ImmutableList.Builder<String> tokens) {
    ImmutableList<BPEToken> bpeTokens = tokens.build().stream().collect(groupingBy(identity(), counting())).entrySet().stream()
        .map(e -> new BPEToken(e.getKey(), e.getValue().intValue()))
        .sorted(comparing(bt -> bt.getRoot().length(), reverseOrder()))
        .collect(toImmutableList());

    for (int x = 0; x < bpeTokens.size(); x++) {
      BPEToken bt1 = bpeTokens.get(x);
      for (int y = x + 1; y < bpeTokens.size(); y++) {
        BPEToken bt2 = bpeTokens.get(y);
        if (bt1.getRoot().contains(bt2.getRoot())) {
          bt2.setCount(bt2.getCount() - bt1.getCount());
        }
      }
    }

    return bpeTokens.stream().filter(bt -> bt.getCount() > 0).collect(toImmutableList());
  }

  public static class BPEToken {
    private String root;
    private int count;

    public BPEToken(String root, int count) {
      this.root = root;
      this.count = count;
    }

    public String getRoot() {
      return root;
    }

    public BPEToken setRoot(String root) {
      this.root = root;
      return this;
    }

    public int getCount() {
      return count;
    }

    public BPEToken setCount(int count) {
      this.count = count;
      return this;
    }

    @Override
    public String toString() {
      return root + "=" + count;
    }
  }
}

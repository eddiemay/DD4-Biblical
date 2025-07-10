package com.digitald4.biblical.util;

import com.google.common.collect.ImmutableList;

public class Constants {
  public final static String BASE_URL = "https://dd4-biblical.appspot.com/";
  public final static String API_URL = BASE_URL + "_api";
  public final static String API_VERSION = "v1";

  public final static ImmutableList<String> VOCAB_FILES = ImmutableList.of(
      "heb_prefixes.jsonl", "heb_vocab_overrides.jsonl",
      "heb_vocab_lexicon_ancient.jsonl", "heb_vocab_lexicon_strongs.jsonl",
      "gk_vocab_overrides.jsonl", "gk_vocab_lexicon_strongs.jsonl",
      "gez_prefixes.jsonl", "gez_vocab.jsonl");

}

package com.digitald4.biblical.util;

import com.google.common.collect.ImmutableList;

public class Constants {
  public final static String BASE_URL = "https://dd4-biblical.appspot.com/";
  public final static String API_URL = BASE_URL + "_api";
  public final static String API_VERSION = "v1";

  public final static ImmutableList<String> VOCAB_FILES = ImmutableList.of("heb_prefixes.json",
      "heb_vocab_overrides.json", "heb_vocab_lexicon_ancient.json", "heb_vocab_lexicon_strongs.json",
      "gk_vocab_overrides.json", "gk_vocab_lexicon_strongs.json", "gez_prefixes.json", "gez_vocab.json");

}

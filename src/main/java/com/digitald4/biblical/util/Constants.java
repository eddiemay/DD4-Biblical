package com.digitald4.biblical.util;

import com.google.common.collect.ImmutableList;

public class Constants {
  public final static String BASE_URL = "https://dd4-biblical.appspot.com/";
  public final static String API_URL = BASE_URL + "_api";
  public final static String API_VERSION = "v1";

  public final static ImmutableList<String> VOCAB_FILES = ImmutableList.of("heb_vocab_prefixes.txt",
      "heb_vocab_overrides.txt", "heb_vocab_lexicon_ancient.txt", "heb_vocab_lexicon_strongs.txt",
      "gk_vocab_overrides.txt", "gk_vocab_lexicon_strongs.txt");

}

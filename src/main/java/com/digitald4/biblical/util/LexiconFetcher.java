package com.digitald4.biblical.util;

import com.digitald4.biblical.model.Lexicon;

public interface LexiconFetcher {
  Lexicon getLexicon(String strongsId);
}

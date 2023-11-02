package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.model.Lexicon.Interlinear;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.VerseRange;
import com.google.common.collect.ImmutableList;

public interface LexiconFetcher {
  Lexicon getLexicon(String strongsId);
  ImmutableList<Interlinear> fetchInterlinear(VerseRange verseRange);
  ImmutableList<Interlinear> fetchInterlinear(
      BibleBook book, int chapter, int startVerse, int endVerse);
}

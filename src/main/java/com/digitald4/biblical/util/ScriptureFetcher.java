package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.google.common.collect.ImmutableList;

public interface ScriptureFetcher {
  ImmutableList<Scripture> fetch(String version, BibleBook book, int chapter);

  String getChapterUrl(String version, ScriptureReferenceProcessor.VerseRange verseRange);

  String getVerseUrl(Scripture scripture);

  static String trim(String text) {
    return text.replace("\u00a0", "").trim();
  }
}

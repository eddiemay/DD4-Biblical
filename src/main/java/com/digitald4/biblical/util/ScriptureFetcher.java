package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.google.common.collect.ImmutableList;

public interface ScriptureFetcher {
  ImmutableList<Scripture> fetch(String version, String language, BibleBook book, int chapter);

  static String trim(String text) {
    return text.replace("\u00a0", "").replaceAll("\u202C", "").trim();
  }
}

package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Interlinear;
import com.google.common.collect.ImmutableList;

public interface InterlinearFetcher {
  ImmutableList<Interlinear> fetchInterlinear(BibleBook book, int chapter);
}

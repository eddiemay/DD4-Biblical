package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.google.common.collect.ImmutableList;

public interface ScriptureReferenceProcessor {
  ImmutableList<VerseRange> computeVerseRanges(String scriptureStr);

  class VerseRange {
    private final BibleBook book;
    private final int chapter;
    private final int startVerse;
    private final int endVerse;

    public VerseRange(BibleBook book, int chapter, int startVerse, int endVerse) {
      this.book = book;
      this.chapter = chapter;
      this.startVerse = startVerse;
      this.endVerse = endVerse;
    }

    public BibleBook getBook() {
      return book;
    }

    public int getChapter() {
      return chapter;
    }

    public int getStartVerse() {
      return startVerse;
    }

    public int getEndVerse() {
      return endVerse;
    }

    @Override
    public String toString() {
      return String.format("%s %d:%d-%d", getBook(), getChapter(), getStartVerse(), getEndVerse());
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof VerseRange)) {
        return false;
      }
      VerseRange other = (VerseRange) obj;
      return book.equals(other.book) &&
          chapter == other.chapter && startVerse == other.startVerse && endVerse == other.endVerse;
    }
  }
}

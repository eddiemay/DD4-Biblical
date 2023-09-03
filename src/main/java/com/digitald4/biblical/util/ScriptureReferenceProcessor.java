package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.google.common.collect.ImmutableList;

public interface ScriptureReferenceProcessor {
  boolean matchesPattern(String reference);
  ImmutableList<VerseRange> computeVerseRanges(String reference);

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

  class LanguageRequest {
    private final String version;
    private final String language;
    private final boolean required;

    public LanguageRequest (String version, String language, boolean required) {
      this.version = version;
      this.language = language;
      this.required = required;
    }

    public String getVersion() {
      return version;
    }

    public String getLanguage() {
      return language;
    }

    public boolean isRequired() {
      return required;
    }
  }
}

package com.digitald4.biblical.util;

import static com.digitald4.common.exception.DD4StorageException.ErrorCode.BAD_REQUEST;
import static java.lang.Integer.parseInt;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.common.exception.DD4StorageException;
import com.google.common.collect.ImmutableList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.inject.Inject;

public class ScriptureReferenceProcessorSplitImpl implements ScriptureReferenceProcessor {
  private static final Pattern BASE_PATTERN = Pattern.compile("([\\w ]+)\\.? (\\d+)([:\\d,;\\-–]*)");
  private static final Pattern VERSE_RANGE_ACROSS_CHAPTERS = Pattern.compile("(\\d+):(\\d+)[-,–](\\d+):(\\d+)");
  private static final Pattern VERSE_RANGE_ACROSS_CHAPTERS_BASE = Pattern.compile("(\\d+)[-,–](\\d+):(\\d+)");
  private static final Pattern CHAPTER_CHANGE_VERSE_RANGE = Pattern.compile("(\\d+):(\\d+)[-,–](\\d+)");
  private static final Pattern CHAPTER_CHANGE_SINGLE_VERSE = Pattern.compile("(\\d+):(\\d+)");
  private static final Pattern VERSE_RANGE = Pattern.compile("(\\d+)[-,–](\\d+)");
  private static final Pattern FULL_CHAPTERS = Pattern.compile("[-,–](\\d+)");
  private static final Pattern SINGLE_VERSE = Pattern.compile("(\\d+)");

  private final BibleBookStore bibleBookStore;

  @Inject
  public ScriptureReferenceProcessorSplitImpl(BibleBookStore bibleBookStore) {
    this.bibleBookStore = bibleBookStore;
  }

  @Override
  public boolean matchesPattern(String reference) {
    return BASE_PATTERN.matcher(reference).find();
  }

  @Override
  public ImmutableList<VerseRange> computeVerseRanges(String reference) {
    Matcher matcher = BASE_PATTERN.matcher(reference);
    if (!matcher.find()) {
      throw new DD4StorageException("Unable to match scripture pattern: " + reference, BAD_REQUEST);
    }

    ImmutableList.Builder<VerseRange> verseRanges = ImmutableList.builder();
    do {
      BibleBook book = bibleBookStore.get(matcher.group(1).trim());
      int chapter = parseInt(matcher.group(2));
      String[] parts = matcher.group(3).trim().split("[,;]");

      if (parts.length == 0) {
        if (book.getChapterCount() == 1 && chapter > 1) {
          // If the book only has 1 chapter then the chapter is really the verse (i.e. Jude 3).
          verseRanges.add(new VerseRange(book, 1, chapter, chapter));
          chapter = 1;
        } else {
          verseRanges.add(new VerseRange(book, chapter, 1, 400));
        }
      }

      for (String part : parts) {
        part = part.trim();

        if (part.isEmpty()) {
          if (book.getChapterCount() == 1 && chapter > 1) {
            // If the book only has 1 chapter then the chapter is really the verse (i.e. Jude 3)
            verseRanges.add(new VerseRange(book, 1, chapter, chapter));
            chapter = 1;
          } else {
            verseRanges.add(new VerseRange(book, chapter, 1, 400));
          }
          continue;
        }

        Matcher subMatcher = VERSE_RANGE_ACROSS_CHAPTERS.matcher(part);
        if (subMatcher.find()) {
          chapter = parseInt(subMatcher.group(1));
          int endChapter = parseInt(subMatcher.group(3));
          verseRanges.add(new VerseRange(book, chapter, parseInt(subMatcher.group(2)), 400));
          IntStream.range(chapter + 1, endChapter)
              .forEach(chap -> verseRanges.add(new VerseRange(book, chap, 1, 400)));
          chapter = endChapter;
          verseRanges.add(new VerseRange(book, chapter, 1, parseInt(subMatcher.group(4))));
          continue;
        }

        subMatcher = VERSE_RANGE_ACROSS_CHAPTERS_BASE.matcher(part);
        if (subMatcher.find()) {
          int endChapter = parseInt(subMatcher.group(2));
          verseRanges.add(new VerseRange(book, chapter, parseInt(subMatcher.group(1)), 400));
          IntStream.range(chapter + 1, endChapter)
              .forEach(chap -> verseRanges.add(new VerseRange(book, chap, 1, 400)));
          chapter = endChapter;
          verseRanges.add(new VerseRange(book, chapter, 1, parseInt(subMatcher.group(3))));
          continue;
        }

        subMatcher = CHAPTER_CHANGE_VERSE_RANGE.matcher(part);
        if (subMatcher.find()) {
          chapter = parseInt(subMatcher.group(1));
          verseRanges.add(new VerseRange(book, chapter, parseInt(subMatcher.group(2)), parseInt(subMatcher.group(3))));
          continue;
        }

        subMatcher = CHAPTER_CHANGE_SINGLE_VERSE.matcher(part);
        if (subMatcher.find()) {
          chapter = parseInt(subMatcher.group(1));
          verseRanges.add(new VerseRange(book, chapter, parseInt(subMatcher.group(2)), parseInt(subMatcher.group(2))));
          continue;
        }

        subMatcher = VERSE_RANGE.matcher(part);
        if (subMatcher.find()) {
          verseRanges.add(new VerseRange(book, chapter, parseInt(subMatcher.group(1)), parseInt(subMatcher.group(2))));
          continue;
        }

        subMatcher = FULL_CHAPTERS.matcher(part);
        if (subMatcher.find()) {
          int endChapter = parseInt(subMatcher.group(1));
          if (book.getChapterCount() == 1) {
            // If the book only has 1 chapter then this is really a verse range.
            verseRanges.add(new VerseRange(book, 1, chapter, endChapter));
            chapter = 1;
          } else {
            IntStream.range(chapter, endChapter + 1)
                .forEach(chap -> verseRanges.add(new VerseRange(book, chap, 1, 400)));
            chapter = endChapter;
          }
          continue;
        }

        subMatcher = SINGLE_VERSE.matcher(part);
        if (subMatcher.find()) {
          verseRanges.add(new VerseRange(book, chapter, parseInt(subMatcher.group(1)), parseInt(subMatcher.group(1))));
          continue;
        }

        throw new DD4StorageException(
            String.format("Unable to match scripture pattern verse part: %s of: %s", part, reference), BAD_REQUEST);
      }
    } while (matcher.find());

    return verseRanges.build();
  }
}

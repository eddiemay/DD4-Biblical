package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class ScriptureReferenceProcessorSplitImplTest {
  private static final StaticDataDAO staticDataDAO = new StaticDataDAO();
  private static final BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
  private static final BibleBook GENESIS = bibleBookStore.get("Genesis");
  private static final BibleBook KINGS_2 = bibleBookStore.get("2 Kings");
  private static final BibleBook PSALMS = bibleBookStore.get("Psalms");
  private static final BibleBook JUDE = bibleBookStore.get("Jude");

  private final ScriptureReferenceProcessor scriptureStrProcessor =
      new ScriptureReferenceProcessorSplitImpl(bibleBookStore);

  @Test
  public void computeVerseRange() {
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 5));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5,7")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 5),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 7, 7));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5,7")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 5),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 7, 7));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-7")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 7));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-7,9-11")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 7),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 9, 11));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-7,9-11,14")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 7),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 9, 11),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 14, 14));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-7,24:9")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 7),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 9, 9));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-7,24:9,11-13")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 7),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 9, 9),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 11, 13));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-7,24:9-11")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 7),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 9, 11));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-7,24:9-11,13-15")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 7),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 9, 11),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 13, 15));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-24:19")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 1, 19));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-26:19")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 25, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 26, 1, 19));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-26:19,22")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 25, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 26, 1, 19),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 26, 22, 22));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-24:19,25:7-26:3")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 1, 19),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 25, 7, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 26, 1, 3));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:5-24:19,25:7-26:3,22")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 5, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 1, 19),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 25, 7, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 26, 1, 3),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 26, 22, 22));
    assertThat(scriptureStrProcessor.computeVerseRanges(" 2 Kings 23 ")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 1, 400));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23,25:2-5")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 25, 2, 5));
  }

  @Test
  public void computeVerseRange_fullChapters() {
    assertThat(scriptureStrProcessor.computeVerseRanges("Genesis 1-3")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(GENESIS, 1, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(GENESIS, 2, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(GENESIS, 3, 1, 400));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 24:1-3, Genesis 1-3")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 24, 1, 3),
        new ScriptureReferenceProcessor.VerseRange(GENESIS, 1, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(GENESIS, 2, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(GENESIS, 3, 1, 400));
  }

  @Test
  public void computeVerseRange_multiBooks() {
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23,Psalms 117,Psalms 119")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 117, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 119, 1, 400));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23:2-5, Psalms 117:1-2")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 2, 5),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 117, 1, 2));
  }

  @Test
  public void computeVerseRange_shortNames() {
    assertThat(scriptureStrProcessor.computeVerseRanges("2Ki 23:7,Psa 117:1-2,119:1-18,Gen 2:3")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 7, 7),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 117, 1, 2),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 119, 1, 18),
        new ScriptureReferenceProcessor.VerseRange(GENESIS, 2, 3, 3));
  }

  @Test
  public void computeVerseRange_singleChapterBooks() {
    assertThat(scriptureStrProcessor.computeVerseRanges("Jude 23")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(JUDE, 1, 23, 23));
    assertThat(scriptureStrProcessor.computeVerseRanges("Jude 23-25")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(JUDE, 1, 23, 25));
    // If we say Jude 1 we really want the entire chapter and not just the first verse.
    assertThat(scriptureStrProcessor.computeVerseRanges("Jude 1")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(JUDE, 1, 1, 400));
    assertThat(scriptureStrProcessor.computeVerseRanges("Jude 23,25")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(JUDE, 1, 23, 23),
        new ScriptureReferenceProcessor.VerseRange(JUDE, 1, 25, 25));
    assertThat(scriptureStrProcessor.computeVerseRanges("Jude 23-25,27")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(JUDE, 1, 23, 25),
        new ScriptureReferenceProcessor.VerseRange(JUDE, 1, 27, 27));
    assertThat(scriptureStrProcessor.computeVerseRanges("2 Kings 23, Jude 23")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 23, 1, 400),
        new ScriptureReferenceProcessor.VerseRange(JUDE, 1, 23, 23));
  }

  @Test
  public void computeVerseRange_numberedBookFollowing() {
    assertThat(scriptureStrProcessor.computeVerseRanges("Jude 23, 2 Kings 2:7")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(JUDE, 1, 23, 23),
        new ScriptureReferenceProcessor.VerseRange(KINGS_2, 2, 7, 7));
  }

  @Test
  public void computeVerseRange_periodAfterBook() {
    assertThat(scriptureStrProcessor.computeVerseRanges("Gen. 2:3, Psa. 45:7")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(GENESIS, 2, 3, 3),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 45, 7, 7));
  }

  @Test
  public void computeVerseRange_semicolorSeperator() {
    assertThat(scriptureStrProcessor.computeVerseRanges("Gen 2:3; Psa 45:7")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(GENESIS, 2, 3, 3),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 45, 7, 7));
    assertThat(scriptureStrProcessor.computeVerseRanges("Psalm 126:6;127:1-5;128:3")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 126, 6, 6),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 127, 1, 5),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 128, 3, 3));
  }

  @Test
  public void computeVerseRange_dashInsteadOfMinus() {
    assertThat(scriptureStrProcessor.computeVerseRanges("Psalm 126:6–10,127:1–5,128:3–5")).containsExactly(
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 126, 6, 10),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 127, 1, 5),
        new ScriptureReferenceProcessor.VerseRange(PSALMS, 128, 3, 5));
  }
}

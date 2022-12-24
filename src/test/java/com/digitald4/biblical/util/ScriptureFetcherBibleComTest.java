package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.server.APIConnector;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ScriptureFetcherBibleComTest {
  @Mock private final APIConnector apiConnector = mock(APIConnector.class);
  private ScriptureFetcher scriptureFetcher;

  @Before
  public void setup() {
    scriptureFetcher = new ScriptureFetcherBibleCom(apiConnector);
  }

  @Test
  public void fetch() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        "<html></body>" +
            "<div class=\"book bkPSA\">\n" +
            "<div class=\"chapter ch117\" data-usfm=\"PSA.117\"><div class=\"label\">117</div>" +
            "<div class=\"p\"><span class=\"verse v1\" data-usfm=\"PSA.117.1\"><span class=\"label\">1</span><span class=\"content\">Praise &#1497;&#1492;&#1493;&#1492;, all you nations! Extol Him, all you peoples!</span></span></div>" +
            "<div class=\"p\"><span class=\"verse v2\" data-usfm=\"PSA.117.2\"><span class=\"label\">2</span><span class=\"content\">For His loving-commitment is mighty over us, And the truth of &#1497;&#1492;&#1493;&#1492; is everlasting. Praise Yah!</span></span></div>\n" +
            "</div></div>" +
            "</body></html>");

    assertThat(scriptureFetcher.fetch("TS2009", BibleBook.Psalms, 117)).containsExactly(
        new Scripture().setVersion("TS2009").setBook("Psalms").setChapter(117).setVerse(1).setText(
            "Praise יהוה, all you nations! Extol Him, all you peoples!"),
        new Scripture().setVersion("TS2009").setBook("Psalms").setChapter(117).setVerse(2).setText(
            "For His loving-commitment is mighty over us, And the truth of יהוה is everlasting. Praise Yah!"));
  }

  @Test
  public void getChapterUrl() {
    assertThat(scriptureFetcher.getChapterUrl("TS2009", new ScriptureReferenceProcessor.VerseRange(BibleBook.Genesis, 2, 3, 3)))
        .isEqualTo("https://www.bible.com/bible/316/GEN.2.TS2009");
    assertThat(scriptureFetcher.getChapterUrl("TS2009", new ScriptureReferenceProcessor.VerseRange(BibleBook.Kings2, 4, 58, 58)))
        .isEqualTo("https://www.bible.com/bible/316/2KI.4.TS2009");
  }

  @Test
  public void getVerseUrl() {
    assertThat(scriptureFetcher.getVerseUrl(new Scripture().setVersion("TS2009").setBook("Genesis").setChapter(2).setVerse(3)))
        .isEqualTo("https://www.bible.com/bible/316/GEN.2.3.TS2009");
    assertThat(scriptureFetcher.getVerseUrl(new Scripture().setVersion("TS2009").setBook("2 Kings").setChapter(4).setVerse(58)))
        .isEqualTo("https://www.bible.com/bible/316/2KI.4.58.TS2009");
  }
}

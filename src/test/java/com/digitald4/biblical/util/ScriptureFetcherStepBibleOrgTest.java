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

public class ScriptureFetcherStepBibleOrgTest {
  @Mock private final APIConnector apiConnector = mock(APIConnector.class);
  private ScriptureFetcher scriptureFetcher;

  @Before
  public void setup() {
    scriptureFetcher = new ScriptureFetcherStepBibleOrg(apiConnector);
  }

  @Test
  public void fetch() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        "{\n" +
            "    \"time\": 0,\n" +
            "    \"signature\": \"reference=Ps.117|version=RSKJ-NONE-\",\n" +
            "    \"searchType\": \"PASSAGE\",\n" +
            "    \"masterVersion\": \"RSKJ\",\n" +
            "    \"extraVersions\": \"\",\n" +
            "    \"interlinearMode\": \"NONE\",\n" +
            "    \"searchTokens\": [\n" +
            "        {\n" +
            "            \"enhancedTokenInfo\": {\n" +
            "                \"initials\": \"RNKJV\",\n" +
            "                \"hasStrongs\": false,\n" +
            "                \"hasMorphology\": false,\n" +
            "                \"hasRedLetter\": false,\n" +
            "                \"hasNotes\": false,\n" +
            "                \"hasHeadings\": false,\n" +
            "                \"questionable\": false,\n" +
            "                \"shortInitials\": \"RSKJ\",\n" +
            "                \"hasSeptuagintTagging\": false\n" +
            "            },\n" +
            "            \"token\": \"RSKJ\",\n" +
            "            \"tokenType\": \"version\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"enhancedTokenInfo\": {\n" +
            "                \"sectionType\": \"PASSAGE\",\n" +
            "                \"shortName\": \"Psa 117\",\n" +
            "                \"fullName\": \"Psa 117\",\n" +
            "                \"wholeBook\": false,\n" +
            "                \"osisID\": \"Ps.117\",\n" +
            "                \"passage\": false\n" +
            "            },\n" +
            "            \"token\": \"Ps.117\",\n" +
            "            \"tokenType\": \"reference\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"timeTookTotal\": 14,\n" +
            "    \"previousChapter\": {\n" +
            "        \"osisKeyId\": \"Ps.116\",\n" +
            "        \"name\": \"Psa 116\",\n" +
            "        \"lastChapter\": false\n" +
            "    },\n" +
            "    \"nextChapter\": {\n" +
            "        \"osisKeyId\": \"Ps.118\",\n" +
            "        \"name\": \"Psa 118\",\n" +
            "        \"lastChapter\": false\n" +
            "    },\n" +
            "    \"value\": \"<div tabindex='-1' class='passageContentHolder'><h2 class='xgen'>Psalms 117</h2> <span dir='ltr' class='verse ltrDirection'><a name='Ps.117.1' class='verseLink'><span class='verseNumber'>1</span></a>O praise <span class='small-caps'> יהוה</span>, all ye nations: praise him, all ye people. </span> <span dir='ltr' class='verse ltrDirection'><a name='Ps.117.2' class='verseLink'><span class='verseNumber'>2</span></a>For his merciful kindness is great toward us: and the truth of <span class='small-caps'> יהוה</span> endureth for ever. Praise ye <span class='small-caps'> יהוה</span>.  </span> </div>\",\n" +
            "    \"reference\": \"Psa 117\",\n" +
            "    \"osisId\": \"Ps.117\",\n" +
            "    \"fragment\": false,\n" +
            "    \"multipleRanges\": false,\n" +
            "    \"startRange\": 16484,\n" +
            "    \"endRange\": 16485,\n" +
            "    \"languageCode\": [\n" +
            "        \"en\"\n" +
            "    ],\n" +
            "    \"longName\": \"Psalms 117\",\n" +
            "    \"options\": \"VL\",\n" +
            "    \"selectedOptions\": \"VNHUG\",\n" +
            "    \"removedOptions\": [\n" +
            "        {\n" +
            "            \"explanation\": \"This option is not available in the currently selected text.\",\n" +
            "            \"option\": \"N\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"explanation\": \"This option is not available in the currently selected text.\",\n" +
            "            \"option\": \"H\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"explanation\": \"This option is not available in the currently selected text.\",\n" +
            "            \"option\": \"U\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"explanation\": \"This option is not available in the currently selected text.\",\n" +
            "            \"option\": \"G\"\n" +
            "        }\n" +
            "    ]\n" +
            "}");

    assertThat(scriptureFetcher.fetch("RSKJ", BibleBook.Psalms, 117)).containsExactly(
        new Scripture().setVersion("RSKJ").setBook("Psalms").setChapter(117).setVerse(1).setText(
            "O praise יהוה, all ye nations: praise him, all ye people."),
        new Scripture().setVersion("RSKJ").setBook("Psalms").setChapter(117).setVerse(2).setText(
            "For his merciful kindness is great toward us: and the truth of יהוה endureth for ever. Praise ye יהוה."));
  }

  @Test
  public void getChapterUrl() {
    assertThat(scriptureFetcher.getChapterUrl("RSKJ", new ScriptureReferenceProcessor.VerseRange(BibleBook.Genesis, 2, 3, 3)))
        .isEqualTo("https://www.stepbible.org/?q=reference=Genesis.2|version=RSKJ&options=VNHUG");
    assertThat(scriptureFetcher.getChapterUrl("RSKJ", new ScriptureReferenceProcessor.VerseRange(BibleBook.Kings2, 4, 58, 58)))
        .isEqualTo("https://www.stepbible.org/?q=reference=2Kings.4|version=RSKJ&options=VNHUG");
  }

  @Test
  public void getVerseUrl() {
    assertThat(scriptureFetcher.getVerseUrl(new Scripture().setVersion("RSKJ").setBook("Genesis").setChapter(2).setVerse(3)))
        .isEqualTo("https://www.stepbible.org/?q=reference=Genesis.2.3|version=RSKJ&options=VNHUG");
    assertThat(scriptureFetcher.getVerseUrl(new Scripture().setVersion("RSKJ").setBook("2 Kings").setChapter(4).setVerse(36)))
        .isEqualTo("https://www.stepbible.org/?q=reference=2Kings.4.36|version=RSKJ&options=VNHUG");
  }

  @Test
  public void getUrls_singleChapterBook() {
    assertThat(scriptureFetcher.getChapterUrl("RSKJ", new ScriptureReferenceProcessor.VerseRange(BibleBook.Jude, 1, 3, 3)))
        .isEqualTo("https://www.stepbible.org/?q=reference=Jude|version=RSKJ&options=VNHUG");
    assertThat(scriptureFetcher.getVerseUrl(new Scripture().setVersion("RSKJ").setBook("Jude").setChapter(1).setVerse(3)))
        .isEqualTo("https://www.stepbible.org/?q=reference=Jude.3|version=RSKJ&options=VNHUG");
  }
}

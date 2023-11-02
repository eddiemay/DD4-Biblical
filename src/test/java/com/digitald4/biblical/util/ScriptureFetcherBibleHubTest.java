package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import org.junit.Before;
import org.junit.Test;

public class ScriptureFetcherBibleHubTest extends ScriptureFetcherTest {
  @Before
  public void setup() {
    super.setup();
  }

  @Test
  public void fetch() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        "<html></body>" +
            "<div id=\"topheading\"><a href=\"../psalms/116.htm\" title=\"Psalm 116\">◄</a> " +
            "Psalm 117 <a href=\"../psalms/118.htm\" title=\"Psalm 118\">►</a></div>" +
            "<div class=\"chap\">" +
            "<p class=\"sc\"><span class=\"reftext\"><a href=\"//biblehub.com/psalms/117-1.htm\"><b>1</b></a></span>" +
            "Praise יהוה, all you nations! Extol Him, all you <i>peoples</i>!</p>" +
            "<p class=\"sc\"><span class=\"reftext\"><a href=\"//biblehub.com/psalms/117-2.htm\"><b>2</b></a></span>" +
            "For His kindness is mighty over us, And the truth of יהוה is everlasting. Praise Yah!</p>" +
            "</div></body></html>");

    assertThat(scriptureStore.getScriptures("ISR", Language.EN, "Psalms 117").getItems()).containsExactly(
        new Scripture().setVersion("ISR").setBook("Psalms").setChapter(117).setVerse(1).setText(
            "Praise יהוה, all you nations! Extol Him, all you peoples!"),
        new Scripture().setVersion("ISR").setBook("Psalms").setChapter(117).setVerse(2).setText(
            "For His kindness is mighty over us, And the truth of יהוה is everlasting. Praise Yah!"));
  }

  @Test
  public void fetchWLCO() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/wlco.html"));

    assertThat(scriptureStore.getScriptures("WLCO", Language.HEBREW, "Psalms 117").getItems()).containsExactly(
        new Scripture().setVersion("WLCO").setLanguage("he").setBook("Psalms").setChapter(117).setVerse(1)
            .setText("הללו את־יהוה כל־גוים בחוהו כל־האמים׃"),
        new Scripture().setVersion("WLCO").setLanguage("he").setBook("Psalms").setChapter(117).setVerse(2)
            .setText("כי גבר עלינו ׀ חסדו ואמת־יהוה לעולם הללו־יה׃"));
  }

  @Test
  public void fetchGreek() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_nestle_matthew_1.htm"));

    assertThat(scriptureStore.getScriptures("Nestle", Language.GREEK, "Matt 1:1-5").getItems()).containsExactly(
        new Scripture().setVersion("Nestle").setLanguage("gk").setBook("Matthew").setChapter(1).setVerse(1)
            .setText("Βίβλος γενέσεως Ἰησοῦ Χριστοῦ υἱοῦ Δαυεὶδ υἱοῦ Ἀβραάμ."),
        new Scripture().setVersion("Nestle").setLanguage("gk").setBook("Matthew").setChapter(1).setVerse(2)
            .setText("Ἀβραὰμ ἐγέννησεν τὸν Ἰσαάκ, Ἰσαὰκ δὲ ἐγέννησεν τὸν Ἰακώβ, Ἰακὼβ δὲ ἐγέννησεν τὸν Ἰούδαν καὶ τοὺς ἀδελφοὺς αὐτοῦ,"),
        new Scripture().setVersion("Nestle").setLanguage("gk").setBook("Matthew").setChapter(1).setVerse(3)
            .setText("Ἰούδας δὲ ἐγέννησεν τὸν Φαρὲς καὶ τὸν Ζαρὰ ἐκ τῆς Θάμαρ, Φαρὲς δὲ ἐγέννησεν τὸν Ἐσρώμ, Ἐσρὼμ δὲ ἐγέννησεν τὸν Ἀράμ,"),
        new Scripture().setVersion("Nestle").setLanguage("gk").setBook("Matthew").setChapter(1).setVerse(4)
            .setText("Ἀρὰμ δὲ ἐγέννησεν τὸν Ἀμιναδάβ, Ἀμιναδὰβ δὲ ἐγέννησεν τὸν Ναασσών, Ναασσὼν δὲ ἐγέννησεν τὸν Σαλμών,"),
        new Scripture().setVersion("Nestle").setLanguage("gk").setBook("Matthew").setChapter(1).setVerse(5)
            .setText("Σαλμὼν δὲ ἐγέννησεν τὸν Βόες ἐκ τῆς Ῥαχάβ, Βόες δὲ ἐγέννησεν τὸν Ἰωβὴδ ἐκ τῆς Ῥούθ, Ἰωβὴδ δὲ ἐγέννησεν τὸν Ἰεσσαί,"));
  }
}

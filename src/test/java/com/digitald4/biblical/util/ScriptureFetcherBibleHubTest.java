package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import org.junit.Before;
import org.junit.Ignore;
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

  @Test @Ignore
  public void fetchNKJV() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        "<html></body>" +
            "<div class=\"chap\"> <h3>Let All Peoples Praise the <span style=\"font-variant: small-caps\" class=\"small-caps divine-name\">Lord</span></h3>" +
            "<div class=\"poetry\"><p class=\"line\"><span class=\"chapter-3\">" +
            "<span class=\"text Ps-117-1\"><span class=\"reftext\"><a href=\"http://biblehub.com/psalms/117-1.htm\"><b>1</b></a></span>Praise the <span style=\"font-variant: small-caps\" class=\"small-caps divine-name\">Lord</span>, all you Gentiles!</span>" +
            "</span><br><span class=\"text Ps-117-1\"><span class=\"fn\"><a href=\"#footnotes\">[a]</a></span>Laud Him, all you peoples!</span><br>\n" +
            "<span class=\"reftext\"><a href=\"http://biblehub.com/psalms/117-2.htm\"><b>2</b></a></span>For His merciful kindness is great toward us,<br><span class=\"text Ps-117-2\">And the truth of the <span style=\"font-variant: small-caps\" class=\"small-caps divine-name\">Lord</span> <i>endures</i> forever.</span></p></div>" +
            "<div class=\"poetry_top-1\"><p class=\"line\"><span class=\"text Ps-117-2\">Praise the <span style=\"font-variant: small-caps\" class=\"small-caps divine-name\">Lord</span>!</span></p></div>" +
            "<a name=\"footnotes\"></a><div class=\"footnotes\"><h4>Footnotes:</h4><ol><li id=\"fen-NKJV-15869a\"><span class=\"fnref\" title=\"Go to Psalm 117:1\">Psalm 117:1</span> <span class=\"footnote-text\"><i>Praise</i></span></li></ol></div> <!--end of footnotes--></div>" +
            "</body></html>");

    assertThat(scriptureStore.getScriptures("NKJV", Language.EN, "Psalms 117").getItems()).containsExactly(
        new Scripture().setVersion("NKJV").setBook("Psalms").setChapter(117).setVerse(1).setText(
            "Praise the Lord, all you Gentiles! Laud Him, all you peoples!"),
        new Scripture().setVersion("NKJV").setBook("Psalms").setChapter(117).setVerse(2).setText(
            "For His merciful kindness is great toward us, And the truth of the Lord endures forever. Praise the Lord!"));
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
}

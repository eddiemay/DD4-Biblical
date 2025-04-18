package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import org.junit.Test;

public class ScriptureFetcherJWOrgTest extends ScriptureFetcherTest {
  @Test
  public void fetch() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        "<html><body><div id=\"bibleText\" class=\"textSizeIncrement\" rv-html=\"state.chapter.html\"><span class=\"verse disableActiveState jsHasHighlightListener jsHasMediaMarker\" id=\"v19117001\"><span class=\"style-l\"><span class=\"chapterNum\"><a data-anchor=\"#v19117001\" class=\"jsHighlightOnly\">117 </a></span>&nbsp;Praise Jehovah, all you nations;<a class=\"xrefLink jsNoScroll\" id=\"xreflink165948323\" href=\"#xref\" data-bible=\"nwtsty\" data-targetverses=\"66007009-66007010\" data-target=\"165948323\">a</a></span><span class=\"newblock\"></span><span class=\"style-z\">Glorify him, all you peoples.<a class=\"footnoteLink jsNoDialog jsNoScroll\" id=\"footnotesource165948354\" href=\"#footnote\" data-anchor=\"#fn165948354\">*</a><a class=\"xrefLink jsNoScroll\" id=\"xreflink165948337\" href=\"#xref\" data-bible=\"nwtsty\" data-targetverses=\"45015011\" data-target=\"165948337\">b</a></span>\n" +
            "<span class=\"parabreak\"></span></span>\n" +
            "\n" +
            "<span class=\"verse disableActiveState jsHasHighlightListener jsHasMediaMarker\" id=\"v19117002\"><span class=\"style-l\">&nbsp;<sup class=\"verseNum\"><a data-anchor=\"#v19117002\" class=\"jsHighlightOnly\">2&nbsp;</a></sup>&nbsp;For his loyal love toward us is great;<a class=\"xrefLink jsNoScroll\" id=\"xreflink165948368\" href=\"#xref\" data-bible=\"nwtsty\" data-targetverses=\"25003022\" data-target=\"165948368\">c</a></span><span class=\"newblock\"></span><span class=\"style-z\">The faithfulness<a class=\"xrefLink jsNoScroll\" id=\"xreflink165948385\" href=\"#xref\" data-bible=\"nwtsty\" data-targetverses=\"19025010,19091004\" data-target=\"165948385\">d</a> of Jehovah endures forever.<a class=\"xrefLink jsNoScroll\" id=\"xreflink165948401\" href=\"#xref\" data-bible=\"nwtsty\" data-targetverses=\"19100005\" data-target=\"165948401\">e</a></span>\n" +
            "<span class=\"parabreak\"></span><span class=\"style-z\">Praise Jah!<a class=\"footnoteLink jsNoDialog jsNoScroll\" id=\"footnotesource165948436\" href=\"#footnote\" data-anchor=\"#fn165948436\">*</a><a class=\"xrefLink jsNoScroll\" id=\"xreflink165948418\" href=\"#xref\" data-bible=\"nwtsty\" data-targetverses=\"19111001\" data-target=\"165948418\">f</a></span><span class=\"parabreak\"></span></span>\n" +
            "\n" +
            "</div></body></html>");

    assertThat(scriptureStore.getScriptures("NWT", Language.EN, "Psalms 117").getItems()).containsExactly(
        new Scripture().setVersion("NWT").setBook("Psalms").setChapter(117).setVerse(1)
            .setText("Praise Jehovah, all you nations; Glorify him, all you peoples."),
        new Scripture().setVersion("NWT").setBook("Psalms").setChapter(117).setVerse(2)
            .setText("For his loyal love toward us is great; The faithfulness of Jehovah endures forever. Praise Jah!"));
  }
}

package com.digitald4.biblical.util;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.server.APIConnector;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScriptureFetcherBookOfEnochReferencesTest {
  @Mock private final APIConnector apiConnector = mock(APIConnector.class);
  private ScriptureFetcher scriptureFetcher;

  @Before
  public void setup() {
    scriptureFetcher = new ScriptureFetcherBookOfEnochReferences(apiConnector);
  }

  @Test
  public void fetch() {
    when(apiConnector.sendGet(anyString())).thenReturn("<html><body><div class=\"entry-content clear\">\n" +
        "\t\t<p>CHAPTER 72<br>\n" +
        "(R.H. Charles Oxford: The Clarendon Press)</p>\n" +
        "<p>1. The book of the courses of the luminaries of the heaven, the relations of each, according to their classes, their dominion and their seasons, according to their names and places of origin, and according to their months, which Uriel, the holy angel, who was with me, who is their guide, showed me; and he showed me all their laws exactly as they are, and how it is with regard to all the years of the world and unto eternity, till the new creation is accomplished which dureth till eternity. </p>\n" +
        "<p>2. And this is the first law of the luminaries: the luminary the Sun has its rising in the eastern portals of the heaven, and its setting in the western portals of the heaven.<br>\n" +
        "(Psalm 104:19), (Ecclesiastes 1:5), (Ben Sira 43:1-5) </p>\n" +
        "<p>3. And I saw six portals in which the sun rises, and six portals in which the sun sets and the moon rises and sets in these portals, and the leaders of the stars and those whom they lead: six in the east and six in the west, and all following each other in accurately corresponding order: also many windows to the right and left of these portals. </p>\n" +
        "<p>4. And first there goes forth the great luminary, named the Sun, and his circumference is like the circumference of the heaven, and he is quite filled with illuminating and heating fire.</p>\n" +
        "<p>5. The chariot on which he ascends, the wind drives, and the sun goes down from the heaven and returns through the north in order to reach the east, and is so guided that he comes to the appropriate (lit. ‘ that ‘) portal and shines in the face of the heaven.</p>\n" +
        "<p>6. In this way he rises in the first month in the great portal, which is the fourth [those six portals in the cast]. </p>" +
        "<p>&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;<br>\n" +
        "&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;&lt;</p>\n" +
        "<p>2.<br>\n" +
        "(Psalm 104:19) “19 He appointed the moon for seasons; The sun knows its going down.”</p>\n" +
        "<p>(Ecclesiastes 1:5) “5 The sun also rises, and the sun goes down, And hastens to the place where it arose.”</p>\n" +
        "<p>(Ben Sira 43:1-5) “1 The pride of the higher realms is the clear vault of the sky, as glorious to behold as the sight of the heavens. 2 The sun, when it appears, proclaims as it rises what a marvelous instrument it is, the work of the Most High. 3 At noon it parches the land, and who can withstand its burning heat? 4 A man tending a furnace works in burning heat, but three times as hot is the sun scorching the mountains; it breathes out fiery vapors, and its bright rays blind the eyes. 5 Great is the Lord who made it; at his orders it hurries on its course.”</p>\n" +
        "\t\t\t<div id=\"atatags-26942-61be87d957374\"></div>\n" +
        "\t\t\t\n" +
        "\t\t\t<script>\n" +
        "\t\t\t\t__ATA.cmd.push(function() {\n" +
        "\t\t\t\t\t__ATA.initDynamicSlot({\n" +
        "\t\t\t\t\t\tid: 'atatags-26942-61be87d957374',\n" +
        "\t\t\t\t\t\tlocation: 120,\n" +
        "\t\t\t\t\t\tformFactor: '001',\n" +
        "\t\t\t\t\t\tlabel: {\n" +
        "\t\t\t\t\t\t\ttext: 'Advertisements',\n" +
        "\t\t\t\t\t\t},\n" +
        "\t\t\t\t\t\tcreative: {\n" +
        "\t\t\t\t\t\t\treportAd: {\n" +
        "\t\t\t\t\t\t\t\ttext: 'Report this ad',\n" +
        "\t\t\t\t\t\t\t},\n" +
        "\t\t\t\t\t\t\tprivacySettings: {\n" +
        "\t\t\t\t\t\t\t\ttext: 'Privacy',\n" +
        "\t\t\t\t\t\t\t\t\n" +
        "\t\t\t\t\t\t\t}\n" +
        "\t\t\t\t\t\t}\n" +
        "\t\t\t\t\t});\n" +
        "\t\t\t\t});\n" +
        "\t\t\t</script>\t\t\t\t\t</div></body</html>");

    assertThat(scriptureFetcher.fetch("OXFORD", BibleBook.ENOCH, 72)).containsExactly(
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(72).setVerse(1).setText(
            "The book of the courses of the luminaries of the heaven, the relations of each, according to their classes, their dominion and their seasons, according to their names and places of origin, and according to their months, which Uriel, the holy angel, who was with me, who is their guide, showed me; and he showed me all their laws exactly as they are, and how it is with regard to all the years of the world and unto eternity, till the new creation is accomplished which dureth till eternity."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(72).setVerse(2).setText(
            "And this is the first law of the luminaries: the luminary the Sun has its rising in the eastern portals of the heaven, and its setting in the western portals of the heaven."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(72).setVerse(3).setText(
            "And I saw six portals in which the sun rises, and six portals in which the sun sets and the moon rises and sets in these portals, and the leaders of the stars and those whom they lead: six in the east and six in the west, and all following each other in accurately corresponding order: also many windows to the right and left of these portals."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(72).setVerse(4).setText(
            "And first there goes forth the great luminary, named the Sun, and his circumference is like the circumference of the heaven, and he is quite filled with illuminating and heating fire."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(72).setVerse(5).setText(
            "The chariot on which he ascends, the wind drives, and the sun goes down from the heaven and returns through the north in order to reach the east, and is so guided that he comes to the appropriate (lit. ‘ that ‘) portal and shines in the face of the heaven."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(72).setVerse(6).setText(
            "In this way he rises in the first month in the great portal, which is the fourth [those six portals in the cast]."));
  }

  @Test
  public void getChapterUrl() {
    assertThat(scriptureFetcher.getChapterUrl("OXFORD", new ScriptureReferenceProcessor.VerseRange(BibleBook.ENOCH, 72, 3, 3)))
        .isEqualTo("https://bookofenochreferences.wordpress.com/category/the-book-of-enoch-with-biblical-references-chapters-71-to-80/chapter-72/");
    assertThat(scriptureFetcher.getChapterUrl("OXFORD", new ScriptureReferenceProcessor.VerseRange(BibleBook.ENOCH, 4, 58, 58)))
        .isEqualTo("https://bookofenochreferences.wordpress.com/category/the-book-of-enoch-with-biblical-references-chapters-1-to-10/chapter-4/");
  }

  @Test
  public void getVerseUrl() {
    assertThat(scriptureFetcher.getVerseUrl(new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(72).setVerse(3)))
        .isEqualTo("https://bookofenochreferences.wordpress.com/category/the-book-of-enoch-with-biblical-references-chapters-71-to-80/chapter-72/");
    assertThat(scriptureFetcher.getVerseUrl(new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(4).setVerse(58)))
        .isEqualTo("https://bookofenochreferences.wordpress.com/category/the-book-of-enoch-with-biblical-references-chapters-1-to-10/chapter-4/");
  }
}

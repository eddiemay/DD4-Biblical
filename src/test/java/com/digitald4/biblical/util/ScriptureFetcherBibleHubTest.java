package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ScriptureFetcherBibleHubTest extends ScriptureFetcherTest {
  private ScriptureFetcherBibleHub scriptureFetcher;

  @Before
  public void setup() {
    super.setup();
    scriptureFetcher = new ScriptureFetcherBibleHub(apiConnector);
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

    assertThat(scriptureStore.getScriptures("ISR", BibleBook.EN, "Psalms 117").getItems()).containsExactly(
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

    assertThat(scriptureStore.getScriptures("NKJV", BibleBook.EN, "Psalms 117").getItems()).containsExactly(
        new Scripture().setVersion("NKJV").setBook("Psalms").setChapter(117).setVerse(1).setText(
            "Praise the Lord, all you Gentiles! Laud Him, all you peoples!"),
        new Scripture().setVersion("NKJV").setBook("Psalms").setChapter(117).setVerse(2).setText(
            "For His merciful kindness is great toward us, And the truth of the Lord endures forever. Praise the Lord!"));
  }

  @Test
  public void fetchKJV() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        "<html></body>" +
            "<div class=\"chap\"><a name=\"1\" id=\"1\"></a><p class=\"hdg\">The Beginning<br></p><p class=\"cross\">(<a href=\"../john/1.htm\">John 1:1-5</a>)</p><p></p>" +
            "<p class=\"reg\"><span class=\"reftext\"><a href=\"/genesis/1-1.htm\"><b>1</b></a></span>In the beginning God created the heaven and the earth. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-2.htm\"><b>2</b></a></span>And the earth was without form, and void; and darkness <i>was</i> upon the face of the deep. And the Spirit of God moved upon the face of the waters.</p>\n" +
            "<p class=\"hdg\">The First Day: Light</p>" +
            "<p class=\"reg\"><span class=\"reftext\"><a href=\"/genesis/1-3.htm\"><b>3</b></a></span>And God said, Let there be light: and there was light. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-4.htm\"><b>4</b></a></span>And God saw the light, that <i>it was</i> good: and God divided the light from the darkness. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-5.htm\"><b>5</b></a></span>And God called the light Day, and the darkness he called Night. And the evening and the morning were the first day.</p>\n" +
            "<p class=\"hdg\">The Second Day: Firmament</p><p class=\"reg\"><span class=\"reftext\"><a href=\"/genesis/1-6.htm\"><b>6</b></a></span>And God said, Let there be a firmament in the midst of the waters, and let it divide the waters from the waters. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-7.htm\"><b>7</b></a></span>And God made the firmament, and divided the waters which <i>were</i> under the firmament from the waters which <i>were</i> above the firmament: and it was so. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-8.htm\"><b>8</b></a></span>And God called the firmament Heaven. And the evening and the morning were the second day.</p>\n" +
            "<p class=\"hdg\">The Third Day: Dry Ground</p><p class=\"reg\"><span class=\"reftext\"><a href=\"/genesis/1-9.htm\"><b>9</b></a></span>And God said, Let the waters under the heaven be gathered together unto one place, and let the dry <i>land</i> appear: and it was so. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-10.htm\"><b>10</b></a></span>And God called the dry <i>land</i> Earth; and the gathering together of the waters called he Seas: and God saw that <i>it was</i> good. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-11.htm\"><b>11</b></a></span>And God said, Let the earth bring forth grass, the herb yielding seed, <i>and</i> the fruit tree yielding fruit after his kind, whose seed <i>is</i> in itself, upon the earth: and it was so. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-12.htm\"><b>12</b></a></span>And the earth brought forth grass, <i>and</i> herb yielding seed after his kind, and the tree yielding fruit, whose seed <i>was</i> in itself, after his kind: and God saw that <i>it was</i> good. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-13.htm\"><b>13</b></a></span>And the evening and the morning were the third day.</p>\n" +
            "<p class=\"hdg\">The Fourth Day: Sun, Moon, Stars</p><p class=\"reg\"><span class=\"reftext\"><a href=\"/genesis/1-14.htm\"><b>14</b></a></span>And God said, Let there be lights in the firmament of the heaven to divide the day from the night; and let them be for signs, and for seasons, and for days, and years: \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-15.htm\"><b>15</b></a></span>And let them be for lights in the firmament of the heaven to give light upon the earth: and it was so. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-16.htm\"><b>16</b></a></span>And God made two great lights; the greater light to rule the day, and the lesser light to rule the night: <i>he made</i> the stars also. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-17.htm\"><b>17</b></a></span>And God set them in the firmament of the heaven to give light upon the earth, \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-18.htm\"><b>18</b></a></span>And to rule over the day and over the night, and to divide the light from the darkness: and God saw that <i>it was</i> good. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-19.htm\"><b>19</b></a></span>And the evening and the morning were the fourth day.</p>\n" +
            "<p class=\"hdg\">The Fifth Day: Fish and Birds</p><p class=\"reg\"><span class=\"reftext\"><a href=\"/genesis/1-20.htm\"><b>20</b></a></span>And God said, Let the waters bring forth abundantly the moving creature that hath life, and fowl <i>that</i> may fly above the earth in the open firmament of heaven. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-21.htm\"><b>21</b></a></span>And God created great whales, and every living creature that moveth, which the waters brought forth abundantly, after their kind, and every winged fowl after his kind: and God saw that <i>it was</i> good. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-22.htm\"><b>22</b></a></span>And God blessed them, saying, Be fruitful, and multiply, and fill the waters in the seas, and let fowl multiply in the earth. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-23.htm\"><b>23</b></a></span>And the evening and the morning were the fifth day.</p>\n" +
            "<p class=\"hdg\">The Sixth Day: Creatures on Land</p><p class=\"reg\"><span class=\"reftext\"><a href=\"/genesis/1-24.htm\"><b>24</b></a></span>And God said, Let the earth bring forth the living creature after his kind, cattle, and creeping thing, and beast of the earth after his kind: and it was so. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-25.htm\"><b>25</b></a></span>And God made the beast of the earth after his kind, and cattle after their kind, and every thing that creepeth upon the earth after his kind: and God saw that <i>it was</i> good.</p>\n" +
            "<p class=\"reg\"><span class=\"reftext\"><a href=\"/genesis/1-26.htm\"><b>26</b></a></span>And God said, Let us make man in our image, after our likeness: and let them have dominion over the fish of the sea, and over the fowl of the air, and over the cattle, and over all the earth, and over every creeping thing that creepeth upon the earth. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-27.htm\"><b>27</b></a></span>So God created man in his <i>own</i> image, in the image of God created he him; male and female created he them. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-28.htm\"><b>28</b></a></span>And God blessed them, and God said unto them, Be fruitful, and multiply, and replenish the earth, and subdue it: and have dominion over the fish of the sea, and over the fowl of the air, and over every living thing that moveth upon the earth. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-29.htm\"><b>29</b></a></span>And God said, Behold, I have given you every herb bearing seed, which <i>is</i> upon the face of all the earth, and every tree, in the which <i>is</i> the fruit of a tree yielding seed; to you it shall be for meat. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-30.htm\"><b>30</b></a></span>And to every beast of the earth, and to every fowl of the air, and to every thing that creepeth upon the earth, wherein <i>there is</i> life, <i>I have given</i> every green herb for meat: and it was so. \n" +
            "<span class=\"reftext\"><a href=\"/genesis/1-31.htm\"><b>31</b></a></span>And God saw every thing that he had made, and, behold, <i>it was</i> very good. And the evening and the morning were the sixth day.</p></div>" +
            "</body></html>");

    ImmutableList<Scripture> fetched = scriptureFetcher.fetch("KJV", BibleBook.Genesis, 1);

    assertThat(fetched).hasSize(31);
    assertThat(fetched.get(0).getText().toString()).isEqualTo("In the beginning God created the heaven and the earth.");
    assertThat(fetched.get(3).getText().toString()).isEqualTo(
        "And God saw the light, that it was good: and God divided the light from the darkness.");
    assertThat(fetched.get(4).getText().toString()).isEqualTo(
        "And God called the light Day, and the darkness he called Night. And the evening and the morning were the first day.");
  }

  @Test
  public void fetchWLCO() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/wlco.html"));

    assertThat(scriptureStore.getScriptures("WLCO", BibleBook.EN, "Psalms 117").getItems()).containsExactly(
        new Scripture().setVersion("WLCO").setLocale("he").setBook("Psalms").setChapter(117).setVerse(1)
            .setText("הללו את־יהוה כל־גוים בחוהו כל־האמים׃"),
        new Scripture().setVersion("WLCO").setLocale("he").setBook("Psalms").setChapter(117).setVerse(2)
            .setText("כי גבר עלינו ׀ חסדו ואמת־יהוה לעולם הללו־יה׃"));
  }
}

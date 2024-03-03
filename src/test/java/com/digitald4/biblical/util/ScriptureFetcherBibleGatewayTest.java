package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.Language.EN;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import org.junit.Ignore;
import org.junit.Test;

public class ScriptureFetcherBibleGatewayTest extends ScriptureFetcherTest {

  @Test
  public void fetch() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        """
            <html></body><div class="passage-text">
            <div class="passage-content passage-class-0"><div class="version-NRSV result-text-style-normal text-html">
            <p class="chapter-1"><span id="en-NRSV-32" class="text Gen-2-1"><span class="chapternum">2&nbsp;</span>Thus the heavens and the earth were finished, and all their multitude. </span><span id="en-NRSV-33" class="text Gen-2-2"><sup class="versenum">2&nbsp;</sup>And on the seventh day God finished the work that he had done, and he rested on the seventh day from all the work that he had done. </span><span id="en-NRSV-34" class="text Gen-2-3"><sup class="versenum">3&nbsp;</sup>So God blessed the seventh day and hallowed it, because on it God rested from all the work that he had done in creation.</span></p><p><span id="en-NRSV-35" class="text Gen-2-4"><sup class="versenum">4&nbsp;</sup>These are the generations of the heavens and the earth when they were created.</span></p><h3><span class="text Gen-2-4">Another Account of the Creation</span></h3><p><span class="text Gen-2-4">In the day that the <span style="font-variant: small-caps" class="small-caps">Lord</span><sup data-fn="#fen-NRSV-35a" class="footnote" data-link="[<a href=&quot;#fen-NRSV-35a&quot; title=&quot;See footnote a&quot;>a</a>]">[<a href="#fen-NRSV-35a" title="See footnote a">a</a>]</sup> God made the earth and the heavens, </span><span id="en-NRSV-36" class="text Gen-2-5"><sup class="versenum">5&nbsp;</sup>when no plant of the field was yet in the earth and no herb of the field had yet sprung up—for the <span style="font-variant: small-caps" class="small-caps">Lord</span> God had not caused it to rain upon the earth, and there was no one to till the ground; </span><span id="en-NRSV-37" class="text Gen-2-6"><sup class="versenum">6&nbsp;</sup>but a stream would rise from the earth, and water the whole face of the ground— </span><span id="en-NRSV-38" class="text Gen-2-7"><sup class="versenum">7&nbsp;</sup>then the <span style="font-variant: small-caps" class="small-caps">Lord</span> God formed man from the dust of the ground,<sup data-fn="#fen-NRSV-38b" class="footnote" data-link="[<a href=&quot;#fen-NRSV-38b&quot; title=&quot;See footnote b&quot;>b</a>]">[<a href="#fen-NRSV-38b" title="See footnote b">b</a>]</sup> and breathed into his nostrils the breath of life; and the man became a living being. </span><span id="en-NRSV-39" class="text Gen-2-8"><sup class="versenum">8&nbsp;</sup>And the <span style="font-variant: small-caps" class="small-caps">Lord</span> God planted a garden in Eden, in the east; and there he put the man whom he had formed. </span><span id="en-NRSV-40" class="text Gen-2-9"><sup class="versenum">9&nbsp;</sup>Out of the ground the <span style="font-variant: small-caps" class="small-caps">Lord</span> God made to grow every tree that is pleasant to the sight and good for food, the tree of life also in the midst of the garden, and the tree of the knowledge of good and evil.</span></p><span id="en-NRSV-54" class="text Gen-2-23"><sup class="versenum">23&nbsp;</sup>Then the man said,</span><div class="poetry"><p class="line"><span class="text Gen-2-23">“This at last is bone of my bones</span><br><span class="indent-1"><span class="indent-1-breaks">&nbsp;&nbsp;&nbsp;&nbsp;</span><span class="text Gen-2-23">and flesh of my flesh;</span></span><br><span class="text Gen-2-23">this one shall be called Woman,<sup data-fn="#fen-NRSV-54d" class="footnote" data-link="[<a href=&quot;#fen-NRSV-54d&quot; title=&quot;See footnote d&quot;>d</a>]">[<a href="#fen-NRSV-54d" title="See footnote d">d</a>]</sup></span><br><span class="indent-1"><span class="indent-1-breaks">&nbsp;&nbsp;&nbsp;&nbsp;</span><span class="text Gen-2-23">for out of Man<sup data-fn="#fen-NRSV-54e" class="footnote" data-link="[<a href=&quot;#fen-NRSV-54e&quot; title=&quot;See footnote e&quot;>e</a>]">[<a href="#fen-NRSV-54e" title="See footnote e">e</a>]</sup> this one was taken.”</span></span></p></div><h4>Footnotes</h4><ol><li id="fen-NRSV-35a"><a href="#en-NRSV-35" title="Go to Genesis 2:4">Genesis 2:4</a> <span class="footnote-text">Heb <i>YHWH,</i> as in other places where “<span class="small-caps">Lord</span>” is spelled with capital letters (see also Ex 3.14–15 with notes).</span></li>
            <li id="fen-NRSV-38b"><a href="#en-NRSV-38" title="Go to Genesis 2:7">Genesis 2:7</a> <span class="footnote-text">Or <i>formed a man</i> (Heb <i>adam</i>) <i>of dust from the ground</i> (Heb <i>adamah</i>)</span></li>
            </ol></div> <!--end of footnotes-->
            </div>
            </div>
            </div></body></html>""");

    assertThat(scriptureStore.getScriptures("NRSV", EN, "Genesis 2").getItems()).containsExactly(
        new Scripture().setVersion("NRSV").setBook("Genesis").setChapter(2).setVerse(1).setText(
            "Thus the heavens and the earth were finished, and all their multitude."),
        new Scripture().setVersion("NRSV").setBook("Genesis").setChapter(2).setVerse(2).setText(
            "And on the seventh day God finished the work that he had done, and he rested on the seventh day from all the work that he had done."),
        new Scripture().setVersion("NRSV").setBook("Genesis").setChapter(2).setVerse(3).setText(
            "So God blessed the seventh day and hallowed it, because on it God rested from all the work that he had done in creation."),
        new Scripture().setVersion("NRSV").setBook("Genesis").setChapter(2).setVerse(4).setText(
            "These are the generations of the heavens and the earth when they were created. In the day that the Lord God made the earth and the heavens,"),
        new Scripture().setVersion("NRSV").setBook("Genesis").setChapter(2).setVerse(5).setText(
            "when no plant of the field was yet in the earth and no herb of the field had yet sprung up—for the Lord God had not caused it to rain upon the earth, and there was no one to till the ground;"),
        new Scripture().setVersion("NRSV").setBook("Genesis").setChapter(2).setVerse(6).setText(
            "but a stream would rise from the earth, and water the whole face of the ground—"),
        new Scripture().setVersion("NRSV").setBook("Genesis").setChapter(2).setVerse(7).setText(
            "then the Lord God formed man from the dust of the ground, and breathed into his nostrils the breath of life; and the man became a living being."),
        new Scripture().setVersion("NRSV").setBook("Genesis").setChapter(2).setVerse(8).setText(
            "And the Lord God planted a garden in Eden, in the east; and there he put the man whom he had formed."),
        new Scripture().setVersion("NRSV").setBook("Genesis").setChapter(2).setVerse(9).setText(
            "Out of the ground the Lord God made to grow every tree that is pleasant to the sight and good for food, the tree of life also in the midst of the garden, and the tree of the knowledge of good and evil."),
        new Scripture().setVersion("NRSV").setBook("Genesis").setChapter(2).setVerse(23).setText(
            "Then the man said, “This at last is bone of my bones and flesh of my flesh; this one shall be called Woman, for out of Man this one was taken.”"));
  }

  @Test @Ignore
  public void fetch_poetry() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        """
            <html></body><div class="passage-text">
            <div class='passage-content passage-class-0'><div class="version-NRSV result-text-style-normal text-html">
             <p class="first-line-none"><span id="en-NRSV-35986" class="text Ps151-1-1"> </span></p><h4 class="psalm-title"><span class="text Ps151-1-1">This psalm is ascribed to David as his own composition (though it is outside the number<sup data-fn='#fen-NRSV-35986a' class='footnote' data-link='[&lt;a href=&quot;#fen-NRSV-35986a&quot; title=&quot;See footnote a&quot;&gt;a&lt;/a&gt;]'>[<a href="#fen-NRSV-35986a" title="See footnote a">a</a>]</sup>), after he had fought in single combat with Goliath.</span></h4><div class="poetry"><p class="line"><span class="text Ps151-1-1"><sup class="versenum">1 </sup>I was small among my brothers,</span><br /><span class="indent-1"><span class="indent-1-breaks">&nbsp;&nbsp;&nbsp;&nbsp;</span><span class="text Ps151-1-1">and the youngest in my father’s house;</span></span><br /><span class="text Ps151-1-1">I tended my father’s sheep.</span></p></div><div class="poetry top-1"><p class="line"><span id="en-NRSV-35987" class="text Ps151-1-2"><sup class="versenum">2 </sup>My hands made a harp;</span><br /><span class="indent-1"><span class="indent-1-breaks">&nbsp;&nbsp;&nbsp;&nbsp;</span><span class="text Ps151-1-2">my fingers fashioned a lyre.</span></span></p></div><div class="poetry top-1"><p class="line"><span id="en-NRSV-35988" class="text Ps151-1-3"><sup class="versenum">3 </sup>And who will tell my Lord?</span><br /><span class="indent-1"><span class="indent-1-breaks">&nbsp;&nbsp;&nbsp;&nbsp;</span><span class="text Ps151-1-3">The Lord himself; it is he who hears.<sup data-fn='#fen-NRSV-35988b' class='footnote' data-link='[&lt;a href=&quot;#fen-NRSV-35988b&quot; title=&quot;See footnote b&quot;&gt;b&lt;/a&gt;]'>[<a href="#fen-NRSV-35988b" title="See footnote b">b</a>]</sup></span></span></p></div><div class="poetry top-1"><p class="line"><span id="en-NRSV-35989" class="text Ps151-1-4"><sup class="versenum">4 </sup>It was he who sent his messenger<sup data-fn='#fen-NRSV-35989c' class='footnote' data-link='[&lt;a href=&quot;#fen-NRSV-35989c&quot; title=&quot;See footnote c&quot;&gt;c&lt;/a&gt;]'>[<a href="#fen-NRSV-35989c" title="See footnote c">c</a>]</sup></span><br /><span class="indent-1"><span class="indent-1-breaks">&nbsp;&nbsp;&nbsp;&nbsp;</span><span class="text Ps151-1-4">and took me from my father’s sheep,</span></span><br /><span class="indent-1"><span class="indent-1-breaks">&nbsp;&nbsp;&nbsp;&nbsp;</span><span class="text Ps151-1-4">and anointed me with his anointing oil.</span></span></p></div><div class="poetry top-1"><p class="line"><span id="en-NRSV-35990" class="text Ps151-1-5"><sup class="versenum">5 </sup>My brothers were handsome and tall,</span><br /><span class="indent-1"><span class="indent-1-breaks">&nbsp;&nbsp;&nbsp;&nbsp;</span><span class="text Ps151-1-5">but the Lord was not pleased with them.</span></span></p></div><div class="poetry top-1"><p class="line"><span id="en-NRSV-35991" class="text Ps151-1-6"><sup class="versenum">6 </sup>I went out to meet the Philistine,<sup data-fn='#fen-NRSV-35991d' class='footnote' data-link='[&lt;a href=&quot;#fen-NRSV-35991d&quot; title=&quot;See footnote d&quot;&gt;d&lt;/a&gt;]'>[<a href="#fen-NRSV-35991d" title="See footnote d">d</a>]</sup></span><br /><span class="indent-1"><span class="indent-1-breaks">&nbsp;&nbsp;&nbsp;&nbsp;</span><span class="text Ps151-1-6">and he cursed me by his idols.</span></span></p></div><div class="poetry top-1"><p class="line"><span id="en-NRSV-35992" class="text Ps151-1-7"><sup class="versenum">7 </sup>But I drew his own sword;</span><br /><span class="indent-1"><span class="indent-1-breaks">&nbsp;&nbsp;&nbsp;&nbsp;</span><span class="text Ps151-1-7">I beheaded him, and took away disgrace from the people of Israel.</span></span></p></div> <div class="footnotes">
            <h4>Footnotes</h4><ol><li id="fen-NRSV-35986a"><a href="#en-NRSV-35986" title="Go to Psalm 151 1:1">Psalm 151 1:1</a> <span class='footnote-text'>Other ancient authorities add <i>of the one hundred fifty</i> (psalms)</span></li>

            <li id="fen-NRSV-35988b"><a href="#en-NRSV-35988" title="Go to Psalm 151 1:3">Psalm 151 1:3</a> <span class='footnote-text'>Other ancient authorities add <i>everything</i>; others add <i>me</i>; others read <i>who will hear me</i></span></li>

            <li id="fen-NRSV-35989c"><a href="#en-NRSV-35989" title="Go to Psalm 151 1:4">Psalm 151 1:4</a> <span class='footnote-text'>Or <i>angel</i></span></li>

            <li id="fen-NRSV-35991d"><a href="#en-NRSV-35991" title="Go to Psalm 151 1:6">Psalm 151 1:6</a> <span class='footnote-text'>Or <i>foreigner</i></span></li>

            </ol></div> <!--end of footnotes-->
            </div>
            </div>
            </div>
            </div>
            </div></body></html>""");

    assertThat(scriptureStore.getScriptures("NRSV", EN, "Psalms 151").getItems()).containsExactly(
        new Scripture().setVersion("NRSV").setBook("Apocryphal Psalms").setChapter(1).setVerse(1).setText(
            "I was small among my brothers, and the youngest in my father’s house; I tended my father’s sheep."),
        new Scripture().setVersion("NRSV").setBook("Apocryphal Psalms").setChapter(1).setVerse(2).setText(
            "My hands made a harp; my fingers fashioned a lyre."),
        new Scripture().setVersion("NRSV").setBook("Apocryphal Psalms").setChapter(1).setVerse(3).setText(
            "And who will tell my Lord? The Lord himself; it is he who hears."),
        new Scripture().setVersion("NRSV").setBook("Apocryphal Psalms").setChapter(1).setVerse(4).setText(
            "It was he who sent his messenger and took me from my father’s sheep, and anointed me with his anointing oil."),
        new Scripture().setVersion("NRSV").setBook("Apocryphal Psalms").setChapter(1).setVerse(5).setText(
            "My brothers were handsome and tall, but the Lord was not pleased with them."),
        new Scripture().setVersion("NRSV").setBook("Apocryphal Psalms").setChapter(1).setVerse(6).setText(
            "I went out to meet the Philistine, and he cursed me by his idols."),
        new Scripture().setVersion("NRSV").setBook("Apocryphal Psalms").setChapter(1).setVerse(7).setText(
            "But I drew his own sword; I beheaded him, and took away disgrace from the people of Israel."));
  }

  @Test
  public void fetch_removesNotes() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        """
            <html></body><div class="passage-text">
            <div class="passage-content passage-class-0"><div class="version-NRSV result-text-style-normal text-html">
            <p class="first-line-none"><span id="en-NRSV-35523" class="text 1Esd-1-1">(b) The books from 1 Esdras through 3 Maccabees are recognized as Deuterocanonical Scripture by the Greek and the Russian Orthodox Churches. They are not so recognized by the Roman Catholic Church, but 1 Esdras and the Prayer of Manasseh (together with 2 Esdras) are placed in an appendix to the Latin Vulgate Bible.</span></p><h3><span class="text 1Esd-1-1">Josiah Celebrates the Passover</span></h3><p class="chapter-1"><span class="text 1Esd-1-1"><span class="chapternum">1&nbsp;</span>Josiah kept the passover to his Lord in Jerusalem; he killed the passover lamb on the fourteenth day of the first month, </span><span id="en-NRSV-35524" class="text 1Esd-1-2"><sup class="versenum">2&nbsp;</sup>having placed the priests according to their divisions, arrayed in their vestments, in the temple of the Lord. </span><span id="en-NRSV-35525" class="text 1Esd-1-3"><sup class="versenum">3&nbsp;</sup>He told the Levites, the temple servants of Israel, that they should sanctify themselves to the Lord and put the holy ark of the Lord in the house that King Solomon, son of David, had built; </span><span id="en-NRSV-35526" class="text 1Esd-1-4"><sup class="versenum">4&nbsp;</sup>and he said, “You need no longer carry it on your shoulders. Now worship the Lord your God and serve his people Israel; prepare yourselves by your families and kindred, </span><span id="en-NRSV-35527" class="text 1Esd-1-5"><sup class="versenum">5&nbsp;</sup>in accordance with the directions of King David of Israel and the magnificence of his son Solomon. Stand in order in the temple according to the groupings of the ancestral houses of you Levites, who minister before your kindred the people of Israel, </span><span id="en-NRSV-35528" class="text 1Esd-1-6"><sup class="versenum">6&nbsp;</sup>and kill the passover lamb and prepare the sacrifices for your kindred, and keep the passover according to the commandment of the Lord that was given to Moses.”</span></p></div></div></div></body></html>""");

    assertThat(scriptureStore.getScriptures("NRSV", EN, "1 ESDRAS 1").getItems()).containsExactly(
        new Scripture().setVersion("NRSV").setBook("1 Esdras").setChapter(1).setVerse(1).setText(
            "Josiah kept the passover to his Lord in Jerusalem; he killed the passover lamb on the fourteenth day of the first month,"),
        new Scripture().setVersion("NRSV").setBook("1 Esdras").setChapter(1).setVerse(2).setText(
            "having placed the priests according to their divisions, arrayed in their vestments, in the temple of the Lord."),
        new Scripture().setVersion("NRSV").setBook("1 Esdras").setChapter(1).setVerse(3).setText(
            "He told the Levites, the temple servants of Israel, that they should sanctify themselves to the Lord and put the holy ark of the Lord in the house that King Solomon, son of David, had built;"),
        new Scripture().setVersion("NRSV").setBook("1 Esdras").setChapter(1).setVerse(4).setText(
            "and he said, “You need no longer carry it on your shoulders. Now worship the Lord your God and serve his people Israel; prepare yourselves by your families and kindred,"),
        new Scripture().setVersion("NRSV").setBook("1 Esdras").setChapter(1).setVerse(5).setText(
            "in accordance with the directions of King David of Israel and the magnificence of his son Solomon. Stand in order in the temple according to the groupings of the ancestral houses of you Levites, who minister before your kindred the people of Israel,"),
        new Scripture().setVersion("NRSV").setBook("1 Esdras").setChapter(1).setVerse(6).setText(
            "and kill the passover lamb and prepare the sacrifices for your kindred, and keep the passover according to the commandment of the Lord that was given to Moses.”"));
  }
}

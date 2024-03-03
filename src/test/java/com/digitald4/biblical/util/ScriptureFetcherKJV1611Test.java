package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import org.junit.Test;

public class ScriptureFetcherKJV1611Test extends ScriptureFetcherTest {

  @Test
  public void fetch() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        """
            <html></body><div class="book_reader">
            <div class="previous" title="Previous Chapter"> <a href="../1611_1-Maccabees-Chapter-3/" title="View 1 Maccabees Chapter 3" class="previous_btn"></a> </div>
            <div class="chapters_div">
            <div class="chapters_div_in">
            <div class="header_div">
            <div class="left_buttons">
            <span class="hidebooks"><a href="#" title="Previous Book" class="btn">&lt;&lt;</a></span>
            <a href="../1611_1-Maccabees-Chapter-3/" title="View 1 Maccabees Chapter 3" class="btn pre_chap">&lt; <span>Previous Chapter</span></a>
            </div>
            <div class="right_buttons"><a href="../1611_1-Maccabees-Chapter-5/" title="View 1 Maccabees Chapter 5" class="btn next_chap"><span>Next Chapter</span> &gt;</a> <span class="hidebooks"><a href="#" title="Next Book" class="btn hidebooks">&gt;&gt;</a></span></div>
            <h3 class="cinzel white">1 Maccabees<br><span class="chapter cinzel">Chapter 4</span></h3>
            <div class="clr"></div>
            </div>
            <div class="bx-wrapper">
            <div class="bxslider">
            <div class="in_slider">
            <h4 class="center "><a href="../King-James-Bible-English/"><strong class="red cinzel">Viewing the original 1611 KJV with archaic English spelling.</strong></a><br>
            <a class="bold red cinzel underline" href="../1-Maccabees-Chapter-4/">Click to switch to the Standard KJV.</a></h4>
            <br><br>
            <div id="fontsizehide" class="red cinzel center">
            <a class="red" style="font-size:130%;" onclick="resizeText(1)" href="#" rel="nofollow">+</a> &nbsp; &nbsp; <a class="red cinzel" href="#" title="Adjust Font Size" onclick="resizeText(0.2)">Text Size</a> &nbsp; &nbsp;
            <a class="red" style="font-size:130%;" onclick="resizeText(-1)" href="#" rel="nofollow">—</a>
            <br><br>
            </div>
            <p class="center"><a class="red cinzel help" href="/Apocrypha-Books/" title="1 Maccabees was published in the original 1611 King James Bible. Click to learn more...">Why is 1 Maccabees shown with the King James Bible?</a></p>
            <div id="div" style="font-size: 1em !important;">

            <p><a href="../1611_1-Maccabees-4-1/" title="View more info for 1 Maccabees 4:1"><span id="3" class="versehover">1 </span></a>Then tooke Gorgias fiue thousand footmen, and a thousand of the best horsemen, and remooued out of the campe by night:</p><p><a href="../1611_1-Maccabees-4-2/" title="View more info for 1 Maccabees 4:2"><span id="4" class="versehover">2 </span></a>To the end he might rush in vpon the camp of the Iewes, and smite them suddenly. And the men of the fortresse were his guides.</p><p><a href="../1611_1-Maccabees-4-3/" title="View more info for 1 Maccabees 4:3"><span id="5" class="versehover">3 </span></a>Now when Iudas heard thereof, hee himselfe remooued, and the valiant men with him, that hee might smite the Kings armie which was at Emmaus,</p><p><a href="../1611_1-Maccabees-4-4/" title="View more info for 1 Maccabees 4:4"><span id="6" class="versehover">4 </span></a>While as yet the forces were dispersed from the campe.</p><p><a href="../1611_1-Maccabees-4-5/" title="View more info for 1 Maccabees 4:5"><span id="7" class="versehover">5 </span></a>In the meane season came Gorgias by night into the campe of Iudas: and when hee found no man there, hee sought them in the mountaines: for said hee, these fellowes flee from vs.</p></div>
            <script type="text/javascript">resizeText(0)</script>
            </div>
            </div>
            <h4 class="center "><a href="../King-James-Bible-English/"><strong class="red cinzel">Viewing the original 1611 KJV with archaic English spelling</strong></a>
            <br><a class="bold red cinzel underline" href="../1-Maccabees-Chapter-4/">Click to switch to the Standard KJV.</a></h4>
            <br>
            </div>
            <div class="footer_div">
            <div>
            <div class="left_buttons"><span class="hidebooks"><a href="#" title="Previous Book" class="btn">&lt;&lt;</a></span>
            <a href="../1611_1-Maccabees-Chapter-3/" title="View 1 Maccabees Chapter 3" class="btn pre_chap">&lt; <span>Previous Chapter</span></a></div>
            <div class="right_buttons"><a href="../1611_1-Maccabees-Chapter-5/" title="View 1 Maccabees Chapter 5" class="btn next_chap"><span>Next Chapter</span> &gt;</a> <span class="hidebooks"><a href="#" title="Next Book" class="btn">&gt;&gt;</a></span></div>
             </div>
            <div class="clr"></div>
            <div><br></div>

            </div>
            </div>
            </div>
            <div class="next"> <a href="../1611_1-Maccabees-Chapter-5/" title="View 1 Maccabees Chapter 5" class="next_btn"></a> </div>
            <div class="clear"></div>
            </div></body></html>""");

    assertThat(scriptureStore.getScriptures("KJV1611", Language.EN, "1 MACCABEES 4").getItems()).containsExactly(
        new Scripture().setVersion("KJV1611").setBook("1 Maccabees").setChapter(4).setVerse(1).setText(
            "Then tooke Gorgias fiue thousand footmen, and a thousand of the best horsemen, and remooued out of the campe by night:"),
        new Scripture().setVersion("KJV1611").setBook("1 Maccabees").setChapter(4).setVerse(2).setText(
            "To the end he might rush in vpon the camp of the Iewes, and smite them suddenly. And the men of the fortresse were his guides."),
        new Scripture().setVersion("KJV1611").setBook("1 Maccabees").setChapter(4).setVerse(3).setText(
            "Now when Iudas heard thereof, hee himselfe remooued, and the valiant men with him, that hee might smite the Kings armie which was at Emmaus,"),
        new Scripture().setVersion("KJV1611").setBook("1 Maccabees").setChapter(4).setVerse(4).setText(
            "While as yet the forces were dispersed from the campe."),
        new Scripture().setVersion("KJV1611").setBook("1 Maccabees").setChapter(4).setVerse(5).setText(
            "In the meane season came Gorgias by night into the campe of Iudas: and when hee found no man there, hee sought them in the mountaines: for said hee, these fellowes flee from vs."));
  }
}

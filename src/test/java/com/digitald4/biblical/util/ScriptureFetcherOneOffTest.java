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

public class ScriptureFetcherOneOffTest {
  @Mock private final APIConnector apiConnector = mock(APIConnector.class);
  private ScriptureFetcher scriptureFetcher;

  @Before
  public void setup() {
    scriptureFetcher = new ScriptureFetcherOneOff(apiConnector);
  }

  @Test
  public void fetchCommunityRule() {
    when(apiConnector.sendGet(anyString())).thenReturn("<!DOCTYPE HTML PUBLIC \"-//w3c//dtd html 4.0 transitional//en\" \"http://www.w3.org/TR/REC-html40/loose.dtd\">\n" +
        "<HTML> \n" +
        "  <HEAD>\n" +
        "\t <META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=iso-8859-1\"> \n" +
        "\t <TITLE>Manual of Discipline</TITLE> \n" +
        "  </HEAD> \n" +
        "  <BODY BACKGROUND=\"../background.gif\"> \n" +
        "\t <CENTER>\n" +
        "\t\t<IMG SRC=\"scroll-half.gif\" HEIGHT=\"144\" WIDTH=\"300\" ALT=\"scroll-half\"> \n" +
        "\t\t<P><B><FONT COLOR=\"#800000\" SIZE=\"+3\">Manual of Discipline</FONT></B></P>\n" +
        "\t\t</CENTER> \n" +
        "\t <P><B><FONT COLOR=\"#800000\">Of the Commitment.</FONT></B> </P> \n" +
        "\t <P><B>Everyone who wishes to join the community must pledge himself to\n" +
        "\t\trespect God and man; to live according to the communal rule: to seek God [ ];\n" +
        "\t\tto do what is good and upright in His sight, in accordance with what He has\n" +
        "\t\tcommanded through Moses and through His servants the prophets; to love all that\n" +
        "\t\tHe has chosen and hate all that He has rejected; to keep far from evil and to\n" +
        "\t\tcling to all good works; to act truthfully and righteously and justly on earth\n" +
        "\t\tand to walk no more in the stubbornness of a guilty heart and of lustful eyes,\n" +
        "\t\tdoing all manner of evil; to bring into a bond of mutual love all who have\n" +
        "\t\tdeclared their willingness to carry out the statutes of God; to join the formal\n" +
        "\t\tcommunity of God; to walk blamelessly before Him in conformity with all that\n" +
        "\t\thas been revealed as relevant to the several periods during which they are to\n" +
        "\t\tbear witness (to Him) ; to love all the children of light, each according to\n" +
        "\t\tthe measure of his guilt, which God will ultimately requite.</B> </P> \n" +
        "\t <P><B>All who declare their willingness to serve God's truth must bring all\n" +
        "\t\tof their mind, all of their strength, and all of their wealth into the\n" +
        "\t\tcommunity of God, so that their minds may be purified by the truth of His\n" +
        "\t\tprecepts, their strength controlled by His perfect ways, and their wealth\n" +
        "\t\tdisposed in accordance with His just design. They must not deviate by a single\n" +
        "\t\tstep from carrying out the orders of God at the times appointed for them; they\n" +
        "\t\tmust neither advance the statutory times nor postpone the prescribed seasons.\n" +
        "\t\tThey must not turn aside from the ordinances of God's truth either to the right\n" +
        "\t\tor to the left.</B></P><P><B><FONT COLOR=\"#800000\">Of Initiation.</FONT></B></P></BODY</HTML>");

    assertThat(scriptureFetcher.fetch("essene", BibleBook.COMMUNITY_RULE, 1)).containsExactly(
        new Scripture().setVersion("essene").setBook("Community Rule").setChapter(1).setVerse(1).setText(
            "Of the Commitment."),
        new Scripture().setVersion("essene").setBook("Community Rule").setChapter(1).setVerse(2).setText(
            "Everyone who wishes to join the community must pledge himself to respect God and man; to live according " +
                "to the communal rule: to seek God [ ]; to do what is good and upright in His sight, in accordance " +
                "with what He has commanded through Moses and through His servants the prophets; to love all that He " +
                "has chosen and hate all that He has rejected; to keep far from evil and to cling to all good works; " +
                "to act truthfully and righteously and justly on earth and to walk no more in the stubbornness of a " +
                "guilty heart and of lustful eyes, doing all manner of evil; to bring into a bond of mutual love all " +
                "who have declared their willingness to carry out the statutes of God; to join the formal community " +
                "of God; to walk blamelessly before Him in conformity with all that has been revealed as relevant to " +
                "the several periods during which they are to bear witness (to Him) ; to love all the children of " +
                "light, each according to the measure of his guilt, which God will ultimately requite."),
        new Scripture().setVersion("essene").setBook("Community Rule").setChapter(1).setVerse(3).setText(
            "All who declare their willingness to serve God's truth must bring all of their mind, all of their " +
                "strength, and all of their wealth into the community of God, so that their minds may be purified by " +
                "the truth of His precepts, their strength controlled by His perfect ways, and their wealth disposed " +
                "in accordance with His just design. They must not deviate by a single step from carrying out the " +
                "orders of God at the times appointed for them; they must neither advance the statutory times nor " +
                "postpone the prescribed seasons. They must not turn aside from the ordinances of God's truth either " +
                "to the right or to the left."),
        new Scripture().setVersion("essene").setBook("Community Rule").setChapter(1).setVerse(4).setText(
            "Of Initiation."));
  }

  @Test
  public void fetchEnoch() {
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
  public void fetchWarScroll() {
    when(apiConnector.sendGet(anyString())).thenReturn("<!DOCTYPE html PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'><HTML><HEAD>\n" +
        "  <TITLE>War Scroll (1QM)</TITLE>\n" +
        "  <META NAME=\"robots\" CONTENT=\"index\">\n" +
        "  <META NAME=\"Date\" CONTENT=\"2013-10-06\">\n" +
        "  <META NAME=\"GENERATOR\" CONTENT=\"IzzyED/2\">\n" +
        "  <META NAME=\"Author\" content=\"Andreas Itzchak Rehberg\">\n" +
        "  <META NAME=\"Description\" content=\"War Scroll (1QM)\">\n" +
        "  <META NAME=\"KeyWords\" lang=\"de\" content=\"Kriegsrolle, 1QM, Qumran, Essener\">\n" +
        "  <META NAME=\"KeyWords\" lang=\"en\" content=\"War Scroll, 1QM, Qumran, Essenes\">\n" +
        "  <META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
        "  <META http-equiv=\"Content-Language\" content=\"en\">\n" +
        "  <LINK REL=\"stylesheet\" TYPE=\"text/css\" HREF=\"/styles/qumran.css\">\n" +
        "  <LINK REL=\"stylesheet\" TYPE=\"text/css\" HREF=\"/styles/text.css\">\n" +
        "  </HEAD><BODY>\n" +
        "<H1>War Scroll (1QM)</H1>\n" +
        "<H3>The description of the eschatological war.</H3>\n" +
        "\n" +
        "<TABLE ALIGN=\"center\" WIDTH=\"95%\" BORDER=\"0\" CELLSPACING=\"0\" CELLPADDING=\"0\">\n" +
        " <TR><TH COLSPAN=\"2\">COL I</TH></TR>\n" +
        " <TR><TD WIDTH=\"35\" VALIGN=\"top\">(1)</TD><TD>For the In[structor, the Rule of]\n" +
        "   the War. The first attack of the Sons of Light shall be undertaken against\n" +
        "   the forces of the Sons of Darkness, the army of Belial: the troops of Edom,\n" +
        "   Moab, the sons of Ammon, the [Amalekites],</TD></TR>\n" +
        " <TR><TD VALIGN=\"top\">(2)</TD><TD>Philistia, and the troops of the Kittim of\n" +
        "   Asshur. Supporting them are those who have violated the covenant. The sons of\n" +
        "   Levi, the sons of Judah, and the sons of Benjamin, those exiled to the\n" +
        "   wilderness, shall fight against them</TD></TR>\n" +
        " <TR><TD VALIGN=\"top\">(3)</TD><TD>with [...] against all their troops, when the\n" +
        "   exiles of the Sons of Light return from the Wilderness of the Peoples to\n" +
        "   camp in the Wilderness of Jerusalem. Then after the battle they shall go up\n" +
        "   from that place</TD></TR>" +
        "<TR><TH VALIGN=\"top\" COLSPAN=\"2\">Col. 2</TH></TR>\n" +
        " <TR><TD VALIGN=\"top\">(1)</TD><TD>the congregation's clans, fifty-two. They shall\n" +
        "   rank the chiefs of the priests after the Chief Priest and his deputy; twelve\n" +
        "   chief priests to serve</TD></TR>\n" +
        " <TR><TD VALIGN=\"top\">(2)</TD><TD>in the regular offering before God. The chiefs\n" +
        "   of the courses, twenty-six, shall serve in their courses. After them the\n" +
        "   chiefs of the Levites serve continually, twelve in all, one</TD></TR>" +
        "<TR><TD VALIGN=\"top\" COLSPAN=\"2\">&nbsp;</TD></TR>\n" +
        "\n" +
        " <TR><TH VALIGN=\"top\" COLSPAN=\"2\">The description of the trumpets.</TH></TR>\n" +
        " <TR><TD VALIGN=\"top\">(15)</TD><TD>[The Rule of the Trumpets: the trumpets] of alarm for all their service" +
        " for the [...] for their commissioned men,</TD></TR>" +
        "<TR><TD VALIGN=\"top\">(16)</TD><TD>[by tens of thousands and thousands and\n" +
            "   hundreds and fifties] and tens. Upon the t[rumpets ...]</TD></TR>\n" +
            " <TR><TD VALIGN=\"top\">(17)</TD><TD>[...]</TD></TR>\n" +
            " <TR><TD VALIGN=\"top\">(18)</TD><TD>[...]</TD></TR>" +
        "</TABLE></BODY></HTML>");

    assertThat(scriptureFetcher.fetch("qumran", BibleBook.WAR_SCROLL, 1)).containsExactly(
        new Scripture().setVersion("qumran").setBook("War Scroll").setChapter(1).setVerse(1).setText(
            "For the In[structor, the Rule of] the War. The first attack of the Sons of Light shall be undertaken against the forces of the Sons of Darkness, the army of Belial: the troops of Edom, Moab, the sons of Ammon, the [Amalekites],"),
        new Scripture().setVersion("qumran").setBook("War Scroll").setChapter(1).setVerse(2).setText(
            "Philistia, and the troops of the Kittim of Asshur. Supporting them are those who have violated the covenant. The sons of Levi, the sons of Judah, and the sons of Benjamin, those exiled to the wilderness, shall fight against them"),
        new Scripture().setVersion("qumran").setBook("War Scroll").setChapter(1).setVerse(3).setText(
            "with [...] against all their troops, when the exiles of the Sons of Light return from the Wilderness of the Peoples to camp in the Wilderness of Jerusalem. Then after the battle they shall go up from that place"),
        new Scripture().setVersion("qumran").setBook("War Scroll").setChapter(2).setVerse(1).setText(
            "the congregation's clans, fifty-two. They shall rank the chiefs of the priests after the Chief Priest and his deputy; twelve chief priests to serve"),
        new Scripture().setVersion("qumran").setBook("War Scroll").setChapter(2).setVerse(2).setText(
            "in the regular offering before God. The chiefs of the courses, twenty-six, shall serve in their courses. After them the chiefs of the Levites serve continually, twelve in all, one"),
        new Scripture().setVersion("qumran").setBook("War Scroll").setChapter(2).setVerse(15).setText(
            "[The Rule of the Trumpets: the trumpets] of alarm for all their service for the [...] for their commissioned men,"),
    new Scripture().setVersion("qumran").setBook("War Scroll").setChapter(2).setVerse(16).setText(
        "[by tens of thousands and thousands and hundreds and fifties] and tens. Upon the t[rumpets ...]"),
    new Scripture().setVersion("qumran").setBook("War Scroll").setChapter(2).setVerse(17).setText(
        "[...]"),
    new Scripture().setVersion("qumran").setBook("War Scroll").setChapter(2).setVerse(18).setText(
        "[...]"));
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

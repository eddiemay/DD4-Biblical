package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.Language.EN;
import static com.digitald4.biblical.util.Language.GEEZ;
import static com.digitald4.biblical.util.Language.HEBREW;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

public class ScriptureFetcherOneOffTest extends ScriptureFetcherTest {
  private static final String ISAIAH_SCROLL = "src/main/python/services/dss_images/books/1Q_Isaiah_a.txt";
  @Before
  public void setup() {
    super.setup();
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

    assertThat(scriptureStore.getScriptures("DSS", EN, "COMMUNITY RULE 1").getItems()).containsExactly(
        new Scripture().setVersion("DSS").setBook("Community Rule").setChapter(1).setVerse(1).setText(
            "Of the Commitment."),
        new Scripture().setVersion("DSS").setBook("Community Rule").setChapter(1).setVerse(2).setText(
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
        new Scripture().setVersion("DSS").setBook("Community Rule").setChapter(1).setVerse(3).setText(
            "All who declare their willingness to serve God's truth must bring all of their mind, all of their " +
                "strength, and all of their wealth into the community of God, so that their minds may be purified by " +
                "the truth of His precepts, their strength controlled by His perfect ways, and their wealth disposed " +
                "in accordance with His just design. They must not deviate by a single step from carrying out the " +
                "orders of God at the times appointed for them; they must neither advance the statutory times nor " +
                "postpone the prescribed seasons. They must not turn aside from the ordinances of God's truth either " +
                "to the right or to the left."),
        new Scripture().setVersion("DSS").setBook("Community Rule").setChapter(1).setVerse(4).setText(
            "Of Initiation."));
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
        "  <META NAME=\"KeyWords\" lang=\"de\" content=\"Kriegsrolle, 1QM, DSS, Essener\">\n" +
        "  <META NAME=\"KeyWords\" lang=\"en\" content=\"War Scroll, 1QM, DSS, Essenes\">\n" +
        "  <META http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
        "  <META http-equiv=\"Content-Language\" content=\"en\">\n" +
        "  <LINK REL=\"stylesheet\" TYPE=\"text/css\" HREF=\"/styles/DSS.css\">\n" +
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

    assertThat(scriptureStore.getScriptures("DSS", EN, "WAR SCROLL 1-2").getItems()).containsExactly(
        new Scripture().setVersion("DSS").setBook("War Scroll").setChapter(1).setVerse(1).setText(
            "For the In[structor, the Rule of] the War. The first attack of the Sons of Light shall be undertaken against the forces of the Sons of Darkness, the army of Belial: the troops of Edom, Moab, the sons of Ammon, the [Amalekites],"),
        new Scripture().setVersion("DSS").setBook("War Scroll").setChapter(1).setVerse(2).setText(
            "Philistia, and the troops of the Kittim of Asshur. Supporting them are those who have violated the covenant. The sons of Levi, the sons of Judah, and the sons of Benjamin, those exiled to the wilderness, shall fight against them"),
        new Scripture().setVersion("DSS").setBook("War Scroll").setChapter(1).setVerse(3).setText(
            "with [...] against all their troops, when the exiles of the Sons of Light return from the Wilderness of the Peoples to camp in the Wilderness of Jerusalem. Then after the battle they shall go up from that place"),
        new Scripture().setVersion("DSS").setBook("War Scroll").setChapter(2).setVerse(1).setText(
            "the congregation's clans, fifty-two. They shall rank the chiefs of the priests after the Chief Priest and his deputy; twelve chief priests to serve"),
        new Scripture().setVersion("DSS").setBook("War Scroll").setChapter(2).setVerse(2).setText(
            "in the regular offering before God. The chiefs of the courses, twenty-six, shall serve in their courses. After them the chiefs of the Levites serve continually, twelve in all, one"),
        new Scripture().setVersion("DSS").setBook("War Scroll").setChapter(2).setVerse(15).setText(
            "[The Rule of the Trumpets: the trumpets] of alarm for all their service for the [...] for their commissioned men,"),
        new Scripture().setVersion("DSS").setBook("War Scroll").setChapter(2).setVerse(16).setText(
            "[by tens of thousands and thousands and hundreds and fifties] and tens. Upon the t[rumpets ...]"),
        new Scripture().setVersion("DSS").setBook("War Scroll").setChapter(2).setVerse(17).setText(
            "[...]"),
        new Scripture().setVersion("DSS").setBook("War Scroll").setChapter(2).setVerse(18).setText(
            "[...]"));
  }

  @Test
  public void fetchJosephus() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/ant-8.html"));

    assertThat(scriptureStore.getScriptures("CCC", EN, "JOSEPHUS 8").getItems()).containsExactly(
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(1).setText(
            "[About An. 1056.] We have already treated of David, and his virtue; and of the benefits he was the author of to his countreymen; of his wars also, and battels which he managed with success, and then died an old man, in the foregoing book. And when Solomon his son, who was but a youth in age, had taken the Kingdom, and whom David had declared, while he was alive, the Lord of that people, according to God’s will: when he sat upon the throne, the whole body of the people made joyful acclamations to him: as is usual at the beginning of a reign: and wished that all his affairs might come to a blessed conclusion; and that he might arrive at a great age, and at the most happy state of affairs possible."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(2).setText(
            "[About An. 1055.] But Adonijah, who while his father was living attempted to gain possession of the government, came to the King’s mother Bathsheba, and saluted her with great civility: and when she asked him, whether he came to her as desiring her assistance in any thing or not? and bid him tell her if that were the case, for that she would chearfully afford it him; he began to say, that “She knew her self that the Kingdom was his, both on account of his elder age, and of the disposition of the multitude; and that yet it was transferred to Solomon her son, according to the will of God. He also said, that he was contented to be a servant under him, and was pleased with the present settlement. But he desired her to be a means of obtaining a favour from his brother to him, and to persuade him to bestow on him in marriage Abishag: who had indeed slept by his father, but because his father was too old he did not lie with her, and she was still a virgin.” So Bathsheba promised him to afford him her assistance very earnestly; and to bring this marriage about; because the King would be willing to gratify him in such a thing; and because she would press it to him very earnestly. Accordingly he went away in hopes of succeeding in this match. So Solomon’s mother went presently to her son, to speak to him about what she had promised, upon Adonijah’s supplication to her. And when her son came forward to meet her, and embraced her; and when he had brought her into the house where his royal throne was set, he sat thereon; and bid them set another throne on the right hand for his mother. When Bathsheba was set down, she said, “O son, grant me one request that I desire of thee, and do not any thing to me that is disagreeable or ungrateful: which thou wilt do if thou deniest me.” And when Solomon bid her to lay her commands upon him, because it was agreeable to his duty to grant her every thing she should ask; and complained that she did not at first begin her discourse with a firm expectation of obtaining what she desired; but had some suspicion of a denial: she intreated him to grant, that his brother Adonijah might marry Abishag."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(3).setText(
            "But the King was greatly offended at these words; and sent away his mother, and said, that “Adonijah aimed at great things; and that he wondered that she did not desire him to yield up the Kingdom to him, as to his elder brother: since she desired that he might marry Abishag: and that he had potent friends, Joab the captain of the host, and Abiathar the Priest.” So he called for Benaiah, the Captain of the guards, and ordered him to slay his brother Adonijah. He also called for Abiathar the Priest, and said to him, “I will not put thee to death, because of those other hardships which thou hast endured with my father; and because of the ark which thou hast born along with him: but I inflict this following punishment upon thee, because thou wast among Adonijah’s followers, and wast of his party. Do not thou continue here; nor come any more into my sight: but go to thine own town, and live on thy own fields, and there abide all thy life: for thou hast offended so greatly, that it is not just that thou shouldest retain thy dignity any longer.” For the fore\u00ADmentioned cause therefore it was, that the house of Ithamar was deprived of the sacerdotal dignity, as God had foretold to Eli, the grandfather of Abiathar. So it was transferred to the family of Phineas, to Zadok. Now those that were of the family of Phineas, but lived privately during the time that the High Priesthood was transferred to the house of Ithamar, (of which family Eli was the first that received it) were these that follow: Bukki, the son of Abishua, the High Priest: his son was Joatham: Joatham’s son was Meraioth: Meraioth’s son was Arophæus: Arophæus’s son was Ahitub: and Ahitub’s son was Zadok; who was first made High Priest in the reign of David."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(4).setText(
            "Now when Joab, the Captain of the host, heard of the slaughter of Adonijah, he was greatly afraid: for he was a greater friend to him, than to Solomon: and suspecting, not without reason, that he was in danger on account of his favour to Adonijah, he fled to the altar; and supposed he might procure safety thereby to himself; because of the King’s piety towards God. But when some told the King what Joab’s supposal was, he sent Benaiah, and commanded him to raise him up from the altar, and bring him to the judgment seat, in order to make his defence. However Joab said, he would not leave the altar, but would die there, rather than in another place. And when Benaiah had reported his answer to the King, Solomon commanded him to cut off his head there; and let him take that as a punishment for those two Captains of the host whom he had wickedly slain, and to bury his body: that his sins might never leave his family; but that himself and his father, by Joab’s death, might be guiltless. And when Benaiah had done what he was commanded to do, he was himself appointed to be Captain of the whole army. The King also made Zadok to be alone the High Priest, in the room of Abiathar: whom he had removed."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(5).setText(
            "But as to Shimei, Solomon commanded that he should build him an house, and stay at Jerusalem, and attend upon him; and should not have authority to go over the brook Cedron; and that if he disobeyed that command, death should be his punishment. He also threatened him so terribly, that he compelled him to take an oath that he would obey. Accordingly Shimei said, that “He had reason to thank Solomon for giving him such an injunction,” and added an oath that he would do as he bid him: and leaving his own countrey, he made his abode in Jerusalem. But three years afterwards, when he heard that two of his servants were run away from him, and were in Gath, he went for his servants in haste; and when he was come back with them, the King perceived it, and was much displeased that he had contemned his commands, and, what was more, had no regard to the oaths he had sworn to God. So he called him, and said to him, “Didst not thou swear never to leave me, nor to go out of this city to another? thou shalt not therefore escape punishment for thy perjury: but I will punish thee, thou wicked wretch, both for this crime, and for those wherewith thou didst abuse my father when he was in his flight: that thou mayst know that wicked men gain nothing at last; although they be not punished immediately upon their unjust practices: but that in all the time wherein they think themselves secure, because they have yet suffered nothing, their punishment increases, and is heavier upon them; and that to a greater degree than if they had been punished immediately upon the commission of their crimes.” So Benaiah, on the King’s command, slew Shimei."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(6).setText(
            "[About An. 1053.] Solomon having already settled himself firmly in his Kingdom, and having brought his enemies to punishment, he married the daughter of Pharaoh, King of Egypt: and built the walls of Jerusalem much larger and stronger than those that had been before: and thenceforward he managed publick affairs very peaceably. Nor was his youth any hindrance in the exercise of justice, or in the observation of the laws, or in the remembrance of what charges his father had given him at his death: but he discharged every duty with great accuracy that might have been expected from such as are aged, and of the greatest prudence: he now resolved to go to Hebron, and sacrifice to God upon the brazen altar that was built by Moses. Accordingly he offered there burnt-offerings, in number a thousand. And when he had done this, he thought he had payed great honour to God. For as he was asleep that very night, God appeared to him, and commanded him to ask of him some gifts which he was ready to give him, as a reward for his piety. So Solomon asked of God what was most excellent, and of the greatest worth in it self; what God would bestow with the greatest joy; and what it was most profitable for man to receive. For he did not desire to have bestowed upon him either gold, or silver, or any other riches; as a man and a youth might naturally have done: for these are the things that generally are esteemed by most men, as alone of the greatest worth, and the best gifts of God/ But, said he, “Give me, O Lord, a sound mind, and a good understanding; whereby I may speak and judge the people according to truth and righteousness.” With these petitions God was well pleased; and promised to give him all those things that he had not mentioned in his option, riches, glory, victory over his enemies; and, in the first place, understanding and wisdom: and this in such a degree, as no other mortal man, neither Kings nor ordinary persons ever had. He also promised to preserve the Kingdom to his posterity for a very long time: if he continued righteous, and obedient to him, and imitated his father in those things wherein he excelled. When Solomon heard this from God, he presently leaped out of his bed; and when he had worshipped him, he returned to Jerusalem; and after he had offered great sacrifices before the tabernacle, he feasted all his own family."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(7).setText(
            "[About An. 1052.] In these days an hard cause came before him in judgment, which it was very difficult to find any end of. And I think it necessary to explain the fact, about which the contest was; that such as light upon my writings may know what a difficult cause Solomon was to determine; and those that are concerned in such matters may take this sagacity of the King’s for a pattern, that they may the more easily give sentence about such questions. There were two women, who were harlots in the course of their lives, that came to him: of whom she that seemed to be injured began to speak first, and said, “O King, I and this other woman dwell together in one room. Now it came to pass that we both bore a son at the same hour of the same day; and on the third day this woman overlaid her son, and killed it; and then took my son out of my bosom, and removed him to her self: and as I was asleep she laid her dead son in my arms. Now when, in the morning, I was desirous to give the breast to the child, I did not find my own; but saw this woman’s dead child lying by me: for I considered it exactly, and found it so to be. Hence it was that I demanded my son: and when I could not obtain him, I have recourse, my Lord, to thy assistance. For since we were alone, and there was no body there that could convict her, or affright her, she cares for nothing: but perseveres in the stout denial of the fact.” When this woman had told this her story, the King asked the other woman, what she had to say in contradiction to that story? But when she denied that she had done what was charged upon her; and said that it was her child that was living, and that it was her antagonists child that was dead: and when no one could devise what judgment could be given, and the whole court were blind in their understanding, and could not tell how to find out this riddle; the King alone invented the following way how to discover it. He bad them bring in both the dead and the living child; and sent one of his guards, and commanded him to fetch a sword, and draw it, and to cut both the children into two pieces: that each of the women might have half the living and half the dead child. Hereupon all the people privately laughed at the King, as no more than a youth. But in the mean time she that was the real mother of the living child cryed out, that he should not do so; but deliver that child to the other woman as her own: for she would be satisfied with the life of the child, and with the sight of it, although it were esteemed the other’s child. But the other woman was ready to see the child divided; and was desirous moreover that the first woman should be tormented. When the King understood that both their words proceeded from the truth of their passions, he adjudged the child to her that cried out to save it; for that she was the real mother of it: and he condemned the other as a wicked woman, who had not only killed her own child, but was endeavouring to see her friend’s child destroyed also. Now the multitude looked on this determination as a great sign and demonstration of the King’s sagacity and wisdom: and after that day attended to him as to one that had a divine mind."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(8).setText(
            "Now the captains of his armies, and officers appointed over the whole countrey were these. Over the lot of Ephraim was Ures; over the toparchy of Bethlehem was Dioclerus. Abinadab, who married Solomon’s daughter, had the region of Dora, and the sea coast under him. The great plain was under Benaiah, the son of Achilus. He also governed all the countrey as far as Jordan. Gabarius ruled over Gilead, and Gaulanitis; and had under him the sixty great and fenced cities [of Og.] Achinadab managed the affairs of all Galilee, as far as Sidon; and had himself also married a daughter of Solomon, whose name was Basima. Banacates had the sea coast about Arce: as had Shaphat, mount Tabor, and Carmel, and [the lower] Galilee, as far as the river Jordan: one man was appointed over all this countrey. Shimei was intrusted with the lot of Benjamin; and Gabares had the country beyond Jordan. Over whom there was again one governor appointed. Now the people of the Hebrews, and particularly the tribe of Judah, received a wonderful increase, when they betook themselves to husbandry, and the cultivation of their grounds. For as they enjoyed peace, and were not distracted with wars, and troubles; and having besides an unbounded fruition of the most desirable liberty, every one was busy in augmenting the product of their own lands; and making them worth more than they had formerly been."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(9).setText(
            "The King had also other rulers, who were over the land of Syria, and of the Philistines, which reached from the river Euphrates to Egypt: and these collected his tributes of the nations. Now these contributed to the King’s table, and to his supper every day thirty cori of fine flour, and sixty of meal: as also ten fat oxen, and twenty oxen out of the pastures; and an hundred fat lambs: all these were besides what were taken by hunting, harts, and buffaloes, and birds, and fishes, which were brought to the King by foreigners day by day. Solomon had also so great a number of chariots: that the stalls of his horses for those chariots were forty thousand: and besides these he had twelve thousand horsemen: the one half of which waited upon the King in Jerusalem, and the rest were dispersed abroad, and dwelt in the royal villages: But the same officer who provided for the King his expences, supplied also the fodder for the horses, and still carried it to the place where the King abode at that time."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(10).setText(
            "Now the sagacity and wisdom which God had bestowed on Solomon was so great, that he exceeded the ancients: insomuch that he was no way inferior to the Egyptians, who are said to have been beyond all men in understanding: nay indeed it is evident that their sagacity was very much inferior to that of the King’s. He also excelled and distinguished himself in wisdom above those who were most eminent among the Hebrews at that time for shrewdness. Those I mean were Ethan, and Heman, and Chalcol, and Darda, the sons of Mahol. He also composed Books of Odes, and Songs, a thousand and five. Of Parables and Similitudes three thousand. For he spake a parable upon every sort of tree, from the hyssop to the cedar: and in like manner also about beasts, about all sorts of living creatures, whether upon the earth, or in the seas, or in the air. For he was not unacquainted with any of their natures; nor omitted enquiries about them; but described them all like a philosopher; and demonstrated his exquisite knowledge of their several properties. God also enabled him to learn that skill which expels demons: which is a science useful, and sanative to men. He composed such incantations also by which distempers are alleviated. And he left behind him the manner of using exorcisms; by which they drive away demons; so that they never return: and this method of cure is of great force unto this day. For I have seen a certain man of my own countrey, whose name was Eleazar, releasing people that were demoniacal in the presence of Vespasian, and his sons, and his Captains, and the whole multitude of his soldiers: the manner of the cure was this: he put a ring that had a root of one of those sorts mentioned by Solomon to the nostrils of the demoniack: after which he drew out the demon through his nostrils: and when the man fell down immediately, he abjured him to return into him no more: making still mention of Solomon, and reciting the incantations which he composed. And when Eleazar would persuade and demonstrate to the spectators that he had such a power, he set a little way off a cup or bason full of water, and commanded the demon, as he went out of the man, to overturn it; and thereby to let the spectators know that he had left the man. And when this was done, the skill and wisdom of Solomon was shewed very manifestly. For which reason it is, that all men may know the vastness of Solomon’s abilities, and how he was beloved of God, and that the extraordinary virtues of every kind with which this King was endowed, may not be unknown to any people under the sun; for this reason, I say, it is, that we have proceeded to speak so largely of these matters."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(11).setText(
            "Moreover Hiram, King of Tyre, when he had heard that Solomon succeeded to his father’s Kingdom, was very glad of it: for he was a friend of David’s. So he sent ambassadors to him, and saluted him, and congratulated him on the present happy state of his affairs. Upon which Solomon sent him an epistle, the contents of which here follow."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(12).setText(
            "Know thou that my father would have built a temple to God; but was hindred by wars, and continual expeditions: for he did not leave off to overthrow his enemies, till he made them all subject to tribute. But I give thanks to God for the peace I at present enjoy: and on that account I am at leisure, and design to build an house to God. For God foretold to my father that such an houose should he built by me. Wherefore I desire thee to send some of thy subjects with mine, to mount Lebanon, to cut down timber: for the Sidonians are more skilful than our people in cutting of wood. As for wages to the hewers of wood, I will pay whatsoever price thou shalt determine."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(13).setText(
            "When Hiram had read this epistle, he was pleased with it; and wrote back this answer to Solomon."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(14).setText(
            "It is fit to bless God, that he hath committed thy father’s government to thee; who art a wise man, and endowed with all virtues. As for my self, I rejoice at the condition thou art in: and will be subservient to thee in all that thou sendest to me about. For when by my subjects I have cut down many and large trees of cedar, and cypress wood, I will send them to sea; and will order my subjects to make flotes of them, and to sail to what place soever of thy countrey thou shalt desire, and leave them there. After which thy subjects may carry them to Jerusalem. But do thou take care to procure us corn for this timber; which we stand in need of, because we inhabit in an island."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(15).setText(
            "The copies of these epistles remain at this day, and are preserved not only in our books, but among the Tyrians also: insomuch that if any one would know the certainty about them, he may desire of the keepers of the publick records of Tyre to shew him them, and he will find what is there set down to agree with what we have said. I have said so much out of a desire that my readers may know, that we speak nothing but the truth; and do not compose an history out of some plausible relations, which deceive men and please them at the same time; nor attempt to avoid examination; nor desire men to believe us immediately. Nor are we at liberty to depart from speaking truth, which is the proper commendation of an historian, and yet be blameless. But we insist upon no admission of what we say, unless we be able to manifest its truth by demonstration, and the strongest vouchers."),
        new Scripture().setVersion("CCC").setBook("Josephus").setChapter(8).setVerse(16).setText(
            "Now King Solomon, as soon as this epistle of the King of Tyre was brought him, commended the readiness and good will he declared therein: and repayed him in what he desired, and sent him yearly twenty thousand cori of wheat: and as many baths of oil. Now the bath is able to contain seventy two sextaries. He also sent him the same measure of wine. So the friendship between Hiram and Solomon hereby increased more and more: and they swore to continue it for ever. And the King appointed a tribute to be laid on all the people, of thirty thousand labourers; whose work he rendred easy to them by prudently dividing it among them. For he made ten thousand cut timber in mount Lebanon, for one month: and then to come home, and rest two months; until the time when the other twenty thousand had finished their task at the appointed time. And so afterward it came to pass, that the first ten thousand returned to their work every fourth month. And it was Adoram who was over this tribute. There were also of the strangers who were left by David, who were to carry the stones, and other materials, seventy thousand: and of those that cut the stones, eighty thousand. Of these three thousand and three hundred were rulers over the rest. He also enjoined them to cut out large stones for the foundations of the temple, and that they should fit them and unite them together in the mountain, and so bring them to the city. This was done not only by our own countrey workmen, but by those workmen whom Hiram sent also."));
  }

  @Test
  public void fetchTestamentOfJob() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/main/webapp/books/testament_of_job.html"));

    assertThat(scriptureStore.getScriptures("CCC", EN, "TESTAMENT OF JOB 12:17-19").getItems()).containsExactly(
        new Scripture().setVersion("CCC").setBook("Testament of Job").setChapter(12).setVerse(17).setText(
            "The name of Job was formerly Jobab, and he was called Job by the Lord."),
        new Scripture().setVersion("CCC").setBook("Testament of Job").setChapter(12).setVerse(18).setText(
            "He had lived before his plague eighty five years, and after the plague he took the double share of all; for this reason also his year’s he doubled, which is 170 years. In this way he lived altogether 255 years."),
        new Scripture().setVersion("CCC").setBook("Testament of Job").setChapter(12).setVerse(19).setText(
            "And, he saw sons of his sons to the fourth generation. It is written that he will rise up with those whom the Lord will reawaken. To our Lord by glory. Amen."));
  }

  @Test
  public void fetch3Enoch() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/the-third-book-of-enoch.html"));

    assertThat(scriptureStore.getScriptures("CCC", EN, "3 ENOCH 2").getItems()).containsExactly(
        new Scripture().setVersion("CCC").setBook("3 Enoch").setChapter(2).setVerse(1).setText(
            "In that hour the eagles of the Merkaba, the flaming 'Ophannim and the Seraphim of consuming fire asked a Metatron, saying to him:"),
        new Scripture().setVersion("CCC").setBook("3 Enoch").setChapter(2).setVerse(2).setText(
            "\"Youth! Why sufferest thou one born of woman to enter and behold the Merkaba? From which nation, from which tribe is this one? What is his character?\""),
        new Scripture().setVersion("CCC").setBook("3 Enoch").setChapter(2).setVerse(3).setText(
            "Metatron answered and said to them: \"From the nation of Israel whom the Holy One, blessed be He, chose for his people from among seventy tongues, from the tribe of Levi, whom he set aside as a contribution to his name and from the seed of Aaron whom the Holy One, blessed be He, did choose for his servant and put upon him the crown of priesthood on Sinai\"."),
        new Scripture().setVersion("CCC").setBook("3 Enoch").setChapter(2).setVerse(4).setText(
            "Forthwith they spake and said: \"Indeed, this one is worthy to behold the Merkaba \". And they said: \"Happy is the people that is in such a case!\""));
  }

  @Test
  public void fetchGadTheSeer() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/main/webapp/books/gad_the_seer.txt"));

    assertThat(scriptureStore.getScriptures("CCC", EN, "Gad the Seer 4:1-5").getItems()).containsExactly(
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(4).setVerse(1).setText(
            "In those days a man from Bethlehem, the city of David, went to Jerusalem to pay the vow which he had vowed unto the LORD. His name was Zabad the Parhi, and he was of the family of the Perezites."),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(4).setVerse(2).setText(
            "Zabad’s father was very sick, even unto the point of death, so Zabad vowed: "
                + "“When the LORD heals my father of his sickness, I will weigh out two talents of silver and give them to the House of the LORD, into the hand of King David.”"),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(4).setVerse(3).setText(
            "And it came to pass, when he was at the house of the shepherds along the way, he lost the pouch with the money in it and he was upset."),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(4).setVerse(4).setText(
            "He came to the city of Jerusalem, into the inner city, and he wrote on all the gates these words:"),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(4).setVerse(5).setText(
            "“Anyone who finds a lost pouch with two talents of silver and brings it to me I will give him a talent of silver as a reward.”"));

    assertThat(scriptureStore.getScriptures("CCC", EN, "Gad the Seer 1:1-4,13,14,15,58").getItems()).containsExactly(
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(1).setVerse(1).setText(
            "In the thirty-first year of King David’s reign in Jerusalem, which is the thirty-eighth year of David’s reign, the Word of the LORD came to Gad the Seer in the month of Iyar, near the stream of the Kidron Valley, saying:"),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(1).setVerse(2).setText(
            "“Thus says the LORD: ‘Go, be courageous, and stand in the midst of the stream, and cry in a great voice: “Tarry and hasten, tarry and hasten, tarry and hasten, for there is still a vision for the son of Jesse.”"),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(1).setVerse(3).setText(
            "And during the cry face the Eastern Gate, on the east side of the city, and stretch forth your hands toward heaven.’”"),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(1).setVerse(4).setText(
            "And I did exactly what I had been commanded to do."),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(1).setVerse(13).setText(
            "And lo, the sun came out of heaven in the shape of a man, with a crown on his head, carrying over his right shoulder a lamb, despised and rejected."),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(1).setVerse(14).setText(
            "And on the crown on his head three shepherds are seen, shackled with twelve shackles"),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(1).setVerse(15).setText(
            "and these shackles were of gold, plated with silver. And the voice of the Lamb was heard, great and dreadful like the voice of a lion roaring over his prey: "
                + "“Woe unto me! Woe unto me! Woe unto me! My image [Messiah] has been diminished, My refuge has been lost, My lot and destiny has turned Me"
                + " over to My spoilers, and I was defiled until evening by the touch of impurity.”"),
        new Scripture().setVersion("CCC").setBook("Gad the Seer").setChapter(1).setVerse(58).setText(
            "‘And yet for all that, when they are in the land of their enemies, I will not reject them, neither will I abhor them, to destroy them utterly, and to break My covenant with them; for I am the LORD their God.’"));
  }

  @Test
  public void fetchLivesOfTheProphets() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/main/webapp/books/lives_of_the_prophets.html"));

    assertThat(scriptureStore.getScriptures("CCC", EN, "Prophets 1:1-5").getItems()).containsExactly(
        new Scripture().setVersion("CCC").setBook("Lives of the Prophets").setChapter(1).setVerse(1).setText(
            "Isaiah was of Jerusalem. He met his death at the hands of Manasseh, sawn in two, and was buried below the fountain of Rogel, hard by the conduit of the waters which Hezekiah spoiled [for the enemy] by blocking their course."),
        new Scripture().setVersion("CCC").setBook("Lives of the Prophets").setChapter(1).setVerse(2).setText(
            "For the prophet’s sake God worked the miracle of Siloah; for before his death, in fainting condition he prayed for water, and it was sent to him from this source. Hence it was called Siloah, which means “sent.”"),
        new Scripture().setVersion("CCC").setBook("Lives of the Prophets").setChapter(1).setVerse(3).setText(
            "Also in the time of Hezekiah, before the king made the pools and the reservoirs, at the prayer of Isaiah a little water came forth here, lest the city, at that time besieged by the nations, should be destroyed through lack of water."),
        new Scripture().setVersion("CCC").setBook("Lives of the Prophets").setChapter(1).setVerse(4).setText(
            "For the enemies were seeking a drinking place, and as they surrounded the city, they encamped near Siloah. If then the Hebrews came to the pool, water flowed forth; if the nations came, there was none. Hence even to the present day the water issues suddenly, to keep the miracle in mind."),
        new Scripture().setVersion("CCC").setBook("Lives of the Prophets").setChapter(1).setVerse(5).setText(
            "Because this was worked through the prayer of Isaiah, the people in remembrance buried his body near the spot, with care and high honor, in order that through his prayers, even after his death, they might continue to have the benefit of the water. Indeed, a revelation had been given to them concerning him."));

    assertThat(scriptureStore.getScriptures("CCC", EN, "Prophets 23:1-58").getItems()).containsExactly(
        new Scripture().setVersion("CCC").setBook("Lives of the Prophets").setChapter(23).setVerse(1).setText(
            "Elisha was from Abel-Meholah, of the territory of Reuben."),
        new Scripture().setVersion("CCC").setBook("Lives of the Prophets").setChapter(23).setVerse(2).setText(
            "When he was born in Gilgal, a marvelous thing happened: the golden calf bellowed so loudly that the shrill sound was heard in Jerusalem;"),
        new Scripture().setVersion("CCC").setBook("Lives of the Prophets").setChapter(23).setVerse(3).setText(
            "and the priest announced by Lights and Perfections that a prophet had been born to Israel who should destroy their graven and molten idols."),
        new Scripture().setVersion("CCC").setBook("Lives of the Prophets").setChapter(23).setVerse(4).setText(
            "On his death he was buried in Samaria."));
  }

  @Test
  public void fetchGiants() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/dss_book_of_giants.htm"));

    assertThat(scriptureStore.getScriptures("DSS", EN, "Giants 1").getItems()).containsExactly(
        new Scripture().setVersion("DSS").setBook("Book of Giants").setChapter(1).setVerse(1).setText(
            "1Q23 Frag. 9 + 14 + 15 2[ . . . ] they knew the secrets of [ . . . ] 3[ . . . si]n was great in the earth [ . . . ] 4[ . . . ] and they killed manY [ . . ] 5[ . . . they begat] giants [ . . . ]"));

    assertThat(scriptureStore.getScriptures("DSS", EN, "Giants 4").getItems()).containsExactly(
        new Scripture().setVersion("DSS").setBook("Book of Giants").setChapter(4).setVerse(1).setText(
            "4Q531 Frag. 2 [ . . . ] they defiled [ . . . ] 2[ . . . they begot] giants and monsters [ . . . ] 3[ . . . ] they begot, and, behold, all [the earth was corrupted . . . ] 4[ . . . ] with its blood and by the hand of [ . . . ] 5[giant's] which did not suffice for them and [ . . . ] 6[ . . . ] and they were seeking to devour many [ . . . ] 7[ . . . ] 8[ . . . ] the monsters attacked it."),
        new Scripture().setVersion("DSS").setBook("Book of Giants").setChapter(4).setVerse(2).setText(
            "4Q532 Col. 2 Frags. 1 - 6 2[ . . . ] flesh [ . . . ] 3al[l . . . ] monsters [ . . . ] will be [ . . . ] 4[ . . . ] they would arise [ . . . ] lacking in true knowledge [ . . . ] because [ . . . ] 5[ . . . ] the earth [grew corrupt . . . ] mighty [ . . . ] 6[ . . . ] they were considering [ . . . ] 7[ . . . ] from the angels upon [ . . . ] 8[ . . . ] in the end it will perish and die [ . . . ] 9[ . . . ] they caused great corruption in the [earth . . . ] [ . . . this did not] suffice to [ . . . ] \"they will be [ . . . ]"));

    assertThat(scriptureStore.getScriptures("DSS", EN, "Giants 14").getItems()).containsExactly(
        new Scripture().setVersion("DSS").setBook("Book of Giants").setChapter(14).setVerse(1).setText(
            "4Q531 Frag. 7 3[ . . . great fear] seized me and I fell on my face; I heard his voice [ . . . ] 4[ . . . ] he dwelt among human beings but he did not learn from them [ . . . ]"));

  }

  @Test
  public void fetch2Baruch() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/main/webapp/books/2_baruch.txt"));

    assertThat(scriptureStore.getScriptures("DSS", EN, "2 Baruch 1").getItems()).containsExactly(
        new Scripture().setVersion("CCC").setBook("2 Baruch").setChapter(1).setVerse(1).setText(
            "And it happened in the twenty-fifth year of YeconiYah (Jeconiah), the king of Yahudah, that the Word of YAHWEH came to Baruch, the son of Neriah,"),
        new Scripture().setVersion("CCC").setBook("2 Baruch").setChapter(1).setVerse(2).setText(
            "And said to him: Have you seen all that this people are doing to ME, the evil things which the two tribes which remained have done more than the ten tribes which were carried away into captivity?"),
        new Scripture().setVersion("CCC").setBook("2 Baruch").setChapter(1).setVerse(3).setText(
            "For the former tribes were forced by their kings to sin, but these two have themselves forced and compelled their kings to sin."),
        new Scripture().setVersion("CCC").setBook("2 Baruch").setChapter(1).setVerse(4).setText(
            "Behold, therefore, I shall bring evil upon this city and its inhabitants. And it will be taken away from before MY presence for a time. And I shall scatter this people among the nations that they may be beneficial to the nations."),
        new Scripture().setVersion("CCC").setBook("2 Baruch").setChapter(1).setVerse(5).setText(
            "And MY people will be chastened, and the time will come that they will look for that which can make their times prosperous."));
  }

  @Test
  public void fetch1Clem() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/main/webapp/books/1_clem.txt"));

    assertThat(scriptureStore.getScriptures("DSS", EN, "1 Clem 3").getItems()).containsExactly(
        new Scripture().setVersion("CCC").setBook("1 Clement").setChapter(3).setVerse(1).setText(
            "All glory and enlargement was given unto you, and that was fulfilled which is written My beloved ate and drank and was enlarged and waxed fat and kicked."),
        new Scripture().setVersion("CCC").setBook("1 Clement").setChapter(3).setVerse(2).setText(
            "Hence come jealousy and envy, strife and sedition, persecution and tumult, war and captivity."),
        new Scripture().setVersion("CCC").setBook("1 Clement").setChapter(3).setVerse(3).setText(
            "So men were stirred up, the mean against the honorable, the ill reputed against the highly reputed, the foolish against the wise, the young against the elder."),
        new Scripture().setVersion("CCC").setBook("1 Clement").setChapter(3).setVerse(4).setText(
            "For this cause righteousness and peace stand aloof, while each man hath forsaken the fear of the Lord and become purblind in the faith of Him, neither walketh in the ordinances of His commandments nor liveth according to that which becometh Christ, but each goeth after the lusts of his evil heart, seeing that they have conceived an unrighteous and ungodly jealousy, through which also death entered into the world."));
  }

  @Test
  public void fetchOdes() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/main/webapp/books/odes_of_peace.txt"));

    assertThat(scriptureStore.getScriptures("DSS", EN, "Odes 37").getItems()).containsExactly(
        new Scripture().setVersion("CCC").setBook("Odes of Peace").setChapter(37).setVerse(1).setText(
            "I stretched out my hands to my Lord, || and to the Most High I raised my voice:"),
        new Scripture().setVersion("CCC").setBook("Odes of Peace").setChapter(37).setVerse(2).setText(
            "And I spoke with the lips of my heart, || and He heard me when my voice reached Him;"),
        new Scripture().setVersion("CCC").setBook("Odes of Peace").setChapter(37).setVerse(3).setText(
            "His answer came to me and gave me the fruits of my labors;"),
        new Scripture().setVersion("CCC").setBook("Odes of Peace").setChapter(37).setVerse(4).setText(
            "And it gave me rest by the grace of the Lord. Hallelujah!"));
  }

  @Test
  public void fetchIsaiahDSS() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(getContent(ISAIAH_SCROLL));

    assertThat(scriptureStore.getScriptures("DSS", HEBREW, "Isaiah 1:1-4,31,2:1-2")
        .getItems()).containsExactly(
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he")
            .setLocation("1QIsaA-I-1").setChapter(1).setVerse(1).setText(
                "חזון יש‸ע‸יהו בן אמוץ אשר חזה על יהודה וירושל‸י‸ם ‸ו‸בימי עוזיה יותם אחז ‸י‸חזקיה מלכי יהודה"),
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he")
            .setLocation("1QIsaA-I-2").setChapter(1).setVerse(2).setText(
                "שמעו שמים והאזינו הארץ כיא יהוה דבר בנים גדלתי ורו‸מ‸מת [ ] והמה פשעו בי"),
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he")
            .setLocation("1QIsaA-I-3").setChapter(1).setVerse(3).setText(
                "ידע שור ק‸י‸ונהו וחמור אבוס בעליו ישראל לוא ידע ועמי לוא [ ]ן"),
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he")
            .setLocation("1QIsaA-I-4").setChapter(1).setVerse(4).setText(
                "הוי גוי חוטה עם כבד עוון זרע מרעים בנים משחיתים עזבו את יה[ ] נא.ו את קדוש ישראל נזרו אחור"),
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he")
            .setLocation("1QIsaA-II-5").setChapter(1).setVerse(31).setText(
                "והיה החסנכם לנעורת ופעלכם לניצוץ ובערו שניהם יחדו ואין מכבה"),
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he")
            .setLocation("1QIsaA-II-7").setChapter(2).setVerse(1).setText(
                "הדבר אשר חזה ישעיה בן אמוץ על יהודה וירושלים"),
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he")
            .setLocation("1QIsaA-II-7").setChapter(2).setVerse(2).setText(
                "והיה באחרית הימים נכון יהיה הר בית יהוה בראש הרים ונשא מגבעות ונהרו עלוהי כול הגואים"));
  }

  @Test
  public void fetchIsaiahDSS_noVerseLeftBehind() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(getContent(ISAIAH_SCROLL));

    assertThat(scriptureStore.getScriptures("DSS", HEBREW, "Isaiah 45:24-46:1").getItems()).containsExactly(
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he").setLocation("1QIsaA-XXXIX-4").setChapter(45).setVerse(24).setText(
            "אך ביהוה ליא יאמר צדקות ועוז עדיו יבואו יבושו כול הנחרים בו"),
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he").setLocation("1QIsaA-XXXIX-5").setChapter(45).setVerse(25).setText(
            "ביהוה יצדקו ויתהללו כול זרע ישראל"),
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he").setLocation("1QIsaA-XXXIX-5").setChapter(46).setVerse(1).setText(
            "כרע בל קרס נבו היו עצביהמה לחיה לבהמה נשאותיכמה עמוסות משמיעיהמה"));
  }

  @Test
  public void fetchIsaiahDSS_noVerseLeftBehind64() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(getContent(ISAIAH_SCROLL));

    assertThat(scriptureStore.getScriptures("DSS", HEBREW, "Isaiah 63:19-64:2").getItems()).containsExactly(
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he").setLocation("1QIsaA-LI-14").setChapter(63).setVerse(19).setText(
            "היינו מעולם לוא משלתה בם לוא נ.רא שמכה עליהמה"),
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he").setLocation("1QIsaA-LI-15").setChapter(64).setVerse(1).setText(
            "לוא קרעתה שמים וירדתה מפניכה הרים נזלו"),
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he").setLocation("1QIsaA-LI-16").setChapter(64).setVerse(2).setText(
            "כקדוח אש עמסים מים תבעה אש לצריכה להודיע שמכה לצריכה ‸מ‸.פניכה גואים ירגזו"));
  }

  @Test
  public void fetchIsaiahDSS_duplicates() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(getContent(ISAIAH_SCROLL));

    assertThat(scriptureStore.getScriptures("DSS", HEBREW, "Isaiah 16:14").getItems()).containsExactly(
        new Scripture().setVersion("DSS").setBook("Isaiah").setLanguage("he").setLocation("1QIsaA-XIII-31").setChapter(16).setVerse(14).setText(
            "ועתה דבר י.וה לאמור בשלוש שנים כשני ש.יר ונקלה כבוד מואב בכול ההמון הרב ושאר מעט מ‸ז‸צער ולוא כבוד"));

    // Make sure there are no duplicates by reading in all verses into an ImmutableMap.
    ImmutableMap<String, String> all = scriptureStore.getScriptures("DSS", HEBREW, "Isaiah 1-66")
        .getItems()
        .stream()
        .collect(
            toImmutableMap(
                s -> String.format("Isaiah-%d-%d", s.getChapter(), s.getVerse()), s -> s.getText().toString()));

    assertThat(all).hasSize(1291);
  }

  @Test
  public void fetchJubileesOpenSiddur_Geez() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/opensiddur_org_jubilees_geez.html"));

    assertThat(scriptureStore.getScriptures("SID", GEEZ, "Jub 2:9").getItems()).containsExactly(
        new Scripture().setVersion("SID").setBook("Jubilees").setLanguage("gez").setChapter(2).setVerse(9).setText(
            "ወወሀበ እግዚአብሔር ፀሐየ ለትእምርት ዐቢይ ዲበ ምድር ለመዋዕል ወለሰንበት ወለአውራኅ ወለበዓላት ወለዓመታት ወለሰንበታተ ዓመታት ወለኢዮቤልዉሳት ወለኵሉ ጊዜ ለዓመታት።"));
  }

  @Test
  public void fetchGeezExperience_Psa117() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
            getContent("src/test/java/com/digitald4/biblical/util/data/geezexperience_tigrinya_Psa_117.json"));

    assertThat(scriptureStore.getScriptures("GzExp", GEEZ, "Psa 117").getItems()).containsExactly(
        new Scripture().setVersion("GzExp").setBook("Psalms").setLanguage("gez").setChapter(117).setVerse(1)
            .setText("ኵሉኹም ኣህዛብ፣ ንእግዚኣብሄር ኣመስግንዎ፣ ኵሉኹም ህዝብታት፣ ወድስዎ።"),
        new Scripture().setVersion("GzExp").setBook("Psalms").setLanguage("gez").setChapter(117).setVerse(2)
            .setText("ጸጋኡ ኣባና ዓብዪ እዩ እሞ፣ ሓቂ እግዚኣብሄር ድማ ንዘለአለም ይነብር እዩ። ሃሌሉያ።"));
  }

  @Test
  public void fetchGeezExperience_Isa2_9() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/geezexperience_tigrinya_Isa_9.json"));

    assertThat(scriptureStore.getScriptures("GzExp", GEEZ, "Isa 9:6").getItems()).containsExactly(
        new Scripture().setVersion("GzExp").setBook("Isaiah").setLanguage("gez").setChapter(9).setVerse(6).setText(
            "ሕጻን ተወሊዱልና፡ ወዲ ተውሂብና እዩ እሞ፡ እቲ ግዝኣት ከኣ ኣብ መንኲቡ እዩ፡ ስሙውን ግሩም፡ መካር፡ ብርቱዕ ኣምላኽ፡ ኣቦ ዘለኣለም፡ መስፍን ሰላም ኪስመ እዩ።"));
  }
}

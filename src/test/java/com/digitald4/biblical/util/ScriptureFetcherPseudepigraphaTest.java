package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.Language.EN;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

public class ScriptureFetcherPseudepigraphaTest extends ScriptureFetcherTest {
  private ScriptureFetcher scriptureFetcher;
  private BibleBook ENOCH = BibleBook.of(85, "Enoch", "Apocrypha,Ethiopian", 108, "Hanok", "Eno");

  @Before
  public void setup() {
    super.setup();
    scriptureFetcher = new ScriptureFetcherPseudepigrapha(apiConnector);
  }

  @Test
  public void fetchJubilees() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        """
            <html><head>
            <title>Jubilees 6</title>
            </head>
            <body>
            <h3><a href="index.htm">The Book of Jubilees</a></h3>
            <blockquote><em>
            Sacrifice of Noah, 1-3 (cf. Gen. vii.20-2). God's convenant with Noah, eating of blood forbidden,
            4-10 (cf. Gen. ix. 1-17). Moses bidden to renew this law against the eating of blood, 11-14.
            Bow set in the clouds for a sign, 15-16. Feast of weeks instituted, history of its observances, 17-22.
            Feast of the new moons, 23-8. Division of the year into 364 days, 29-38.
            </em></blockquote>
            <p>[Chapter 6]</p>
            <ol>
            <li>And on the new moon of the third month he went forth from the ark, and built an altar

            on\s

                that mountain.  </li><li>And he made atonement for the earth, and took a kid and made atonement by its blood for all the guilt of the earth; for everything that had been on it had been destroyed, save those that were in the ark with Noah.</li><li>And he placed the fat thereof on the altar, and he took an ox, and a goat, and a sheep and kids, and salt, and a turtle-dove, and the young of a dove,
            and placed a burnt sacrifice on the altar, and poured thereon an offering mingled with oil, and
            sprinkled wine and strewed frankincense over everything, and caused a goodly savour to arise,
            acceptable before the Lord.</li><li>And the Lord smelt the goodly savour, and He made a covenant with him that
             there should not be any more a flood to destroy the earth; that all the days of the earth seed-time
             and harvest should never cease; cold and heat, and summer and winter, and day and night should
             not change their order, nor cease for ever.  </li> <li>'And you, increase ye and multiply upon the
             earth, and become many upon it, and be a blessing upon it.  The fear of you and the dread of you I
             will
             inspire in everything that is on earth and in the sea.  </li></ol>
            <hr>
            Chapter: <a href="1.htm">1</a> | <a href="2.htm">2</a> | <a href="3.htm">3</a> |\s
            <a href="4.htm">4</a> | <a href="5.htm">5</a> | <a href="6.htm">6</a> | <a href="7.htm">7</a> |\s
            <a href="8.htm">8</a> | <a href="9.htm">9</a> | <a href="10.htm">10</a> | <a href="11.htm">11</a> | <a href="12.htm">12</a> | <a href="13.htm">13</a> |\s
            <a href="14.htm">14</a> | <a href="15.htm">15</a> | <a href="16.htm">16</a> | <a href="17.htm">17</a> | <a href="18.htm">18</a> | <a href="19.htm">19</a> |\s
            <a href="20.htm">20</a> | <a href="21.htm">21</a> | <a href="22.htm">22</a> | <a href="23.htm">23</a> | <a href="24.htm">24</a> | <a href="25.htm">25</a> |\s
            <a href="26.htm">26</a> | <a href="27.htm">27</a> | <a href="28.htm">28</a> | <a href="29.htm">29</a> | <a href="30.htm">30</a> | <a href="31.htm">31</a> |\s
            <a href="32.htm">32</a> | <a href="33.htm">33</a> | <a href="34.htm">34</a> | <a href="35.htm">35</a> | <a href="36.htm">36</a> | <a href="37.htm">37</a> |\s
            <a href="38.htm">38</a> | <a href="39.htm">39</a> | <a href="40.htm">40</a> | <a href="41.htm">41</a> | <a href="42.htm">42</a> | <a href="43.htm">43</a> |\s
            <a href="44.htm">44</a> | <a href="45.htm">45</a> | <a href="46.htm">46</a> | <a href="47.htm">47</a> | <a href="48.htm">48</a> | <a href="49.htm">49</a> |\s
            <a href="50.htm">50</a>\s
            <hr>
            <b>From <a href="../index.html">The Apocrypha and Pseudepigrapha of the Old Testament</a><br>
            by R.H. Charles, Oxford:  Clarendon Press, 1913<br>
            Scanned and Edited by Joshua Williams, Northwest Nazarene College</b>
            </body></html>""");

    assertThat(scriptureStore.getScriptures("OXFORD", EN, "JUBILEES 6").getItems()).containsExactly(
        new Scripture().setVersion("OXFORD").setBook("Jubilees").setChapter(6).setVerse(1).setText(
            "And on the new moon of the third month he went forth from the ark, and built an altar on that mountain."),
        new Scripture().setVersion("OXFORD").setBook("Jubilees").setChapter(6).setVerse(2).setText(
            "And he made atonement for the earth, and took a kid and made atonement by its blood for all the guilt of the earth; for everything that had been on it had been destroyed, save those that were in the ark with Noah."),
        new Scripture().setVersion("OXFORD").setBook("Jubilees").setChapter(6).setVerse(3).setText(
            "And he placed the fat thereof on the altar, and he took an ox, and a goat, and a sheep and kids, and salt, and a turtle-dove, and the young of a dove, and placed a burnt sacrifice on the altar, and poured thereon an offering mingled with oil, and sprinkled wine and strewed frankincense over everything, and caused a goodly savour to arise, acceptable before the Lord."),
        new Scripture().setVersion("OXFORD").setBook("Jubilees").setChapter(6).setVerse(4).setText(
            "And the Lord smelt the goodly savour, and He made a covenant with him that there should not be any more a flood to destroy the earth; that all the days of the earth seed-time and harvest should never cease; cold and heat, and summer and winter, and day and night should not change their order, nor cease for ever."),
        new Scripture().setVersion("OXFORD").setBook("Jubilees").setChapter(6).setVerse(5).setText(
            "'And you, increase ye and multiply upon the earth, and become many upon it, and be a blessing upon it. The fear of you and the dread of you I will inspire in everything that is on earth and in the sea."));
  }

  @Test
  public void fetchJubilees_poetry() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/Jubilees 19.html"));

    assertThat(scriptureStore.getScriptures("OXFORD", EN, "Jubilees 19:17").getItems()).containsExactly(
        new Scripture().setVersion("OXFORD").setBook("Jubilees").setChapter(19).setVerse(17).setText(
            "And he said unto her: My daughter, watch over my son Jacob, For he shall be in my stead on the earth, And for a blessing in the midst of the children of men, And for the glory of the whole seed of Shem."));
  }

  @Test
  public void fetchJasher() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/jasher.html"));

    assertThat(scriptureStore.getScriptures("OXFORD", EN, "Jasher 69").getItems()).containsExactly(
        new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(69).setVerse(1).setText(
            "And the king of Edom died in those days, in the eighteenth year of his reign, and was buried in his temple which he had built for himself as his royal residence in the land of Edom."),
        new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(69).setVerse(2).setText(
            "And the children of Esau sent to Pethor, which is upon the river, and they fetched from there a young man of beautiful eyes and comely aspect, whose name was Saul, and they made him king over them in the place of Samlah."),
        new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(69).setVerse(3).setText(
            "And Saul reigned over all the children of Esau in the land of Edom for forty years."),
        new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(69).setVerse(4).setText(
            "And when Pharaoh king of Egypt saw that the counsel which Balaam had advised respecting the children of Israel did not succeed, but that still they were fruitful, multiplied and increased throughout the land of Egypt,"),
        new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(69).setVerse(5).setText(
            "Then Pharaoh commanded in those days that a proclamation should be issued throughout Egypt to the children of Israel, saying, No man shall diminish any thing of his daily labor."),
        new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(69).setVerse(6).setText(
            "And the man who shall be found deficient in his labor which he performs daily, whether in mortar or in bricks, then his youngest son shall be put in their place."),
        new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(69).setVerse(7).setText(
            "And the labor of Egypt strengthened upon the children of Israel in those days, and behold if one brick was deficient in any man's daily labor, the Egyptians took his youngest boy by force from his mother, and put him into the building in the place of the brick which his father had left wanting."),
        new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(69).setVerse(8).setText(
            "And the men of Egypt did so to all the children of Israel day by day, all the days for a long period."),
        new Scripture().setVersion("OXFORD").setBook("Jasher").setChapter(69).setVerse(9).setText(
            "But the tribe of Levi did not at that time work with the Israelites their brethren, from the beginning, for the children of Levi knew the cunning of the Egyptians which they exercised at first toward the Israelites."));
  }

  @Test
  public void fetchEnoch() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/enoch.htm"));

    assertThat(scriptureStore.getScriptures("OXFORD", EN, "Enoch 2").getItems()).containsExactly(
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(2).setVerse(1).setText(
            "Observe ye everything that takes place in the heaven, how they do not change their orbits, and the luminaries which are in the heaven, how they all rise and set in order each in its season, and transgress not against their appointed order."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(2).setVerse(2).setText(
            "Behold ye the earth, and give heed to the things which take place upon it from first to last, how steadfast they are, how none of the things upon earth change, but all the works of God appear to you."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(2).setVerse(3).setText(
            "Behold the summer and the winter, how the whole earth is filled with water, and clouds and dew and rain lie upon it."));
  }

  @Test
  public void fetchEnoch_singleVerseChapter() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/enoch.htm"));

    assertThat(scriptureStore.getScriptures("OXFORD", EN, "Enoch 3").getItems()).containsExactly(
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(3).setVerse(1).setText(
            "Observe and see how (in the winter) all the trees seem as though they had withered and shed all their leaves, except fourteen trees, which do not lose their foliage but retain the old foliage from two to three years till the new comes."));
  }

  @Test
  public void fetchEnoch_poetry() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/enoch.htm"));

    assertThat(scriptureStore.getScriptures("OXFORD", EN, "Enoch 5").getItems()).containsAtLeast(
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(5).setVerse(1).setText(
            "Observe ye how the trees cover themselves with green leaves and bear fruit: wherefore give ye heed and know with regard to all His works, and recognize how He that liveth for ever hath made them so."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(5).setVerse(2).setText(
            "And all His works go on thus from year to year for ever, and all the tasks which they accomplish for Him, and their tasks change not, but according as God hath ordained so is it done."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(5).setVerse(3).setText(
            "And behold how the sea and the rivers in like manner accomplish and change not their tasks from His commandments'."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(5).setVerse(4).setText(
            "But ye--ye have not been steadfast, nor done the commandments of the Lord, But ye have turned away and spoken proud and hard words " +
            "With your impure mouths against His greatness. Oh, ye hard-hearted, ye shall find no peace."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(5).setVerse(5).setText(
            "Therefore shall ye execrate your days, And the years of your life shall perish, " +
                "And the years of your destruction shall be multiplied in eternal execration, And ye shall find no mercy."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(5).setVerse(6).setText(
            "In those days ye shall make your names an eternal execration unto all the righteous, And by you shall all who curse, curse, " +
                "And all the sinners and godless shall imprecate by you, And all the {. . .}shall rejoice, And there shall be forgiveness of sins, " +
                "And every mercy and peace and forbearance: There shall be salvation unto them, a goodly light. " +
                "And for all of you sinners there shall be no salvation, But on you all shall abide a curse."),
        new Scripture().setVersion("OXFORD").setBook("Enoch").setChapter(5).setVerse(7).setText(
            "And for you the godless there shall be a curse. But for the elect there shall be light and joy and peace, "
                + "And they shall inherit the earth."));
  }

  @Test
  public void fetchEnochOther() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/enoch1b.htm"));

    ImmutableList<Scripture> results = scriptureFetcher.fetch("Other", "en", ENOCH, 2);

    assertThat(results.stream().filter(s -> s.getChapter() == 2).collect(toImmutableList())).containsExactly(
        new Scripture().setVersion("Other").setBook("Enoch").setChapter(2).setVerse(1).setText(
            "Observe ye everything that takes place in the heaven, how they do not change their orbits, and the luminaries which are in the heaven, how they all rise and set in order each in its season, and"),
        new Scripture().setVersion("Other").setBook("Enoch").setChapter(2).setVerse(2).setText(
            "transgress not against their appointed order. Behold ye the earth, and give heed to the things which take place upon it from first to last, how steadfast they are, how none of the things upon earth"),
        new Scripture().setVersion("Other").setBook("Enoch").setChapter(2).setVerse(3).setText(
            "change, but all the works of God appear to you. Behold the summer and the winter, how the whole earth is filled with water, and clouds and dew and rain lie upon it."));
  }

  @Test
  public void fetchEnochOther_singleVerseChapter() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/enoch1b.htm"));

    ImmutableList<Scripture> results = scriptureFetcher.fetch("Other", "en", ENOCH, 3).stream()
        .filter(s -> s.getChapter() == 3).collect(toImmutableList());

    assertThat(results).containsExactly(
        new Scripture().setVersion("Other").setBook("Enoch").setChapter(3).setVerse(1).setText(
            "Observe and see how (in the winter) all the trees seem as though they had withered and shed all their leaves, except fourteen trees, which do not lose their foliage but retain the old foliage from two to three years till the new comes."));
  }

  @Test
  public void fetchEnochOther_poetry() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/enoch1b.htm"));

    assertThat(scriptureFetcher.fetch("Other", "en", ENOCH, 92)).containsAtLeast(
        new Scripture().setVersion("Other").setBook("Enoch").setChapter(92).setVerse(1).setText(
            "The book written by Enoch-Enoch indeed wrote this complete doctrine of wisdom, (which is) praised of all men and a judge of all the earth" +
            " for all my children who shall dwell on the earth. And for the future generations who shall observe uprightness and peace."),
        new Scripture().setVersion("Other").setBook("Enoch").setChapter(92).setVerse(2).setText(
            "Let not your spirit be troubled on account of the times; For the Holy and Great One has appointed days for all things."),
        new Scripture().setVersion("Other").setBook("Enoch").setChapter(92).setVerse(5).setText(
            "And sin shall perish in darkness for ever, And shall no more be seen from that day for evermore."));
  }

  @Test
  public void fetch2Enoch() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/enochs2.htm"));

    assertThat(scriptureStore.getScriptures("OXFORD", EN, "2 Enoch 7").getItems()).containsExactly(
        new Scripture().setVersion("OXFORD").setBook("2 Enoch").setChapter(7).setVerse(1).setText(
            "And those men took me and led me up on to the second heaven, and showed me darkness, greater than earthly darkness, and there I saw prisoners hanging, watched, awaiting the great and boundless judgment, and these angels (spirits) were dark-looking, more than earthly darkness, and incessantly making weeping through all hours."),
        new Scripture().setVersion("OXFORD").setBook("2 Enoch").setChapter(7).setVerse(2).setText(
            "And I said to the men who were with me: Wherefore are these incessantly tortured? They answered me: These are God�s apostates, who obeyed not God�s commands, but took counsel with their own will, and turned away with their prince, who also (is) fastened on the fifth heaven."),
        new Scripture().setVersion("OXFORD").setBook("2 Enoch").setChapter(7).setVerse(3).setText(
            "And I felt great pity for them, and they saluted me, and said to me: Man of God, pray for us to the Lord; and I answered to them: Who am I, a mortal man, that I should pray for angels (spirits)? Who knows whither I go, or what will befall me? Or who will pray for me?"));
  }

  @Test
  public void fetchBookOfAdamAndEve() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(getContent("src/test/java/com/digitald4/biblical/util/data/adamnev.htm"));

    assertThat(scriptureStore.getScriptures("OXFORD", EN, "BOOK OF ADAM AND EVE 3").getItems()).containsExactly(
        new Scripture().setVersion("OXFORD").setBook("Book of Adam and Eve").setChapter(3).setVerse(1).setText(
            "I And Adam arose and walked seven days over all that land, and found no victual such as they"),
        new Scripture().setVersion("OXFORD").setBook("Book of Adam and Eve").setChapter(3).setVerse(2).setText(
            "used to have in paradise. And Eve said to Adam: 'Wilt thou slay me? that I may die, and perchance God the Lord will bring thee into paradise, for on my account hast thou been driven thence.'"),
        new Scripture().setVersion("OXFORD").setBook("Book of Adam and Eve").setChapter(3).setVerse(3).setText(
            "Adam answered: 'Forbear, Eve, from such words, that peradventure God bring not some other curse upon us. How is it possible that I should stretch forth my hand against my own flesh? Nay, let us arise and look for something for us to live on, that we fail not.'"));

    assertThat(scriptureStore.getScriptures("OXFORD", EN, "BOOK OF ADAM AND EVE 15").getItems()).containsExactly(
        new Scripture().setVersion("OXFORD").setBook("Book of Adam and Eve").setChapter(15).setVerse(1).setText(
            "When the angels, who were under me, heard this, they refused to worship him. And Michael saith,"),
        new Scripture().setVersion("OXFORD").setBook("Book of Adam and Eve").setChapter(15).setVerse(2).setText(
            "'Worship the image of God, but if thou wilt not worship him, the Lord God will be wrath"),
        new Scripture().setVersion("OXFORD").setBook("Book of Adam and Eve").setChapter(15).setVerse(3).setText(
            "with thee.' And I said, 'If He be wrath with me, I will set my seat above the stars of heaven and will be like the Highest.'"));
  }
}

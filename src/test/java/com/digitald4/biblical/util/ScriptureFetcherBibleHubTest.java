package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.toList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Scripture;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ScriptureFetcherBibleHubTest extends ScriptureFetcherTest {
  private InterlinearFetcher interlinearFetcher;
  @Before
  public void setup() {
    super.setup();
    interlinearFetcher = new ScriptureFetcherBibleHub(apiConnector);
  }

  @Test
  public void fetch() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        "<html></body>\n" +
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

  @Test
  public void fetchSep() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_sep_exodus_12.htm"));

    assertThat(scriptureStore.getScriptures("SEP", Language.EN, "Exodus 12:40").getItems()).containsExactly(
        new Scripture().setVersion("SEP").setLanguage("en").setBook("Exodus").setChapter(12).setVerse(40)
            .setText("And the sojourning of the children of Israel, while they sojourned in the land of Egypt and the land of Chanaan, was four hundred and thirty years."));
  }

  @Test
  public void fetchInterlinear() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent(
            "src/test/java/com/digitald4/biblical/util/data/biblehub_interlinear_psalms_117.htm"));

    ImmutableList<Interlinear> interlinears =
        interlinearFetcher.fetchInterlinear(new BibleBook().setName("Psalms"), 117);

    assertThat(interlinears.stream().map(Interlinear::getStrongsId).collect(toList()))
        .containsExactly("H1984", "H0853", "H3068", "H3605", "H1471", "H7623", "H3605", "H0523",
            "H3588", "H1396", "H5921", "H2617", "H0571", "H3068", "H5769", "H1984", "H3050");

    assertThat(interlinears.stream().map(Interlinear::getTransliteration).collect(toList()))
        .containsExactly("hal·lū", "’eṯ-", "Yah·weh", "kāl-", "gō·w·yim;", "bə·ḥū·hū,", "kāl-",
            "hā·’um·mîm.", "kî", "ḡā·ḇar", "‘ā·lê·nū", "ḥas·dōw,", "we·’ĕ·meṯ-", "Yah·weh",
            "lə·‘ō·w·lām,", "hal·lū-", "yāh.");

    assertThat(interlinears.stream().map(Interlinear::getWord).collect(toList()))
        .containsExactly("הַֽלְל֣וּ", "אֶת־", "יְ֭הוָה", "כָּל־", "גּוֹיִ֑ם", "שַׁ֝בְּח֗וּהוּ", "כָּל־",
            "הָאֻמִּֽים", "כִּ֥י", "גָ֘בַ֤ר", "עָלֵ֨ינוּ ׀", "חַסְדּ֗וֹ", "וֶֽאֱמֶת־", "יְהוָ֥ה", "לְעוֹלָ֗ם", "הַֽלְלוּ־", "יָֽהּ");

    assertThat(interlinears.stream().map(Interlinear::getTranslation).collect(toList()))
        .containsExactly("Praise", "-", "Yahweh", "all", "you Gentiles", "Laud Him", "all",
            "you tribes", "For", "is great", "toward us", "His merciful kindness",
            "and the truth [endures]", "of Yahweh", "forever", "Praise", "YAH");

    assertThat(interlinears.stream().map(Interlinear::getMorphology).collect(toList()))
        .containsExactly("V‑Piel‑Imp‑mp", "DirObjM", "N‑proper‑ms", "N‑msc", "N‑mp",
            "V‑Piel‑Imp‑mp | 3ms", "N‑msc", "Art | N‑fp", "Conj", "V‑Qal‑Perf‑3ms", "Prep | 1cp",
            "N‑msc | 3ms", "Conj‑w | N‑fsc", "N‑proper‑ms", "Prep‑l | N‑ms", "V‑Piel‑Imp‑mp", "N‑proper‑ms");
  }

  @Test
  public void fetchInterlinear_noStrongs() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent(
            "src/test/java/com/digitald4/biblical/util/data/biblehub_interlinear_genesis_14.htm"));

    ImmutableList<Interlinear> interlinears =
        interlinearFetcher.fetchInterlinear(new BibleBook().setName("Genesis"), 14)
            .stream().filter(i -> i.getVerse() == 18).collect(toImmutableList());

    assertThat(interlinears).containsExactly(
        createInterlinear("Genesis", 14, 18, 1, "וּמַלְכִּי־", "ū·mal·kî-", null, "And"),
        createInterlinear("Genesis", 14, 18, 2, "צֶ֙דֶק֙", "ṣe·ḏeq", "H4442", "then Melchizedek"),
        createInterlinear("Genesis", 14, 18, 3, "מֶ֣לֶךְ", "me·leḵ", "H4428", "king"),
        createInterlinear("Genesis", 14, 18, 4, "שָׁלֵ֔ם", "šā·lêm,", "H8004", "of Salem"),
        createInterlinear("Genesis", 14, 18, 5, "הוֹצִ֖יא", "hō·w·ṣî", "H3318", "brought out"),
        createInterlinear("Genesis", 14, 18, 6, "לֶ֣חֶם", "le·ḥem", "H3899", "bread"),
        createInterlinear("Genesis", 14, 18, 7, "וָיָ֑יִן", "wā·yā·yin;", "H3196", "and wine"),
        createInterlinear("Genesis", 14, 18, 8, "וְה֥וּא", "wə·hū", "H1931", "since he"),
        createInterlinear("Genesis", 14, 18, 9, "כֹהֵ֖ן", "ḵō·hên", "H3548", "[was] priest"),
        createInterlinear("Genesis", 14, 18, 10, "לְאֵ֥ל", "lə·’êl", "H0410", "of God"),
        createInterlinear("Genesis", 14, 18, 11, "עֶלְיֽוֹן", "‘el·yō·wn.", "H5945", "Most High"));

    assertThat(interlinears.get(0).getMorphology()).isNull();
  }

  @Test
  public void getStrongsReferences_withNonTranslatedWord() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_interlinear_isaiah_7.htm"));

    ImmutableList<Interlinear> interlinears = interlinearFetcher
        .fetchInterlinear(new BibleBook().setName("Isaiah"), 7)
        .stream().filter(i -> i.getVerse() == 14).collect(toImmutableList());

    assertThat(interlinears).containsExactly(
        createInterlinear("Isaiah", 7, 14, 1, "לָ֠כֵן", "lā·ḵên", "H3651", "Therefore"),
        createInterlinear("Isaiah", 7, 14, 2, "יִתֵּ֨ן", "yit·tên", "H5414", "will give"),
        createInterlinear("Isaiah", 7, 14, 3, "אֲדֹנָ֥י", "’ă·ḏō·nāy", "H0136", "the Lord"),
        createInterlinear("Isaiah", 7, 14, 4, "ה֛וּא", "hū", "H1931", "He"),
        createInterlinear("Isaiah", 7, 14, 5, "לָכֶ֖ם", "lā·ḵem", null, "you"),
        createInterlinear("Isaiah", 7, 14, 6, "א֑וֹת", "’ō·wṯ;", "H0226", "a sign"),
        createInterlinear("Isaiah", 7, 14, 7, "הִנֵּ֣ה", "hin·nêh", "H2009", "behold"),
        createInterlinear("Isaiah", 7, 14, 8, "הָעַלְמָ֗ה", "hā·‘al·māh,", "H5959", "the virgin"),
        createInterlinear("Isaiah", 7, 14, 9, "הָרָה֙", "hā·rāh", "H2030", "shall become pregnant"),
        createInterlinear("Isaiah", 7, 14, 10, "וְיֹלֶ֣דֶת", "wə·yō·le·ḏeṯ", "H3205", "and bear"),
        createInterlinear("Isaiah", 7, 14, 11, "בֵּ֔ן", "bên,", "H1121", "a Son"),
        createInterlinear("Isaiah", 7, 14, 12, "וְקָרָ֥את", "wə·qā·rāṯ", "H7121", "and shall call"),
        createInterlinear("Isaiah", 7, 14, 13, "שְׁמ֖וֹ", "šə·mōw", "H8034", "His name"),
        createInterlinear("Isaiah", 7, 14, 14, "עִמָּ֥נוּ", "‘im·mā·nū", "H6005", "Immanuel"),
        createInterlinear("Isaiah", 7, 14, 15, "אֵֽל", "’êl.", null, null));

    assertThat(interlinears.get(0).getMorphology()).isEqualTo("Adv");
    // assertThat(interlinears.get(0).getMorphology()).isEqualTo("Adverb");
  }

  @Test
  public void getStrongsReferences_withReferenceText() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_interlinear_isaiah_53.htm"));

    ImmutableList<Interlinear> interlinears =
        interlinearFetcher.fetchInterlinear(new BibleBook().setName("Isaiah"), 53);

    assertThat(interlinears.stream().map(Interlinear::getVerse).distinct().collect(toList()))
        .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
  }

  @Test
  public void getStrongsReferences_withMissingVerseText() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_interlinear_isaiah_64.htm"));

    ImmutableList<Interlinear> interlinears =
        interlinearFetcher.fetchInterlinear(new BibleBook().setName("Isaiah"), 64);

    // There should be no verse 0.
    assertThat(interlinears.stream().map(Interlinear::getVerse).distinct().collect(toList()))
        .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    // Verse 1 should contain 7 words.
    assertThat(interlinears.stream().filter(i -> i.getVerse() == 1).map(Interlinear::getIndex).collect(toList()))
        .containsExactly(1, 2, 3, 4, 5, 6, 7);
  }

  @Test @Ignore
  public void getStrongsReferences_withMultiNonTranslatedWords() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_interlinear_isaiah_8.htm"));

    ImmutableList<Interlinear> interlinears = interlinearFetcher
        .fetchInterlinear(new BibleBook().setName("Isaiah"), 8)
        .stream().filter(i -> i.getVerse() == 3).collect(toImmutableList());

    assertThat(interlinears.stream().map(Interlinear::getStrongsId).collect(toList()))
        .containsExactly("H7126", "H0413", "H5031", "H2029", "H3205", "H1121", "H0559", "H3068",
            "H0413", "H7121", "H8034", null, "H4122", null, null);

    assertThat(interlinears.stream().map(Interlinear::getTranslation).collect(toList()))
        .containsExactly("And I went", "to", "the prophetess", "and she conceived", "and bore", "a son",
            "And said", "Yahweh", "to me", "Call", "his name", "from", "Maher-shalal-hash-baz", "-", null);

    assertThat(interlinears.stream().map(Interlinear::getMorphology).collect(toList()))
        .containsExactly("Conj-w | V-Qal-ConsecImperf-1cs", "Prep", "Art | N-fs", "Conj-w | V-Qal-ConsecImperf-3fs",
            "Conj-w | V-Qal-ConsecImperf-3fs", "N-ms", "Conj-w | V-Qal-ConsecImperf-3ms", "N-proper-ms",
            "Prep | 1cs", "V-Qal-Imp-ms", "N-msc | 3ms", "Prep", null, null, "N-proper-ms");
  }

  @Test
  public void fetchGreekInterlinear() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent(
            "src/test/java/com/digitald4/biblical/util/data/biblehub_interlinear_matt_5.htm"));

    ImmutableList<Interlinear> interlinears =
        interlinearFetcher.fetchInterlinear(new BibleBook().setName("Matthew").setNumber(40), 5)
            .stream().filter(i -> i.getVerse() == 17).collect(toImmutableList());

    assertThat(interlinears).containsExactly(
        createInterlinear("Matthew", 5, 17, 1, "Μὴ", "Mē", "G3361", "Not"),
        createInterlinear("Matthew", 5, 17, 2, "νομίσητε", "nomisēte", "G3543", "think"),
        createInterlinear("Matthew", 5, 17, 3, "ὅτι", "hoti", "G3754", "that"),
        createInterlinear("Matthew", 5, 17, 4, "ἦλθον", "ēlthon", "G2064", "I have come"),
        createInterlinear("Matthew", 5, 17, 5, "καταλῦσαι", "katalysai", "G2647", "to abolish"),
        createInterlinear("Matthew", 5, 17, 6, "τὸν", "ton", "G3588", "the"),
        createInterlinear("Matthew", 5, 17, 7, "νόμον", "nomon", "G3551", "law"),
        createInterlinear("Matthew", 5, 17, 8, "ἢ", "ē", "G2228", "or"),
        createInterlinear("Matthew", 5, 17, 9, "τοὺς", "tous", "G3588", "the"),
        createInterlinear("Matthew", 5, 17, 10, "προφήτας", "prophētas", "G4396", "Prophets"),
        createInterlinear("Matthew", 5, 17, 11, "οὐκ", "ouk", "G3756", "not"),
        createInterlinear("Matthew", 5, 17, 12, "ἦλθον", "ēlthon", "G2064", "I have come"),
        createInterlinear("Matthew", 5, 17, 13, "καταλῦσαι", "katalysai", "G2647", "to abolish"),
        createInterlinear("Matthew", 5, 17, 14, "ἀλλὰ", "alla", "G0235", "but"),
        createInterlinear("Matthew", 5, 17, 15, "πληρῶσαι", "plērōsai", "G4137", "to fulfill"));

    assertThat(interlinears.get(0).getMorphology()).isEqualTo("Adv");
  }

  public static Interlinear createInterlinear(String book, int chapter, int verse,
      int index, String word, String transliteration, String strongsId, String translation) {
    return new Interlinear().setBook(book).setChapter(chapter).setVerse(verse)
        .setIndex(index).setWord(word).setTransliteration(transliteration).setStrongsId(strongsId)
        .setConstantsOnly(HebrewConverter.toConstantsOnly(word))
        .setTranslation(translation);
  }
}

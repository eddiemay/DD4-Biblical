package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.ScriptureFetcherTest.getContent;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Lexicon.Node;
import com.digitald4.biblical.model.Lexicon.TranslationCount;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class LexiconFetcherBlueLetterImplTest {
  @Mock private final APIConnector apiConnector = mock(APIConnector.class);
  private LexiconFetcher lexiconFetcher;

  @Before
  public void setup() {
    lexiconFetcher = new LexiconFetcherBlueLetterImpl(apiConnector, false);
  }

  @Test
  public void fetch() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/blueletterbible_lexicon.html"));

    Lexicon lexicon = lexiconFetcher.getLexicon("H410");

    assertThat(lexicon).isEqualTo(
        new Lexicon()
            .setId("H410")
            .setWord("אֵל")
            .setConstantsOnly("אל")
            .setTransliteration("'ēl")
            .setPronunciation("ale")
            .setPartOfSpeech("masculine noun")
            .setRootWord("Shortened from אַיִל (<a href=\"\" data-ng-click=\"$ctrl.showStrongsDefs('H352')\">H352</a>)")
            .setDictionaryAid("TWOT Reference: 93a")
            .setTranslationCounts(
                ImmutableList.of(
                    new TranslationCount().setWord("God").setCount(213),
                    new TranslationCount().setWord("god").setCount(16),
                    new TranslationCount().setWord("power").setCount(4),
                    new TranslationCount().setWord("mighty").setCount(5),
                    new TranslationCount().setWord("goodly").setCount(1),
                    new TranslationCount().setWord("great").setCount(1),
                    new TranslationCount().setWord("idols").setCount(1),
                    new TranslationCount().setWord("Immanuel (with H6005)").setCount(2),
                    new TranslationCount().setWord("might").setCount(1),
                    new TranslationCount().setWord("strong").setCount(1)))
            .setOutline(
                ImmutableList.of(
                    new Node().setValue("god, god-like one, mighty one").setChildren(
                        ImmutableList.of(
                            new Node().setValue("mighty men, men of rank, mighty heroes"),
                            new Node().setValue("angels"),
                            new Node().setValue("god, false god, (demons, imaginations)"),
                            new Node().setValue("God, the one true God, Jehovah"))),
                    new Node().setValue("mighty things in nature"),
                    new Node().setValue("strength, power"))));

    assertThat(lexicon.getStrongsDefinition().toString()).isEqualTo(
            "אֵל ʼêl, ale; shortened from <a href=\"\" data-ng-click=\"$ctrl.showStrongsDefs('H352')\">H352</a>; strength; as adjective, mighty; especially the Almighty (but used also of any deity):—God (god), × goodly, × great, idol, might(-y one), power, strong. Compare names in '-el.'");
    assertThat(lexicon.translation()).isEqualTo("God");
  }

  @Test
  public void fetchInterlinear_byVerse() throws Exception {
    when(apiConnector.sendGet(anyString()))
        .thenReturn(
            getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_text_genesis_1-1.htm"))
        .thenThrow(new DD4StorageException("Not found", ErrorCode.NOT_FOUND));

    ImmutableList<Interlinear> interlinears = lexiconFetcher
        .fetchInterlinear(new BibleBook().setName("Genesis"), 1, 1);

    assertThat(interlinears).containsExactly(
        createInterlinear("Genesis", 1, 1, 1, "בְּרֵאשִׁ֖ית", "bə-rê-šîṯ", "H7225", "In the beginning"),
        createInterlinear("Genesis", 1, 1, 2, "בָּרָ֣א", "bā-rā", "H1254", "created"),
        createInterlinear("Genesis", 1, 1, 3, "אֱלֹהִ֑ים", "’ĕ-lō-hîm;", "H430", "God"),
        createInterlinear("Genesis", 1, 1, 4, "אֵ֥ת", "’êṯ", "H853", "-"),
        createInterlinear("Genesis", 1, 1, 5, "הַשָּׁמַ֖יִם", "haš-šā-ma-yim", "H8064", "the heavens"),
        createInterlinear("Genesis", 1, 1, 6, "וְאֵ֥ת", "wə-’êṯ", "H853", "and"),
        createInterlinear("Genesis", 1, 1, 7, "הָאָֽרֶץ", "hā-’ā-reṣ.", "H776", "the earth"));

    assertThat(interlinears.get(0).getMorphology()).isEqualTo("Prep-b | N-fs");
    // assertThat(interlinears.get(0).getMorphology()).isEqualTo("Preposition-b :: Noun - feminine singular");
  }

  @Test
  public void fetchInterlinear_byChapter() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent(
            "src/test/java/com/digitald4/biblical/util/data/biblehub_interlinear_psalms_117.htm"));

    ImmutableList<Interlinear> interlinears =
        lexiconFetcher.fetchInterlinear(new BibleBook().setName("Psalms"), 117);

    assertThat(interlinears.stream().map(Interlinear::getStrongsId).collect(toList()))
        .containsExactly("H1984", "H853", "H3068", "H3605", "H1471", "H7623", "H3605", "H523",
            "H3588", "H1396", "H5921", "H2617", "H571", "H3068", "H5769", "H1984", "H3050");

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
  public void fetchInterlinear_byVerse_noStrongs() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_text_genesis_14-18.htm"));

    ImmutableList<Interlinear> interlinears =
        lexiconFetcher.fetchInterlinear(new BibleBook().setName("Genesis"), 14, 18);

    assertThat(interlinears).containsExactly(
        createInterlinear("Genesis", 14, 18, 1, "וּמַלְכִּי־", "ū-mal-kî-", null, "And"),
        createInterlinear("Genesis", 14, 18, 2, "צֶ֙דֶק֙", "ṣe-ḏeq", "H4442", "then Melchizedek"),
        createInterlinear("Genesis", 14, 18, 3, "מֶ֣לֶךְ", "me-leḵ", "H4428", "king"),
        createInterlinear("Genesis", 14, 18, 4, "שָׁלֵ֔ם", "šā-lêm,", "H8004", "of Salem"),
        createInterlinear("Genesis", 14, 18, 5, "הוֹצִ֖יא", "hō-w-ṣî", "H3318", "brought out"),
        createInterlinear("Genesis", 14, 18, 6, "לֶ֣חֶם", "le-ḥem", "H3899", "bread"),
        createInterlinear("Genesis", 14, 18, 7, "וָיָ֑יִן", "wā-yā-yin;", "H3196", "and wine"),
        createInterlinear("Genesis", 14, 18, 8, "וְה֥וּא", "wə-hū", "H1931", "since he"),
        createInterlinear("Genesis", 14, 18, 9, "כֹהֵ֖ן", "ḵō-hên", "H3548", "[was] priest"),
        createInterlinear("Genesis", 14, 18, 10, "לְאֵ֥ל", "lə-’êl", "H410", "of God"),
        createInterlinear("Genesis", 14, 18, 11, "עֶלְיֽוֹן", "‘el-yō-wn.", "H5945", "Most High"));

    assertThat(interlinears.get(0).getMorphology()).isNull();
  }

  @Test
  public void fetchInterlinear_byChapter_noStrongs() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent(
            "src/test/java/com/digitald4/biblical/util/data/biblehub_interlinear_genesis_14.htm"));

    ImmutableList<Interlinear> interlinears =
        lexiconFetcher.fetchInterlinear(new BibleBook().setName("Genesis"), 14)
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
        createInterlinear("Genesis", 14, 18, 10, "לְאֵ֥ל", "lə·’êl", "H410", "of God"),
        createInterlinear("Genesis", 14, 18, 11, "עֶלְיֽוֹן", "‘el·yō·wn.", "H5945", "Most High"));

    assertThat(interlinears.get(0).getMorphology()).isNull();
  }

  @Test
  public void getStrongsReferences_withNonTranslatedWord() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_text_isaiah_7-14.htm"));

    ImmutableList<Interlinear> interlinears =
        lexiconFetcher.fetchInterlinear(new BibleBook().setName("Isaiah"), 7, 14);

    assertThat(interlinears).containsExactly(
        createInterlinear("Isaiah", 7, 14, 1, "לָ֠כֵן", "lā-ḵên", "H3651", "Therefore"),
        createInterlinear("Isaiah", 7, 14, 2, "יִתֵּ֨ן", "yit-tên", "H5414", "will give"),
        createInterlinear("Isaiah", 7, 14, 3, "אֲדֹנָ֥י", "’ă-ḏō-nāy", "H136", "the Lord"),
        createInterlinear("Isaiah", 7, 14, 4, "ה֛וּא", "hū", "H1931", "He"),
        createInterlinear("Isaiah", 7, 14, 5, "לָכֶ֖ם", "lā-ḵem", null, "you"),
        createInterlinear("Isaiah", 7, 14, 6, "א֑וֹת", "’ō-wṯ;", "H226", "a sign"),
        createInterlinear("Isaiah", 7, 14, 7, "הִנֵּ֣ה", "hin-nêh", "H2009", "behold"),
        createInterlinear("Isaiah", 7, 14, 8, "הָעַלְמָ֗ה", "hā-‘al-māh,", "H5959", "the virgin"),
        createInterlinear("Isaiah", 7, 14, 9, "הָרָה֙", "hā-rāh", "H2030", "shall become pregnant"),
        createInterlinear("Isaiah", 7, 14, 10, "וְיֹלֶ֣דֶת", "wə-yō-le-ḏeṯ", "H3205", "and bear"),
        createInterlinear("Isaiah", 7, 14, 11, "בֵּ֔ן", "bên,", "H1121", "a Son"),
        createInterlinear("Isaiah", 7, 14, 12, "וְקָרָ֥את", "wə-qā-rāṯ", "H7121", "and shall call"),
        createInterlinear("Isaiah", 7, 14, 13, "שְׁמ֖וֹ", "šə-mōw", "H8034", "His name"),
        createInterlinear("Isaiah", 7, 14, 14, "עִמָּ֥נוּ", "‘im-mā-nū", "H6005", "Immanuel"),
        createInterlinear("Isaiah", 7, 14, 15, "אֵֽל", "’êl.", null, null));

    assertThat(interlinears.get(0).getMorphology()).isEqualTo("Adv");
    // assertThat(interlinears.get(0).getMorphology()).isEqualTo("Adverb");
  }

  @Test
  public void getStrongsReferences_byChapter_withNonTranslatedWord() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_interlinear_isaiah_7.htm"));

    ImmutableList<Interlinear> interlinears =
        lexiconFetcher.fetchInterlinear(new BibleBook().setName("Isaiah"), 7)
            .stream().filter(i -> i.getVerse() == 14).collect(toImmutableList());

    assertThat(interlinears).containsExactly(
        createInterlinear("Isaiah", 7, 14, 1, "לָ֠כֵן", "lā·ḵên", "H3651", "Therefore"),
        createInterlinear("Isaiah", 7, 14, 2, "יִתֵּ֨ן", "yit·tên", "H5414", "will give"),
        createInterlinear("Isaiah", 7, 14, 3, "אֲדֹנָ֥י", "’ă·ḏō·nāy", "H136", "the Lord"),
        createInterlinear("Isaiah", 7, 14, 4, "ה֛וּא", "hū", "H1931", "He"),
        createInterlinear("Isaiah", 7, 14, 5, "לָכֶ֖ם", "lā·ḵem", null, "you"),
        createInterlinear("Isaiah", 7, 14, 6, "א֑וֹת", "’ō·wṯ;", "H226", "a sign"),
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
  public void getStrongsReferences_withMultiNonTranslatedWords() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/biblehub_text_isaiah_8-3.htm"));

    ImmutableList<Interlinear> interlinears =
        lexiconFetcher.fetchInterlinear(new BibleBook().setName("Isaiah"), 8, 3);

    assertThat(interlinears.stream().map(Interlinear::getStrongsId).collect(toList()))
        .containsExactly("H7126", "H413", "H5031", "H2029", "H3205", "H1121", "H559", "H3068",
            "H413", "H7121", "H8034", null, "H4122", null, null);

    assertThat(interlinears.stream().map(Interlinear::getTranslation).collect(toList()))
        .containsExactly("And I went", "to", "the prophetess", "and she conceived", "and bore", "a son",
            "And said", "Yahweh", "to me", "Call", "his name", "from", "Maher-shalal-hash-baz", "-", null);

    assertThat(interlinears.stream().map(Interlinear::getMorphology).collect(toList()))
        .containsExactly("Conj-w | V-Qal-ConsecImperf-1cs", "Prep", "Art | N-fs", "Conj-w | V-Qal-ConsecImperf-3fs",
            "Conj-w | V-Qal-ConsecImperf-3fs", "N-ms", "Conj-w | V-Qal-ConsecImperf-3ms", "N-proper-ms",
            "Prep | 1cs", "V-Qal-Imp-ms", "N-msc | 3ms", "Prep", null, null, "N-proper-ms");
  }

  @Test
  public void processScriptureReferences() {
    assertThat(LexiconFetcherBlueLetterImpl.processScriptureReferences("Text with no references"))
        .isEqualTo("Text with no references");
    assertThat(LexiconFetcherBlueLetterImpl.processScriptureReferences("Reference to Isaiah 2:3"))
        .isEqualTo("Reference to <scripture ref=\"Isaiah 2:3\"/>");
    assertThat(LexiconFetcherBlueLetterImpl.processScriptureReferences("Reference to 1 John 3:4"))
        .isEqualTo("Reference to <scripture ref=\"1 John 3:4\"/>");
    assertThat(LexiconFetcherBlueLetterImpl.processScriptureReferences("A Isaiah 2:3 reference"))
        .isEqualTo("A <scripture ref=\"Isaiah 2:3\"/> reference");
    assertThat(LexiconFetcherBlueLetterImpl.processScriptureReferences("A 1 John 3:4 reference"))
        .isEqualTo("A <scripture ref=\"1 John 3:4\"/> reference");
  }

  private static Interlinear createInterlinear(String book, int chapter, int verse, int index,
      String word, String transliteration, String strongsId, String translation) {
    return new Interlinear().setBook(book).setChapter(chapter).setVerse(verse).setIndex(index)
        .setWord(word).setTransliteration(transliteration).setStrongsId(strongsId)
        .setConstantsOnly(HebrewConverter.toConstantsOnly(word))
        .setTranslation(translation);
  }
}

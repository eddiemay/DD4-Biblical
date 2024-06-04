package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.ScriptureFetcherTest.getContent;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.model.Lexicon.Node;
import com.digitald4.biblical.model.Lexicon.TranslationCount;
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
    lexiconFetcher = new LexiconFetcherBlueLetterImpl(apiConnector);
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
            .setStrongsDefinition("אֵל ʼêl, ale; shortened from <a href=\"\" data-ng-click=\"$ctrl.showStrongsDef('H352')\">H352</a>; strength; as adjective, mighty; especially the Almighty (but used also of any deity):—God (god), × goodly, × great, idol, might(-y one), power, strong. Compare names in '-el.'")
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

    assertThat(lexicon.translation()).isEqualTo("God");
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
}

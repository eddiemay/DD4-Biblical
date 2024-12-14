package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord.TokenType;
import com.digitald4.common.storage.DAOInMemoryImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class MachineTranslatorTest {
  private static final DAOInMemoryImpl inMemoryDao = new DAOInMemoryImpl();
  private static final ImmutableList<TokenWord> TOKEN_WORDS = ImmutableList.of(
      new TokenWord().setWord("ב").setTranslation("in ").setTokenType(TokenType.PREFIX_ONLY),
      new TokenWord().setWord("ה").setTranslation("the ").setAsSuffix("").setTokenType(TokenType.PREFIX),
      new TokenWord().setWord("ו").setTranslation("and ").setTokenType(TokenType.PREFIX), // .setTransliteration("wa"),
      new TokenWord().setWord("י").setTranslation("he ").setTokenType(TokenType.PREFIX).setAsSuffix("of me"),
      new TokenWord().setWord("ימ").setTranslation("s").setTokenType(TokenType.SUFFIX_ONLY),
      new TokenWord().setWord("כ").setTranslation("like ").setAsSuffix(" you").setTokenType(TokenType.PREFIX),
      new TokenWord().setWord("ל").setTranslation("to ").setTokenType(TokenType.PREFIX),
      new TokenWord().setWord("ש").setTranslation("that which ").setTokenType(TokenType.PREFIX_ONLY),
      new TokenWord().setWord("ת").setTranslation("you ").setTokenType(TokenType.PREFIX).setAsSuffix("s"),
      new TokenWord().setWord("את").setTranslation("you").setStrongsId("H0853"),
      new TokenWord().setWord("ראשית").setTranslation("beginning").setStrongsId("H7221"),
      new TokenWord().setWord("ברא").setTranslation("created").setStrongsId("H1254"),
      new TokenWord().setWord("אלוה").setTranslation("Mighty one").setStrongsId("H0433"),
      new TokenWord().setWord("אלה").setTranslation("Mighty one").setStrongsId("H0430"),
      new TokenWord().setWord("שמימ").setTranslation("heavens").setStrongsId("H8064"),
      new TokenWord().setWord("ארצ").setTranslation("earth").setStrongsId("H0776"),
      new TokenWord().setWord("יע").setTranslation("shovel").setStrongsId("H3257"),
      new TokenWord().setWord("עש").setTranslation("made").setStrongsId("H6213"),
      new TokenWord().setWord("זכור").setTranslation("remember").setStrongsId("H2142"),
      new TokenWord().setWord("מצוה").setTranslation("commandment").setStrongsId("H4687"),
      new TokenWord().setWord("אשר").setTranslation("which").setStrongsId("H0834"),
      new TokenWord().setWord("נתנ").setTranslation("given").setStrongsId("H5414"),
      new TokenWord().setWord("ה'").setTranslation("Yah").setStrongsId("H3050").setTransliteration("Yah"),
      new TokenWord().setWord("לכ").setTranslation("to you").setStrongsId("H0853"),
      new TokenWord().setWord("μακαριοι").setTranslation("blessed are").setStrongsId("G3107"),
      new TokenWord().setWord("οι").setTranslation("the").setStrongsId("G3588"),
      new TokenWord().setWord("πτωχοι").setTranslation("poor").setStrongsId("G4434"),
      new TokenWord().setWord("τω").setTranslation("to").setStrongsId("G3588"),
      new TokenWord().setWord("πνευματι").setTranslation("spirit").setStrongsId("G4151"),
      new TokenWord().setWord("οτι").setTranslation("that").setStrongsId("G3754"),
      new TokenWord().setWord("αυτων").setTranslation("them").setStrongsId("G0846"),
      new TokenWord().setWord("εστιν").setTranslation("to be").setStrongsId("G1510"),
      new TokenWord().setWord("η").setTranslation("or").setStrongsId("G22s8"),
      new TokenWord().setWord("βασιλεια").setTranslation("Kingdom").setStrongsId("G0932"),
      new TokenWord().setWord("των").setTranslation("to the").setStrongsId("G3588"),
      new TokenWord().setWord("ουρανων").setTranslation("heavens").setStrongsId("G3772"),
      new TokenWord().setWord("תורה").setTranslation("law").setStrongsId("H8451"),
      new TokenWord().setWord("צוה").setTranslation("command").setStrongsId("H6680"),
      new TokenWord().setWord("ל").setTranslation("to").setTokenType(TokenType.PREFIX),
      new TokenWord().setWord("נו").setTranslation("of us"),
      new TokenWord().setWord("מושה").setTranslation("Moses").setStrongsId("H4872"),
      new TokenWord().setWord("מורשה").setTranslation("possession").setStrongsId("H4181"),
      new TokenWord().setWord("קהל").setStrongsId("H6952").setTranslation("congregation"),
      new TokenWord().setWord("יעקוב").setStrongsId("H3290").setTranslation("Jacob"));

  private final TokenWordStore tokenWordStore =
      new TokenWordStore(() -> TOKEN_WORDS, ImmutableMap::of);
  private final MachineTranslator machineTranslator =
      new MachineTranslator(tokenWordStore, new HebrewTokenizer(tokenWordStore));

  @Test
  public void tokenize_inBeginning() {
    assertThat(machineTranslator.translate("בראשית").get(0).getSubTokens()).containsExactly(
        new SubToken().setWord("ב").setTranslation("in ").setTransliteration("ba"),
        new SubToken().setWord("ראשית").setTranslation("beginning").setStrongsId("H7221").setTransliteration("rashyt"));
  }

  @Test
  public void tokenize_created() {
    assertThat(machineTranslator.translate("ברא").get(0).getSubTokens()).containsExactly(
        new SubToken().setWord("ברא").setTranslation("created").setStrongsId("H1254").setTransliteration("bara"));
  }

  @Test
  public void tokenize_andHeMade() {
    assertThat(machineTranslator.translate("ויעש").get(0).getSubTokens()).containsExactly(
        new SubToken().setWord("ו").setTranslation("and ").setTransliteration("wa"),
        new SubToken().setWord("י").setTranslation("he ").setTransliteration("ya"),
        new SubToken().setWord("עש").setTranslation("made").setStrongsId("H6213").setTransliteration("ish"));
  }

  @Test
  public void translate_Gen_1_1() {
    ImmutableList<Interlinear> translation = machineTranslator.translate("בְּרֵאשִׁ֖ית בָּרָ֣א אֱלֹהִ֑ים אֵ֥ת הַשָּׁמַ֖יִם וְאֵ֥ת הָאָֽרֶץ׃");
    assertThat(translation.stream().flatMap(i -> i.getSubTokens().stream()).collect(toImmutableList())).containsExactly(
        new SubToken().setWord("ב").setTranslation("in ").setTransliteration("ba"),
        new SubToken().setWord("ראשית").setTranslation("beginning").setStrongsId("H7221").setTransliteration("rashyt"),
        new SubToken().setWord("ברא").setTranslation("created").setStrongsId("H1254").setTransliteration("bara"),
        new SubToken().setWord("אלוה").setTranslation("Mighty one").setStrongsId("H0433").setTransliteration("aluh"),
        new SubToken().setWord("ימ").setTranslation("s").setTransliteration("ym"),
        new SubToken().setWord("את").setTranslation("you").setStrongsId("H0853").setTransliteration("at"),
        new SubToken().setWord("ה").setTranslation("the ").setTransliteration("ha"),
        new SubToken().setWord("שמימ").setTranslation("heavens").setStrongsId("H8064").setTransliteration("shamym"),
        new SubToken().setWord("ו").setTranslation("and ").setTransliteration("wa"),
        new SubToken().setWord("את").setTranslation("you").setStrongsId("H0853").setTransliteration("at"),
        new SubToken().setWord("ה").setTranslation("the ").setTransliteration("ha"),
        new SubToken().setWord("ארצ").setTranslation("earth").setStrongsId("H0776").setTransliteration("aratz"));
  }

  @Test
  public void translate_Deut_33_4() {
    ImmutableList<Interlinear> translation = machineTranslator.translate("תֹּורָ֥ה צִוָּה־לָ֖נוּ מֹשֶׁ֑ה מֹורָשָׁ֖ה קְהִלַּ֥ת יַעֲקֹֽב׃");
    assertThat(translation.stream().flatMap(i -> i.getSubTokens().stream()).collect(toImmutableList())).containsExactly(
        new SubToken().setWord("תורה").setTranslation("law").setStrongsId("H8451").setTransliteration("turah"),
        new SubToken().setWord("צוה").setTranslation("command").setStrongsId("H6680").setTransliteration("tzuh"),
        new SubToken().setWord("ל").setTranslation("to ").setTransliteration("la"),
        new SubToken().setWord("נו").setTranslation("of us").setTransliteration("nu"),
        new SubToken().setWord("מושה").setTranslation("Moses").setStrongsId("H4872").setTransliteration("mushah"),
        new SubToken().setWord("מורשה").setTranslation("possession").setStrongsId("H4181").setTransliteration("murashah"),
        new SubToken().setWord("קהל").setStrongsId("H6952").setTranslation("congregation").setTransliteration("qahal"),
        new SubToken().setWord("ת").setTranslation("s").setTransliteration("ta"),
        new SubToken().setWord("יעקוב").setStrongsId("H3290").setTranslation("Jacob").setTransliteration("yiqub"));
  }

  @Test
  public void translate_Jub_49_1() {
    ImmutableList<Interlinear> translation = machineTranslator.translate("זכור את המצוה אשר נתן ה' לך");
    assertThat(translation.stream().flatMap(i -> i.getSubTokens().stream()).collect(toImmutableList())).containsExactly(
        new SubToken().setWord("זכור").setTranslation("remember").setStrongsId("H2142").setTransliteration("zacur"),
        new SubToken().setWord("את").setTranslation("you").setStrongsId("H0853").setTransliteration("at"),
        new SubToken().setWord("ה").setTranslation("the ").setTransliteration("ha"),
        new SubToken().setWord("מצוה").setTranslation("commandment").setStrongsId("H4687").setTransliteration("matzuh"),
        new SubToken().setWord("אשר").setTranslation("which").setStrongsId("H0834").setTransliteration("ashar"),
        new SubToken().setWord("נתנ").setTranslation("given").setStrongsId("H5414").setTransliteration("natan"),
        new SubToken().setWord("ה'").setTranslation("Yah").setStrongsId("H3050").setTransliteration("Yah"),
        new SubToken().setWord("לכ").setTranslation("to you").setStrongsId("H0853").setTransliteration("lac"));
  }

  @Test
  public void translate_matt_5_3() {
    var translation = machineTranslator.translate("Μακάριοι οἱ πτωχοὶ τῷ πνεύματι, ὅτι αὐτῶν ἐστιν ἡ βασιλεία τῶν οὐρανῶν.");
    assertThat(translation.stream().flatMap(i -> i.getSubTokens().stream()).collect(toImmutableList())).containsExactly(
        new SubToken().setWord("Μακαριοι").setTranslation("Blessed are").setStrongsId("G3107").setTransliteration(""),
        new SubToken().setWord("οι").setTranslation("the").setStrongsId("G3588").setTransliteration(""),
        new SubToken().setWord("πτωχοι").setTranslation("poor").setStrongsId("G4434").setTransliteration(""),
        new SubToken().setWord("τω").setTranslation("to").setStrongsId("G3588").setTransliteration(""),
        new SubToken().setWord("πνευματι").setTranslation("spirit").setStrongsId("G4151").setTransliteration(""),
        new SubToken().setWord("οτι").setTranslation("that").setStrongsId("G3754").setTransliteration(""),
        new SubToken().setWord("αυτων").setTranslation("them").setStrongsId("G0846").setTransliteration(""),
        new SubToken().setWord("εστιν").setTranslation("to be").setStrongsId("G1510").setTransliteration(""),
        new SubToken().setWord("βασιλεια").setTranslation("Kingdom").setStrongsId("G0932").setTransliteration(""),
        new SubToken().setWord("των").setTranslation("to the").setStrongsId("G3588").setTransliteration(""),
        new SubToken().setWord("ουρανων").setTranslation("heavens").setStrongsId("G3772").setTransliteration(""));
  }
}

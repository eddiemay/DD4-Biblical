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
      new TokenWord().setWord("ו").setTranslation("and ").setTokenType(TokenType.PREFIX),
      new TokenWord().setWord("י").setTranslation("he ").setTokenType(TokenType.PREFIX).setAsSuffix("of me"),
      new TokenWord().setWord("ימ").setTranslation("s").setTokenType(TokenType.SUFFIX_ONLY),
      new TokenWord().setWord("כ").setTranslation("like ").setAsSuffix(" you").setTokenType(TokenType.PREFIX),
      new TokenWord().setWord("ל").setTranslation("to ").setTokenType(TokenType.PREFIX),
      new TokenWord().setWord("ש").setTranslation("that which ").setTokenType(TokenType.PREFIX_ONLY),
      new TokenWord().setWord("ת").setTranslation("you ").setTokenType(TokenType.PREFIX),
      new TokenWord().setWord("את").setTranslation("you").setStrongsId("H0853"),
      new TokenWord().setWord("ראשית").setTranslation("beginning").setStrongsId("H7221"),
      new TokenWord().setWord("ברא").setTranslation("created").setStrongsId("H1254"),
      new TokenWord().setWord("אלה").setTranslation("Mighty one").setStrongsId("H0430"),
      new TokenWord().setWord("שמימ").setTranslation("heavens").setStrongsId("H8064"),
      new TokenWord().setWord("ארצ").setTranslation("earth").setStrongsId("H0776"),
      new TokenWord().setWord("יע").setTranslation("shovel").setStrongsId("H3257"),
      new TokenWord().setWord("עש").setTranslation("made").setStrongsId("H6213"),
      new TokenWord().setWord("זכור").setTranslation("remember").setStrongsId("H2142"),
      new TokenWord().setWord("מצוה").setTranslation("commandment").setStrongsId("H4687"),
      new TokenWord().setWord("אשר").setTranslation("which").setStrongsId("H0834"),
      new TokenWord().setWord("נתנ").setTranslation("given").setStrongsId("H5414"),
      new TokenWord().setWord("ה'").setTranslation("Yah").setStrongsId("H3050"),
      new TokenWord().setWord("לכ").setTranslation("to you").setStrongsId("H0853"));

  private final TokenWordStore tokenWordStore =
      new TokenWordStore(() -> inMemoryDao, () -> TOKEN_WORDS, ImmutableMap::of);
  private final MachineTranslator machineTranslator =
      new MachineTranslator(tokenWordStore, new HebrewTokenizer(tokenWordStore));

  @Test
  public void tokenize_inBeginning() {
    assertThat(machineTranslator.translate("בראשית").get(0).getSubTokens()).containsExactly(
        new SubToken().setWord("ב").setTranslation("in "),
        new SubToken().setWord("ראשית").setTranslation("beginning").setStrongsId("H7221"));
  }

  @Test
  public void tokenize_created() {
    assertThat(machineTranslator.translate("ברא").get(0).getSubTokens()).containsExactly(
        new SubToken().setWord("ברא").setTranslation("created").setStrongsId("H1254"));
  }

  @Test
  public void tokenize_andHeMade() {
    assertThat(machineTranslator.translate("ויעש").get(0).getSubTokens()).containsExactly(
        new SubToken().setWord("ו").setTranslation("and "),
        new SubToken().setWord("י").setTranslation("he "),
        new SubToken().setWord("עש").setTranslation("made").setStrongsId("H6213"));
  }

  @Test
  public void translate_Gen_1_1() {
    ImmutableList<Interlinear> translation = machineTranslator.translate("בראשית ברא אלהים את השמים ואת הארץ");
    assertThat(translation.stream().flatMap(i -> i.getSubTokens().stream()).collect(toImmutableList())).containsExactly(
        new SubToken().setWord("ב").setTranslation("in "),
        new SubToken().setWord("ראשית").setTranslation("beginning").setStrongsId("H7221"),
        new SubToken().setWord("ברא").setTranslation("created").setStrongsId("H1254"),
        new SubToken().setWord("אלה").setTranslation("Mighty one").setStrongsId("H0430"),
        new SubToken().setWord("ימ").setTranslation("s"),
        new SubToken().setWord("את").setTranslation("you").setStrongsId("H0853"),
        new SubToken().setWord("ה").setTranslation("the "),
        new SubToken().setWord("שמימ").setTranslation("heavens").setStrongsId("H8064"),
        new SubToken().setWord("ו").setTranslation("and "),
        new SubToken().setWord("את").setTranslation("you").setStrongsId("H0853"),
        new SubToken().setWord("ה").setTranslation("the "),
        new SubToken().setWord("ארצ").setTranslation("earth").setStrongsId("H0776"));
  }

  @Test
  public void translate_Jub_49_1() {
    ImmutableList<Interlinear> translation = machineTranslator.translate("זכור את המצוה אשר נתן ה' לך");
    assertThat(translation.stream().flatMap(i -> i.getSubTokens().stream()).collect(toImmutableList())).containsExactly(
        new SubToken().setWord("זכור").setTranslation("remember").setStrongsId("H2142"),
        new SubToken().setWord("את").setTranslation("you").setStrongsId("H0853"),
        new SubToken().setWord("ה").setTranslation("the "),
        new SubToken().setWord("מצוה").setTranslation("commandment").setStrongsId("H4687"),
        new SubToken().setWord("אשר").setTranslation("which").setStrongsId("H0834"),
        new SubToken().setWord("נתנ").setTranslation("given").setStrongsId("H5414"),
        new SubToken().setWord("ה'").setTranslation("Yah").setStrongsId("H3050"),
        new SubToken().setWord("לכ").setTranslation("to you").setStrongsId("H0853"));
  }
}

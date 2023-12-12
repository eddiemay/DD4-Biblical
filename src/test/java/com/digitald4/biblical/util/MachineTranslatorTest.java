package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord.TokenType;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class MachineTranslatorTest {
  private static final ImmutableList<TokenWord> TOKEN_WORDS = ImmutableList.of(
      new TokenWord().setRoot("ב").setTranslation("in ").setTokenType(TokenType.PREFIX_ONLY),
      new TokenWord().setRoot("ה").setTranslation("the ").setWithSuffix("").setTokenType(TokenType.PREFIX),
      new TokenWord().setRoot("ו").setTranslation("and ").setTokenType(TokenType.PREFIX),
      new TokenWord().setRoot("י").setTranslation("he ").setTokenType(TokenType.PREFIX).setWithSuffix("of me"),
      new TokenWord().setRoot("ימ").setTranslation("s").setTokenType(TokenType.SUFFIX_ONLY),
      new TokenWord().setRoot("ת").setTranslation("you ").setTokenType(TokenType.PREFIX),
      new TokenWord().setRoot("ש").setTranslation("that which ").setTokenType(TokenType.PREFIX_ONLY),
      new TokenWord().setRoot("את").setTranslation("you").setStrongsId("H0853"),
      new TokenWord().setRoot("ראשית").setTranslation("beginning").setStrongsId("H7221"),
      new TokenWord().setRoot("ברא").setTranslation("created").setStrongsId("H1254"),
      new TokenWord().setRoot("אלה").setTranslation("Mighty one").setStrongsId("H0430"),
      new TokenWord().setRoot("שמימ").setTranslation("heavens").setStrongsId("H8064"),
      new TokenWord().setRoot("ארצ").setTranslation("earth").setStrongsId("H0776"),
      new TokenWord().setRoot("יע").setTranslation("shovel").setStrongsId("H3257"),
      new TokenWord().setRoot("עש").setTranslation("made").setStrongsId("H6213"));

  private final MachineTranslator machineTranslator = new MachineTranslator(() -> TOKEN_WORDS);

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
    ImmutableList<Interlinear> translation =
        machineTranslator.translate("בראשית ברא אלהים את השמים ואת הארץ");
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
}

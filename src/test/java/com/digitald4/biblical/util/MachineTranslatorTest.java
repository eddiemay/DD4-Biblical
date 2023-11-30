package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.util.MachineTranslator.TokenWord;
import com.digitald4.biblical.util.MachineTranslator.TokenWord.TokenType;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class MachineTranslatorTest {
  private static final ImmutableList<TokenWord> TOKEN_WORDS = ImmutableList.of(
      new TokenWord().setWord("ב").setTranslation("in "),
      new TokenWord().setWord("ה").setTranslation("the ").setAsSuffix(""),
      new TokenWord().setWord("ו").setTranslation("and "),
      new TokenWord().setWord("י").setTranslation("he "),
      new TokenWord().setWord("ימ").setTranslation("sea").setAsSuffix("s"),
      new TokenWord().setWord("ת").setTranslation("you "),
      new TokenWord().setWord("ש").setTranslation("that which ").setTokenType(TokenType.PREFIX_ONLY),
      new TokenWord().setWord("את").setTranslation("you").setStrongsId("H0853"),
      new TokenWord().setWord("ראשית").setTranslation("beginning").setStrongsId("H7221"),
      new TokenWord().setWord("ברא").setTranslation("created").setStrongsId("H1254"),
      new TokenWord().setWord("אלה").setTranslation("Mighty one").setStrongsId("H0430"),
      new TokenWord().setWord("שמימ").setTranslation("heavens").setStrongsId("H8064"),
      new TokenWord().setWord("ארצ").setTranslation("earth").setStrongsId("H0776"),
      new TokenWord().setWord("יע").setTranslation("shovel").setStrongsId("H3257"),
      new TokenWord().setWord("עש").setTranslation("made").setStrongsId("H6213"));
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
        new SubToken().setWord("ארצ").setTranslation("earth").setStrongsId("H0776")
    );
  }
}

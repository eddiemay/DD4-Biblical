package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;

import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord.TokenType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Ignore;
import org.junit.Test;

public class HebrewTokenizerTest {

  private static final HebrewTokenizer tokenizer = new HebrewTokenizer(
      ImmutableSet.of(
          new TokenWord().setRoot("ב").setTokenType(TokenType.PREFIX_ONLY),
          new TokenWord().setRoot("ה").setTokenType(TokenType.PREFIX),
          new TokenWord().setRoot("ו").setTokenType(TokenType.PREFIX),
          new TokenWord().setRoot("י").setTokenType(TokenType.PREFIX),
          new TokenWord().setRoot("ים").setTokenType(TokenType.SUFFIX),
          new TokenWord().setRoot("ש").setTokenType(TokenType.PREFIX_ONLY),
          new TokenWord().setRoot("כ").setTokenType(TokenType.PREFIX),
          new TokenWord().setRoot("ברא"),
          new TokenWord().setRoot("ראשית"),
          new TokenWord().setRoot("את").setStrongsId("H0853"),
          new TokenWord().setRoot("אלוה"),
          new TokenWord().setRoot("ארץ"),
          new TokenWord().setRoot("שמים"),
          new TokenWord().setRoot("היתה"),
          new TokenWord().setRoot("תהו"),
          new TokenWord().setRoot("בהו"),
          new TokenWord().setRoot("חשך"),
          new TokenWord().setRoot("על"),
          new TokenWord().setRoot("פני"),
          new TokenWord().setRoot("תהום"),
          new TokenWord().setRoot("רוח"),
          new TokenWord().setRoot("מרחפת"),
          new TokenWord().setRoot("מים"),
          new TokenWord().setRoot("אמר"),
          new TokenWord().setRoot("אמר"),
          new TokenWord().setRoot("אור"),
          new TokenWord().setRoot("יהי"),
          new TokenWord().setRoot("ויהי"),
          new TokenWord().setRoot("עש"),
          new TokenWord().setRoot("בכ").setStrongsId("H0123"),
          new TokenWord().setRoot("יע")));

  @Test
  public void tokenize_inBeginning() {
    assertThat(tokenizer.tokenizeWord("בראשית")).containsExactly("ב", "ראשית");
  }

  @Test
  public void tokenize_created() {
    assertThat(tokenizer.tokenizeWord("ברא")).containsExactly("ברא");
  }

  @Test
  public void tokenize_andHeMade() {
    assertThat(tokenizer.tokenizeWord("ויעש")).containsExactly("ו", "י", "עש");
  }

  @Test
  public void tokenize_weep() {
    assertThat(tokenizer.tokenizeWord("בכ", "H0123")).containsExactly("בכ");
  }

  @Test @Ignore
  public void tokenize_inOfYou() {
    assertThat(tokenizer.tokenizeWord("בכ", null)).containsExactly("ב", "כ");
  }

  @Test
  public void tokenize_Gen_1_1() {
    assertThat(tokenizer.tokenize("בראשית ברא אלוהים את השמים ואת הארץ")).containsExactly(
        ImmutableList.of("ב", "ראשית"),
        ImmutableList.of("ברא"),
        ImmutableList.of("אלוה", "ים"),
        ImmutableList.of("את"),
        ImmutableList.of("ה", "שמים"),
        ImmutableList.of("ו","את"),
        ImmutableList.of("ה", "ארץ"));
  }

  @Test
  public void tokenize_Gen_1_2() {
    assertThat(tokenizer.tokenize("והארץ היתה תהו ובהו וחשך על פני תהום ורוח אלוהים מרחפת על פני המים")).containsExactly(
        ImmutableList.of("ו", "ה", "ארץ"),
        ImmutableList.of("היתה"),
        ImmutableList.of("תהו"),
        ImmutableList.of("ו", "בהו"),
        ImmutableList.of("ו", "חשך"),
        ImmutableList.of("על"),
        ImmutableList.of("פני"),
        ImmutableList.of("תהום"),
        ImmutableList.of("ו", "רוח"),
        ImmutableList.of("אלוה", "ים"),
        ImmutableList.of("מרחפת"),
        ImmutableList.of("על"),
        ImmutableList.of("פני"),
        ImmutableList.of("ה", "מים"));
  }

  @Test
  public void tokenize_Gen_1_3() {
    assertThat(tokenizer.tokenize("ויאמר אלוהים יהי אור ויהי אור")).containsExactly(
        ImmutableList.of("ו", "י", "אמר"),
        ImmutableList.of("אלוה", "ים"),
        ImmutableList.of("יהי"),
        ImmutableList.of("אור"),
        ImmutableList.of("ויהי"),
        ImmutableList.of("אור"));
  }

}

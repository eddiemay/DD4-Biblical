package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;

import com.digitald4.biblical.util.MachineTranslator.TokenWord;
import com.digitald4.biblical.util.MachineTranslator.TokenWord.TokenType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

public class SubwordTokenizerTest {

  private static final SubwordTokenizer subwordTokenizer = new SubwordTokenizer(
      ImmutableSet.of(
          new TokenWord().setWord("ב"),
          new TokenWord().setWord("ה"),
          new TokenWord().setWord("ו"),
          new TokenWord().setWord("י"),
          new TokenWord().setWord("ים"),
          new TokenWord().setWord("ש").setTokenType(TokenType.PREFIX_ONLY),
          new TokenWord().setWord("ברא"),
          new TokenWord().setWord("ראשית"),
          new TokenWord().setWord("את"),
          new TokenWord().setWord("אלוה"),
          new TokenWord().setWord("ארץ"),
          new TokenWord().setWord("שמים"),
          new TokenWord().setWord("היתה"),
          new TokenWord().setWord("תהו"),
          new TokenWord().setWord("בהו"),
          new TokenWord().setWord("חשך"),
          new TokenWord().setWord("על"),
          new TokenWord().setWord("פני"),
          new TokenWord().setWord("תהום"),
          new TokenWord().setWord("רוח"),
          new TokenWord().setWord("מרחפת"),
          new TokenWord().setWord("מים"),
          new TokenWord().setWord("אמר"),
          new TokenWord().setWord("אמר"),
          new TokenWord().setWord("אור"),
          new TokenWord().setWord("יהי"),
          new TokenWord().setWord("ויהי"),
          new TokenWord().setWord("עש"),
          new TokenWord().setWord("יע")));

  @Test
  public void tokenize_inBeginning() {
    assertThat(subwordTokenizer.tokenizeWord("בראשית")).containsExactly("ב", "ראשית");
  }

  @Test
  public void tokenize_created() {
    assertThat(subwordTokenizer.tokenizeWord("ברא")).containsExactly("ברא");
  }

  @Test
  public void tokenize_andHeMade() {
    assertThat(subwordTokenizer.tokenizeWord("ויעש")).containsExactly("ו", "י", "עש");
  }

  @Test
  public void tokenize_Gen_1_1() {
    assertThat(subwordTokenizer.tokenize("בראשית ברא אלוהים את השמים ואת הארץ")).containsExactly(
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
    assertThat(subwordTokenizer.tokenize("והארץ היתה תהו ובהו וחשך על פני תהום ורוח אלוהים מרחפת על פני המים")).containsExactly(
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
    assertThat(subwordTokenizer.tokenize("ויאמר אלוהים יהי אור ויהי אור")).containsExactly(
        ImmutableList.of("ו", "י", "אמר"),
        ImmutableList.of("אלוה", "ים"),
        ImmutableList.of("יהי"),
        ImmutableList.of("אור"),
        ImmutableList.of("ויהי"),
        ImmutableList.of("אור"));
  }
}

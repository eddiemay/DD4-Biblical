package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

public class SubwordTokenizerTest {

  private static final SubwordTokenizer subwordTokenizer = new SubwordTokenizer(
      ImmutableSet.of(
          "ב", "##ב", "ה", "##ה", "ו", "י", "##י",
          "##ים",
          "ברא", "##ברא", "ראשית", "##ראשית", "ים", "את", "##את", "אלוה", "##אלוה", "##ארץ", "##שמ",
          "היתה", "תהו", "בהו", "##בהו", "חשך", "##חשך", "על", "פני", "תהום", "רוח", "##רוח", "מרחפת", "מים", "##מים",
          "אמר", "##אמר", "אור", "##אור", "יהי", "ויהי"));

  @Test
  public void tokenize_inBeginning() {
    assertThat(subwordTokenizer.tokenizeWord("בראשית")).containsExactly("ב", "##ראשית");
  }

  @Test
  public void tokenize_created() {
    assertThat(subwordTokenizer.tokenizeWord("ברא")).containsExactly("ברא");
  }

  @Test
  public void tokenize_Gen_1_1() {
    assertThat(subwordTokenizer.tokenize("בראשית ברא אלוהים את השמים ואת הארץ")).containsExactly(
        ImmutableList.of("ב", "##ראשית"),
        ImmutableList.of("ברא"),
        ImmutableList.of("אלוה", "##ים"),
        ImmutableList.of("את"),
        ImmutableList.of("ה", "##שמ", "##ים"),
        ImmutableList.of("ו","##את"),
        ImmutableList.of("ה", "##ארץ"));
  }

  @Test
  public void tokenize_Gen_1_2() {
    assertThat(subwordTokenizer.tokenize("והארץ היתה תהו ובהו וחשך על פני תהום ורוח אלוהים מרחפת על פני המים")).containsExactly(
        ImmutableList.of("ו", "##ה", "##ארץ"),
        ImmutableList.of("היתה"),
        ImmutableList.of("תהו"),
        ImmutableList.of("ו", "##בהו"),
        ImmutableList.of("ו", "##חשך"),
        ImmutableList.of("על"),
        ImmutableList.of("פני"),
        ImmutableList.of("תהום"),
        ImmutableList.of("ו", "##רוח"),
        ImmutableList.of("אלוה", "##ים"),
        ImmutableList.of("מרחפת"),
        ImmutableList.of("על"),
        ImmutableList.of("פני"),
        ImmutableList.of("ה", "##מים"));
  }

  @Test
  public void tokenize_Gen_1_3() {
    assertThat(subwordTokenizer.tokenize("ויאמר אלוהים יהי אור ויהי אור")).containsExactly(
        ImmutableList.of("ו", "##י", "##אמר"),
        ImmutableList.of("אלוה", "##ים"),
        ImmutableList.of("יהי"),
        ImmutableList.of("אור"),
        ImmutableList.of("ויהי"),
        ImmutableList.of("אור"));
  }
}

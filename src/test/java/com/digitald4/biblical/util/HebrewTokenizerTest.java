package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;

import com.digitald4.biblical.store.LexiconStore;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord.TokenType;
import com.digitald4.common.storage.DAOInMemoryImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Ignore;
import org.junit.Test;

public class HebrewTokenizerTest {
  private static final DAOInMemoryImpl inMemoryImpl = new DAOInMemoryImpl();
  private static final LexiconStore lexiconStore = new LexiconStore(() -> inMemoryImpl, null);
  private static final HebrewTokenizer tokenizer = new HebrewTokenizer(
      new TokenWordStore(() -> inMemoryImpl, () -> ImmutableSet.of(
          new TokenWord().setWord("ב").setTokenType(TokenType.PREFIX_ONLY),
          new TokenWord().setWord("ה").setTokenType(TokenType.PREFIX),
          new TokenWord().setWord("ו").setTokenType(TokenType.PREFIX),
          new TokenWord().setWord("י").setTokenType(TokenType.PREFIX),
          new TokenWord().setWord("ים").setTokenType(TokenType.SUFFIX),
          new TokenWord().setWord("ש").setTokenType(TokenType.PREFIX_ONLY),
          new TokenWord().setWord("כ").setTokenType(TokenType.PREFIX),
          new TokenWord().setWord("ברא"),
          new TokenWord().setWord("ראשית"),
          new TokenWord().setWord("את").setStrongsId("H0853"),
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
          new TokenWord().setWord("בכ").setStrongsId("H0123"),
          new TokenWord().setWord("יע")), lexiconStore));

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

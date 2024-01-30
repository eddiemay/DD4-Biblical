package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Ignore;
import org.junit.Test;

public class BPETokenizerTest {
  @Test @Ignore
  public void tokenizeAlahim() {
    assertThat(new BPETokenizer().tokenize("אלהימ")).containsExactly("אלהימ");
  }

  @Test @Ignore
  public void tokenizeDoNotSteal() {
    assertThat(new BPETokenizer().tokenize("לא תגנב")).containsExactly("לא", "תגנב");
  }
}

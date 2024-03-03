package com.digitald4.biblical.model;

import static com.google.common.truth.Truth.assertThat;

import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import org.junit.Test;

public class ScriptureVersionTest {
  private static final StaticDataDAO staticDataDAO = new StaticDataDAO();
  private static final BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
  @Test
  public void correctBookCounts() {
    assertThat(bibleBookStore.getBibleBooks("ISR")).hasSize(66);
    assertThat(bibleBookStore.getBibleBooks("RSKJ")).hasSize(66);
    assertThat(bibleBookStore.getBibleBooks("NRSV")).hasSize(83);
    assertThat(bibleBookStore.getBibleBooks("NWT")).hasSize(66);
    assertThat(bibleBookStore.getBibleBooks("KJV1611")).hasSize(81);
    assertThat(bibleBookStore.getBibleBooks("OXFORD")).hasSize(5);
    assertThat(bibleBookStore.getBibleBooks("Sefaria")).hasSize(12);
    assertThat(bibleBookStore.getBibleBooks("CCC")).hasSize(37);
    assertThat(bibleBookStore.getBibleBooks("WLCO")).hasSize(39);
    assertThat(bibleBookStore.getBibleBooks("DSS")).hasSize(4);
  }
}

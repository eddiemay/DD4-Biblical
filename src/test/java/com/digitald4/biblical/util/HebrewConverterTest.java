package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class HebrewConverterTest {
  @Test
  public void toAncient() {
    assertThat(HebrewConverter.toAncient("הללו את־יהוה כל־גוים בחוהו כל־האמים׃").replaceAll("\u00a0", " "))
        .isEqualTo("𓀠𓏱𓏱𓏲   𓃾𓏴   𓂝𓀠𓏲𓀠   𓂩𓏱   𓌙𓏲𓂝𓈖   𓉔𓈈𓏲𓀠𓏲   𓂩𓏱   𓀠𓃾𓈖𓂝𓈖׃");
    assertThat(HebrewConverter.toAncient("כי גבר עלינו ׀ חסדו ואמת־יהוה לעולם הללו־יה׃").replaceAll("\u00a0", " "))
        .isEqualTo("𓂩𓂝   𓌙𓉔𓁶   𓁹𓏱𓂝𓆓𓏲   ׀   𓈈𓊽𓇯𓏲   𓏲𓃾𓈖𓏴   𓂝𓀠𓏲𓀠   𓏱𓁹𓏲𓏱𓈖   𓀠𓏱𓏱𓏲   𓂝𓀠׃");
  }
}

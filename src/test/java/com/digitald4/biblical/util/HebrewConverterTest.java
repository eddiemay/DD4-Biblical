package com.digitald4.biblical.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Ignore;
import org.junit.Test;

public class HebrewConverterTest {
  @Test
  public void toAncient() {
    assertThat(HebrewConverter.toAncient("הללו את־יהוה כל־גוים בחוהו כל־האמים׃").replaceAll("\u00a0", " "))
        .isEqualTo("𓀠𓏱𓏱𓏲   𓃾𓏴   𓂝𓀠𓏲𓀠   𓂩𓏱   𓃀𓏲𓂝𓈖   𓉔𓈈𓏲𓀠𓏲   𓂩𓏱   𓀠𓃾𓈖𓂝𓈖");
    assertThat(HebrewConverter.toAncient("כי גבר עלינו ׀ חסדו ואמת־יהוה לעולם הללו־יה׃").replaceAll("\u00a0", " "))
        .isEqualTo("𓂩𓂝   𓃀𓉔𓁶   𓁹𓏱𓂝𓆓𓏲   𓈈𓊽𓇯𓏲   𓏲𓃾𓈖𓏴   𓂝𓀠𓏲𓀠   𓏱𓁹𓏲𓏱𓈖   𓀠𓏱𓏱𓏲   𓂝𓀠");
  }

  @Test
  public void toAncientRtl() {
    assertThat(HebrewConverter.toAncientRtl("הללו את־יהוה כל־גוים בחוהו כל־האמים׃").replaceAll("\u00a0", " "))
        .isEqualTo("𓈖𓂝𓈖𓃾𓀠   𓏱𓂩   𓏲𓀠𓏲𓈈𓉔   𓈖𓂝𓏲𓃀   𓏱𓂩   𓀠𓏲𓀠𓂝   𓏴𓃾   𓏲𓏱𓏱𓀠");
    assertThat(HebrewConverter.toAncientRtl("כי גבר עלינו ׀ חסדו ואמת־יהוה לעולם הללו־יה׃").replaceAll("\u00a0", " "))
        .isEqualTo("𓀠𓂝   𓏲𓏱𓏱𓀠   𓈖𓏱𓏲𓁹𓏱   𓀠𓏲𓀠𓂝   𓏴𓈖𓃾𓏲   𓏲𓇯𓊽𓈈   𓏲𓆓𓂝𓏱𓁹   𓁶𓉔𓃀   𓂝𓂩");
  }

  @Test
  public void toConstants() {
    // Genesis 1:1
    assertThat(HebrewConverter.toConstantsOnly("בְּרֵאשִׁ֖ית בָּרָ֣א אֱלֹהִ֑ים אֵ֥ת הַשָּׁמַ֖יִם וְאֵ֥ת הָאָֽרֶץ׃"))
        .isEqualTo("בראשית ברא אלהים את השמים ואת הארץ");
    // Genesis 2:3
    assertThat(HebrewConverter.toConstantsOnly("וַיְבָ֤רֶךְ אֱלֹהִים֙ אֶת־יֹ֣ום הַשְּׁבִיעִ֔י וַיְקַדֵּ֖שׁ אֹתֹ֑ו כִּ֣י בֹ֤ו שָׁבַת֙ מִכָּל־מְלַאכְתֹּ֔ו אֲשֶׁר־בָּרָ֥א אֱלֹהִ֖ים לַעֲשֹֽׂות׃ פ"))
        .isEqualTo("ויברך אלהים את יום השביעי ויקדש אתו כי בו שבת מכל מלאכתו אשר ברא אלהים לעשות");
  }

  @Test
  public void toContantsOnly_removesHidden() {
    assertThat(HebrewConverter.toConstantsOnly("פָּנָֽ֗יַ\u202A\u202C")).isEqualTo("פני");
  }

  @Test
  public void toContantsOnly_geez() {
    assertThat(HebrewConverter.toGeezConstants(HebrewConverter.removePunctuation("ካልኦት ኣማልኽቲ ኣብ ቅድመይ ኣይሀልዉኻ።")))
        .isEqualTo("ከለአተ አመለኸተ አበ ቀደመየ አየሀለወኸ");
  }

  @Test
  public void toFullHebrew_geez() {
    assertThat(HebrewConverter.toGeezConstants(HebrewConverter.removePunctuation("ካልኦት ኣማልኽቲ ኣብ ቅድመይ ኣይሀልዉኻ።")))
        .isEqualTo("ከለአተ አመለኸተ አበ ቀደመየ አየሀለወኸ");
  }

  @Test
  public void toFullHebrew_waw() {
    assertThat(HebrewConverter.toFullHebrew("יֻלַּד")).isEqualTo("יולד");
    assertThat(HebrewConverter.toFullHebrew("יֹשְׁבֵי֙")).isEqualTo("יושבי");
    assertThat(HebrewConverter.toFullHebrew("אֱלֹהֵ֣י")).isEqualTo("אלוהי");
    assertThat(HebrewConverter.toFullHebrew("אָדֹ֖ם")).isEqualTo("אדום");
    // assertThat(HebrewConverter.toFullHebrew("וְגֹאֲל֖וֹ")).isEqualTo("וגואליו");
    assertThat(HebrewConverter.toFullHebrew("גֹּאֲלֶ֔ךָ")).isEqualTo("גואלך"); // redeemer
  }

  @Test @Ignore
  public void toFullHebrew_yod() {
    assertThat(HebrewConverter.toFullHebrew("וִירוּשָׁלִָ֑ם")).isEqualTo("וירושלים");
    assertThat(HebrewConverter.toFullHebrew("דִּבּוּר")).isEqualTo("דיבור");
  }

  @Test @Ignore
  public void toFullHebrew_dibur() {
    assertThat(HebrewConverter.toFullHebrew("דִּבּוּר")).isEqualTo("דיבור"); // divur, speak
    assertThat(HebrewConverter.toFullHebrew("דָבָ֔ר")).isEqualTo("דבר"); // deber, anything
    assertThat(HebrewConverter.toFullHebrew("וַיְדַבֵּ֥ר")).isEqualTo("וידבר"); // dabar, and he spoke
    assertThat(HebrewConverter.toFullHebrew("דַּבֵּ֞ר")).isEqualTo("דבר"); // dabar, speak
    assertThat(HebrewConverter.toFullHebrew("דִּבֵּר")).isEqualTo("דיבר"); // dibar, he spoke
  }

  public static void printHexString(String input) {
    StringBuilder hexString = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      String hex = Integer.toHexString(c);
      if (!hexString.isEmpty()) {
        hexString.append(" ");
      }
      hexString.append(hex.toUpperCase());
    }
    System.out.println(hexString);
  }
}

package com.digitald4.biblical.util;

import static com.digitald4.biblical.model.BibleBook.EN;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ScriptureFetcherSefariaOrgTest  extends ScriptureFetcherTest {

  @Before
  public void setup() {
    super.setup();
  }

  @Test
  public void fetchJubilees() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/sefaria_jubilees_2.json"));

    assertThat(scriptureStore.getScriptures("Sefaria", EN, "Jubilees 2:1-4").getItems()).containsExactly(
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(1).setText(
            "And the angel of the presence spake to Moses according to the word of the Lord, saying:"),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(1).setLocale("he").setText(
            "ויאמר מלאך הפנים אל משה בדבר ה' לאמור:"),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(2).setText(
            "Write the complete history of the creation, how in six days the Lord God finished all His works and all that He created, and kept Sabbath on the seventh day and hallowed it for all ages, and appointed it as a sign for all His works."),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(2).setLocale("he").setText(
            "כתוב בספר תולדות השמים והארץ כי בששת ימים כילה ה' אלהים את כל מלאכתו אשר ברא לעשות וביום השביעי שבת ויקדשהו לעולמי עד וישימהו לאות על כל מעשיו:"),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(3).setText(
            "For on the first day He created the heavens which are above and the earth and the waters and all the spirits which serve before Him"),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(3).setLocale("he").setText(
            "הן ביום הראשון ברא את השמים ממעל והארץ והמים וכל הרוחות העומדים לפניו:"),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(4).setText(
            "--the angels of the presence, and the angels of sanctification, and the angels [of the spirit of fire and the angels] of the spirit of the winds, and the angels of the spirit of the clouds, "),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(4).setLocale("he").setText(
            "ומלאכי הפנים ומלאכי הקדושה ומלאכי רוח האש ומלאכי רוח סערה ומלאכי רוחות ענני עלטה:"));
  }

  @Test @Ignore
  public void fetchTestamentOfReuben() throws Exception {
    when(apiConnector.sendGet(anyString())).thenReturn(
        getContent("src/test/java/com/digitald4/biblical/util/data/testament_of_reuben.json"));

    assertThat(scriptureStore.getScriptures("Sefaria", EN, "Testament of Reuben 2:1-4").getItems()).containsExactly(
        new Scripture().setVersion("Sefaria").setBook("Testament of Reuben").setChapter(2).setVerse(1).setText(
            "And now hear me, my children, what things I saw concerning the seven spirits of deceit, when I repented."),
        new Scripture().setVersion("Sefaria").setBook("Testament of Reuben").setChapter(2).setVerse(1).setLocale("he").setText(
            "ועתה שמעו אלי בני את־אשר ראיתי על־דבר שבעה רוחות התהו באבלי:"),
        new Scripture().setVersion("Sefaria").setBook("Testament of Reuben").setChapter(2).setVerse(2).setText(
            "Seven spirits therefore are appointed against man, and they are the leaders in the works of youth."),
        new Scripture().setVersion("Sefaria").setBook("Testament of Reuben").setChapter(2).setVerse(2).setLocale("he").setText(
            "כי שבעה רוחות נתנו נגד האדם והם ראש כל־מעשי נערות:"),
        new Scripture().setVersion("Sefaria").setBook("Testament of Reuben").setChapter(2).setVerse(3).setText(
            "[And seven other spirits are given to him at his creation, that through them should be done every work of man."),
        new Scripture().setVersion("Sefaria").setBook("Testament of Reuben").setChapter(2).setVerse(3).setLocale("he").setText(
            "ושבעה רוחות אחרים נתנו לו כאשר נוצר ובהם יהי כל־מעשה האדם:"),
        new Scripture().setVersion("Sefaria").setBook("Testament of Reuben").setChapter(2).setVerse(4).setText(
            "The first is the spirit of life, with which the constitution (of man) is created. The second is the sense of sight, with which ariseth desire."),
        new Scripture().setVersion("Sefaria").setBook("Testament of Reuben").setChapter(2).setVerse(4).setLocale("he").setText(
            "הראשון הוא רוח החיים אשר בו יוסד קיום האדם השני הוא רוח הראיה אשר בו תוצר התאוה:"));
  }
}

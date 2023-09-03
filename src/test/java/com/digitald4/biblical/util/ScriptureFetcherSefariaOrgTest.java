package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.Language.EN;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.Scripture;
import com.digitald4.common.exception.DD4StorageException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Before;
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
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(1).setLanguage("he").setText(
            "ויאמר מלאך הפנים אל משה בדבר ה' לאמור:"),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(2).setText(
            "Write the complete history of the creation, how in six days the Lord God finished all His works and all that He created, and kept Sabbath on the seventh day and hallowed it for all ages, and appointed it as a sign for all His works."),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(2).setLanguage("he").setText(
            "כתוב בספר תולדות השמים והארץ כי בששת ימים כילה ה' אלהים את כל מלאכתו אשר ברא לעשות וביום השביעי שבת ויקדשהו לעולמי עד וישימהו לאות על כל מעשיו:"),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(3).setText(
            "For on the first day He created the heavens which are above and the earth and the waters and all the spirits which serve before Him"),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(3).setLanguage("he").setText(
            "הן ביום הראשון ברא את השמים ממעל והארץ והמים וכל הרוחות העומדים לפניו:"),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(4).setText(
            "--the angels of the presence, and the angels of sanctification, and the angels [of the spirit of fire and the angels] of the spirit of the winds, and the angels of the spirit of the clouds, "),
        new Scripture().setVersion("Sefaria").setBook("Jubilees").setChapter(2).setVerse(4).setLanguage("he").setText(
            "ומלאכי הפנים ומלאכי הקדושה ומלאכי רוח האש ומלאכי רוח סערה ומלאכי רוחות ענני עלטה:"));
  }

  @Test
  public void fetchTestamentOfReuben() throws Exception {
    Pattern chapterPattern = Pattern.compile("(\\d+)\\?");
    when(apiConnector.sendGet(anyString())).then(invocation -> {
      String url = invocation.getArgument(0);
      assertThat(url).startsWith(
          "https://www.sefaria.org/api/texts/The_Testaments_of_the_Twelve_Patriarchs%2C_The_Testament_of_Reuben_the_First_born_Son_of_Jacob_and_Leah");
      Matcher matcher = chapterPattern.matcher(url);
      if (!matcher.find()) {
        throw new DD4StorageException("Unable to match pattern with url: " + url);
      }
      int chapter = Integer.parseInt(matcher.group(1));
      return String.format(
          "{\"text\": [\"Reuben chapter %d verse 1\", \"Reuben chapter %d verse 2\"],\n" +
              "\"he\": [\"ראובן\", \"ראובן\"]}", chapter, chapter);
    });

    assertThat(scriptureStore.getScriptures("Sefaria", EN, "TofT 1").getItems()).containsExactlyElementsIn(
        IntStream.range(1, 7 + 1)
            .boxed()
            .flatMap(c ->
                Stream.of(
                    new Scripture().setVersion("Sefaria").setBook("Testaments of the Twelve Patriarchs")
                      .setChapter(1).setVerse(c * 2 - 1).setText("Reuben chapter "  + c + " verse 1"),
                    new Scripture().setVersion("Sefaria").setBook("Testaments of the Twelve Patriarchs")
                        .setLanguage("he").setChapter(1).setVerse(c * 2 - 1).setText("ראובן"),
                    new Scripture().setVersion("Sefaria").setBook("Testaments of the Twelve Patriarchs")
                        .setChapter(1).setVerse(c * 2).setText("Reuben chapter "  + c + " verse 2"),
                    new Scripture().setVersion("Sefaria").setBook("Testaments of the Twelve Patriarchs")
                        .setLanguage("he").setChapter(1).setVerse(c * 2).setText("ראובן")))
            .collect(toImmutableList()));
  }
}

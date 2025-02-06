package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.tools.TranslationTool;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOFileBasedImpl;
import com.digitald4.common.storage.DAOFileDBImpl;
import com.google.common.collect.ImmutableList;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.Test;

public class MachineTranslationIntegrationTest {
  private static final DAOFileDBImpl daoFileDB = new DAOFileDBImpl();
  private static final DAOFileBasedImpl fileDao = new DAOFileBasedImpl("data/interlinear.db").loadFromFile();
  private static final APIConnector apiConnector = new APIConnector(Constants.API_URL, Constants.API_VERSION, 50);
  private static final BibleBookStore bibleBookStore = new BibleBookStore(() -> daoFileDB);
  private static final InterlinearFetcher interlinearFetcher = new ScriptureFetcherBibleHub(apiConnector);
  private static final InterlinearStore interlinearStore =
      new InterlinearStore(() -> fileDao, new ScriptureReferenceProcessorSplitImpl(bibleBookStore), interlinearFetcher);

  private static final TokenWordStore tokenWordStore =
      new TokenWordStore(TranslationTool::tokenWordProvider, TranslationTool::lexiconProvider);
  private static final MachineTranslator machineTranslator =
      new MachineTranslator(tokenWordStore, new HebrewTokenizer(tokenWordStore));

  @AfterClass
  public static void tearDown() {
    fileDao.saveToFile();
    daoFileDB.saveFiles();
  }

  @Test
  public void translate7Days() {
    ImmutableList<String> translations = IntStream.range(1, 35)
        .mapToObj(v -> String.format("Genesis %d:%d", v < 32 ? 1 : 2, v < 32 ? v : v - 31))
        .map(this::translate)
        .collect(toImmutableList());

    assertThat(translations).containsExactly(
        "Genesis 1:1 in beginning create Mighty Ones you the heavens and you the land",
        "Genesis 1:2 and the land was vain and void and darkness upon face deep and spirit Mighty Ones hovering upon face the waters",
        "Genesis 1:3 and he said Mighty Ones let there be light and there came to be light",
        "Genesis 1:4 and he see Mighty Ones you the light for good and he separate Mighty Ones between the light and between the darkness",
        "Genesis 1:5 and he call Mighty Ones to light day and to darkness call night and there came to be evening and there came to be morning day one",
        "Genesis 1:6 and he said Mighty Ones let there be firmament in midst the waters and there came to be division between waters to waters",
        "Genesis 1:7 and he made Mighty Ones you the firmament and he separate between the waters which from under to firmament and between the waters which from upon to firmament and there came to be so",
        "Genesis 1:8 and he call Mighty Ones to firmament heavens and there came to be evening and there came to be morning day second",
        "Genesis 1:9 and he said Mighty Ones he wait the waters from under the heavens unto places one and you see the dry land and there came to be so",
        "Genesis 1:10 and he call Mighty Ones to dry land land and to collection the waters call seas and he see Mighty Ones for good",
        "Genesis 1:11 and he said Mighty Ones you spring the land grass herb from sowing seed tree fruit maker fruit to kind of him which seed of him in it upon the land and there came to be so",
        "Genesis 1:12 and you bring out the land grass herb from sowing seed to kind of it and tree maker fruit which seed of him in it to kind of it and he see Mighty Ones for good",
        "Genesis 1:13 and there came to be evening and there came to be morning day third",
        "Genesis 1:14 and he said Mighty Ones let there be lights in firmament the heavens to the separation between the day and between the night and let it be to signs and to feasts and to days and years",
        "Genesis 1:15 and let it be to lights in firmament the heavens to shine upon the land and there came to be so",
        "Genesis 1:16 and he made Mighty Ones you two the lights the greats you the light the great to dominates the day and you the light the lesser to dominates the night and you the stars",
        "Genesis 1:17 and he set them Mighty Ones in firmament the heavens to shine upon the land",
        "Genesis 1:18 and to rule in day and in night and to the separation between the light and between the darkness and he see Mighty Ones for good",
        "Genesis 1:19 and there came to be evening and there came to be morning day fourth",
        "Genesis 1:20 and he said Mighty Ones let abound the waters creeping soul live and bird he fly upon the land upon face firmament the heavens",
        "Genesis 1:21 and he create Mighty Ones you the dragons the greats and you all soul the live the that moves which abound the waters to kind of them and you all bird wing to kind of it and he see Mighty Ones for good",
        "Genesis 1:22 and he bless them Mighty Ones to say increase and multiply and fill of him you the waters in seas and the bird let multiply in land",
        "Genesis 1:23 and there came to be evening and there came to be morning day fifth",
        "Genesis 1:24 and he said Mighty Ones you bring out the land soul live to kind beast and creeping and his life land to kind and there came to be so",
        "Genesis 1:25 and he made Mighty Ones you lives the land to kind and you the beast to kind and you all creeping the land to kind of it and he see Mighty Ones for good",
        "Genesis 1:26 and he said Mighty Ones we made man in image of us like likeness of us and he rule in fish the sea and in bird the heavens and in beast and in all the land and in all the creeping the that move upon the land",
        "Genesis 1:27 and he create Mighty Ones you the man in image of him in image Mighty Ones create him male and female create them",
        "Genesis 1:28 and he bless them Mighty Ones and he said to them Mighty Ones increase and multiply and fill of him you the land and subdue of him and rule in fish the sea and in bird the heavens and in all live the that moves upon the land",
        "Genesis 1:29 and he said Mighty Ones behold gives of to them you all herb sow seed which upon face all the land and you all the tree which in it fruit tree sow seed to them let there be to food",
        "Genesis 1:30 and to all lives the land and to all bird the heavens and to all that move upon the land which in it soul live you all green herb to food and there came to be so",
        "Genesis 1:31 and he see Mighty Ones you all which made and behold good very and there came to be evening and there came to be morning day the sixth",
        "Genesis 2:1 and he completed the heavens and the land and all hosts",
        "Genesis 2:2 and he complete Mighty Ones in day the seventh work of him which made and he shabated in day the seventh from all work of him which made",
        "Genesis 2:3 and he bless Mighty Ones you day the seventh and he sanctify him for in it shabbat from all work of him which create Mighty Ones to accomplish"
    );
  } // נוח

  @Test
  public void translateChoiceFew() {
    ImmutableList<String> translations = Stream
        .of("Isa 9:6", "Psa 83:18", "Exo 12:11", "Gen 10:1", "Lev 25:4", "Exo 20:2", "Lev 23:14")
        .map(this::translate).collect(toImmutableList());
    assertThat(translations).containsExactly(
        "Isa 9:6 for child born to of us son given to of us and you will be the government upon shoulder of him and he call name of him wonderful counselor God mighty I in he forever prince peace",
        "Psa 83:18 and know of him for you are name of you יהוה to alone of you high upon all the land",
        "Exo 12:11 and like this you and eat of him him waists of yous belts shoes of yous in foots of yous and rod of yous in hand of yous and eats him in haste passover he to יהוה",
        "Gen 10:1 and these generations sons of Noah Shem Ham and Japheth and born of him to them sons after the flood",
        "Lev 25:4 and in year the sevenths shabbat shabbat of it let there be to land shabbat to יהוה field of you not you sow and vineyard of you not you prune",
        "Exo 20:2 I יהוה Mighty Ones of you which the bring outss of you from land Mizraim from house servants",
        "Lev 23:14 and bread and corn and fruitful field not you and eat of him until bone the day this until the brought of yous you offering Mighty Ones of yous statutes ever to generationss of yous in all dwellingss of yous"
    );
  }

  @Test
  public void translateEccChapter12() {
    ImmutableList<String> translations = IntStream.range(1, 15).mapToObj(v -> "Ecc 12:" + v)
        .map(this::translate).collect(toImmutableList());
    assertThat(translations).containsExactly(
        "Ecc 12:1 and remember you creators of you in days of youths of you until which not he come of him days of the evil and the reaching of him years which you said none for me beast desire",
        "Ecc 12:2 until which not you darken the sun and the light and the moon and the stars and dwell of him the clouds after the rain",
        "Ecc 12:3 in day that which he moved of him that which and rebellious the house and the you pervert of him mans of the army and cease of him the daub of uss for little of him and darken of him mountain of himI s in windows",
        "Ecc 12:4 and shut of him doors in street in low voice the grinding and he up to voice the bird and he worship of him all daughters the song",
        "Ecc 12:5 also from high fear of him and fears in way and he despise the almond and he burden the grasshopper and you bullock the desire for walk the man unto house ever of him and about of him in street the mourners",
        "Ecc 12:6 until which not he far [UNK] sorrow the silver and you run springs the gold and you break pitcher upon the fountain and we run the wheel unto the pit",
        "Ecc 12:7 and he return the dust upon the land like that which was and the spirit you return unto the Mighty Ones which given",
        "Ecc 12:8 vanity vanitys said the preacher the all vanity",
        "Ecc 12:9 and more that which was preacher wise again teach knowledge you the people and give ear and search make straight proverbs the multiply",
        "Ecc 12:10 seek preacher to find words of desire and write uprightness words of truth",
        "Ecc 12:11 words of wises like goads and like nails planters mans of assemblys given of him from shepherd one",
        "Ecc 12:12 and more from they sons of the warn accomplish books the multiply none end and study the multiply wearys flesh",
        "Ecc 12:13 flags word the all we listen you the Mighty Ones fear and you commandments of him guard for this all the man",
        "Ecc 12:14 for you all work the Mighty Ones he enter in judgment upon all we hide if good and if evil"
    );
  }

  @Test
  public void translateMattChapter5() {
    ImmutableList<String> translations = IntStream.range(17, 20).mapToObj(v -> "Matt 5:" + v)
        .map(this::translate).collect(toImmutableList());
    assertThat(translations).containsExactly(
        "Matt 5:17 Not think that I have come to abolish the law or their prophets not I have come to abolish but to fulfill",
        "Matt 5:18 truly for speak you till whosoever shall pass away which heaven also or earth jot by or one tittle no not shall pass away from his law till whosoever all things come to pass",
        "Matt 5:19 Whoever if therefore shall break one of the commandments these of the least also teaching so their men least is called by the kingdom of the heavens whoever now whosoever shall keep also teaching this great is called by the kingdom of the heavens"
    );
  }

  @Test
  public void translateJubilees() {
    assertThat(translate(new Scripture().setBook("Jubilees").setChapter(49).setVerse(2).setText("בלילה"))).isEqualTo(
        "Jubilees 49:2 in night"
    );
  }

  @Test
  public void rawTranslation_Jub1_1() {
    assertThat(translate(new Scripture().setBook("Jubilees").setChapter(1).setVerse(1).setText("אלה דברי חלוקת הימים על פי התורה והעדות לתולדות השנים לשבועיהן וליובליהן כל ימי השמים על הארץ כאשר דבר אל משה בהר סיני ויהי בשנה הראשונה לצאת בני ישראל מארץ מצרים בחודש השלישי בשישה עשר בו וידבר ה' אל משה לאמור"))).isEqualTo(
        "Jubilees 1:1 Mighty One words of [UNK] the days upon mouth the law and the testimony to generations the years to oaths of us and to jubilees of us all days of the heavens upon the land like which word unto Moses in mountain Sinai and there came to be in year the first to go out sons of Yasharael from land Mizraim in month the third in six ten in it and he word Yah unto Moses to say"
    );
  }

  @Test
  public void rawTranslation1() {
    assertThat(translate(new Scripture().setBook("Jubilees").setChapter(6).setVerse(47).setText("אתה צו את בני ישראל ושמרו את השנים על פי המספר הזה ארבעה ושישים יום ושלוש מאות יום"))).isEqualTo(
        "Jubilees 6:47 she command you sons of Yasharael and keep of him you the years upon mouth the number this four and sixty day and three hundred day"
    );
  }

  @Test
  public void rawTranslation2() {
    assertThat(translate(new Scripture().setBook("Jubilees").setChapter(49).setVerse(1).setText("זכור את המצוה אשר נתן ה' לך על דבר הפסח לשמור אותו במועדו בארבעה עשר לחודש הראשון"))).isEqualTo(
        "Jubilees 49:1 remember you the commandment which given Yah for yourself upon word the passover to guard him in feast of him in four ten to month the first"
    );
  }

  @Test
  public void rawTranslation3() {
    assertThat(translate(new Scripture().setBook("Jubilees").setChapter(49).setVerse(2).setText("כי תשחט אותו בין הערבים ויאכלו אותו בלילה ערב החמשה עשר מעת בא השמש"))).isEqualTo(
        "Jubilees 49:2 for you kill him between the evenings and he eat of him him in night evening the five ten from time enter the sun"
    );
  }

  private String translate(String ref) {
    return ref + " " + machineTranslator.translate(interlinearStore.getInterlinear(ref)).stream()
        .map(i -> i.getSubTokens().stream().map(SubToken::getTranslation).collect(joining()))
        .collect(joining(" ")).trim();
  }

  private String translate(Scripture scripture) {
    return scripture.reference() + " " + machineTranslator.translate(scripture).stream()
        .map(i -> i.getSubTokens().stream().map(SubToken::getTranslation).collect(joining()))
        .collect(joining(" ")).trim();
  }
}

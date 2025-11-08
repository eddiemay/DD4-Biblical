package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.tools.TranslationTool;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.View;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.ChangeTracker;
import com.digitald4.common.storage.DAOFileBasedImpl;
import com.digitald4.common.storage.DAOFileDBImpl;
import com.digitald4.common.storage.SearchIndexer;
import com.digitald4.common.storage.SearchIndexerFakeImpl;
import com.google.common.collect.ImmutableList;

import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

public class MachineTranslationIntegrationTest {
  private static final SearchIndexer fakeSearchIndexer = new SearchIndexerFakeImpl();
  private static final ChangeTracker changeTracker = new ChangeTracker(null, null, fakeSearchIndexer, null);
  private static final DAOFileDBImpl daoFileDB = new DAOFileDBImpl(changeTracker);
  private static final DAOFileBasedImpl fileDao = new DAOFileBasedImpl(changeTracker,"data/interlinear.jsonl").loadFromFile();
  private static final APIConnector apiConnector = new APIConnector(Constants.API_URL, Constants.API_VERSION, 50);
  private static final BibleBookStore bibleBookStore = new BibleBookStore(() -> daoFileDB);
  private static final InterlinearFetcher interlinearFetcher = new ScriptureFetcherBibleHub(apiConnector);
  private static final ScriptureReferenceProcessor scriptureReferenceProcessor = new ScriptureReferenceProcessorSplitImpl(bibleBookStore);
  private static final InterlinearStore interlinearStore =
      new InterlinearStore(() -> fileDao, scriptureReferenceProcessor, interlinearFetcher);

  private static final TokenWordStore tokenWordStore =
      new TokenWordStore(TranslationTool::tokenWordProvider, TranslationTool::lexiconProvider);
  private static final MachineTranslator machineTranslator =
      new MachineTranslator(tokenWordStore, new HebrewTokenizer(tokenWordStore));
  private static final ScriptureStore scriptureStore = new ScriptureStore(() -> fileDao, fakeSearchIndexer,
      bibleBookStore, scriptureReferenceProcessor, new ScriptureFetcherRouter(apiConnector), interlinearStore, machineTranslator);

  @AfterClass
  public static void tearDown() {
    fileDao.saveToFile();
    daoFileDB.saveFiles();
  }

  @Test
  public void translate7Days() {
    assertThat(translate("Gen 1:1-2:3", "he")).containsExactly(
        "Genesis 1:1 in beginning create Mighty Ones you the heavens and you the land",
        "Genesis 1:2 and the land was vain and void and darkness upon face deep and spirit Mighty Ones hovering upon face the waters",
        "Genesis 1:3 and he said Mighty Ones let there be light and there came to be light",
        "Genesis 1:4 and he see Mighty Ones you the light for good and he separate Mighty Ones between the light and between the darkness",
        "Genesis 1:5 and he call Mighty Ones to light day and to darkness call night and there came to be evening and there came to be morning day one",
        "Genesis 1:6 and he said Mighty Ones let there be firmament in midst the waters and there came to be division between waters to waters",
        "Genesis 1:7 and he make Mighty Ones you the firmament and he separate between the waters which from under to firmament and between the waters which from upon to firmament and there came to be so",
        "Genesis 1:8 and he call Mighty Ones to firmament heavens and there came to be evening and there came to be morning day second",
        "Genesis 1:9 and he said Mighty Ones he wait the waters from under the heavens unto places one and you see the dry land and there came to be so",
        "Genesis 1:10 and he call Mighty Ones to dry land land and to collection the waters call seas and he see Mighty Ones for good",
        "Genesis 1:11 and he said Mighty Ones you spring the land grass herb from sowing seed tree fruit maker fruit to kind of him which seed of him in it upon the land and there came to be so",
        "Genesis 1:12 and you bring out the land grass herb from sowing seed to kind of it and tree maker fruit which seed of him in it to kind of it and he see Mighty Ones for good",
        "Genesis 1:13 and there came to be evening and there came to be morning day third",
        "Genesis 1:14 and he said Mighty Ones let there be lights in firmament the heavens to the separation between the day and between the night and let it be to signs and to feasts and to days and years",
        "Genesis 1:15 and let it be to lights in firmament the heavens to shine upon the land and there came to be so",
        "Genesis 1:16 and he make Mighty Ones you two the lights the greats you the light the great to dominates the day and you the light the lesser to dominates the night and you the stars",
        "Genesis 1:17 and he set them Mighty Ones in firmament the heavens to shine upon the land",
        "Genesis 1:18 and to rule in day and in night and to the separation between the light and between the darkness and he see Mighty Ones for good",
        "Genesis 1:19 and there came to be evening and there came to be morning day fourth",
        "Genesis 1:20 and he said Mighty Ones let abound the waters creeping soul live and bird he fly upon the land upon face firmament the heavens",
        "Genesis 1:21 and he create Mighty Ones you the dragons the greats and you all soul the live the moves which abound the waters to kind of them and you all bird wing to kind of it and he see Mighty Ones for good",
        "Genesis 1:22 and he bless them Mighty Ones to say increase and multiply and fill of him you the waters in seas and the bird let multiply in land",
        "Genesis 1:23 and there came to be evening and there came to be morning day fifth",
        "Genesis 1:24 and he said Mighty Ones you bring out the land soul live to kind beast and move and his life land to kind and there came to be so",
        "Genesis 1:25 and he make Mighty Ones you lives the land to kind and you the beast to kind and you all move the land to kind of it and he see Mighty Ones for good",
        "Genesis 1:26 and he said Mighty Ones we make man in image of us like likeness of us and he rule in fish the sea and in bird the heavens and in beast and in all the land and in all the move the move upon the land",
        "Genesis 1:27 and he create Mighty Ones you the man in image of him in image Mighty Ones create him male and female create them",
        "Genesis 1:28 and he bless them Mighty Ones and he said to them Mighty Ones increase and multiply and fill of him you the land and subdue of him and rule in fish the sea and in bird the heavens and in all live the moves upon the land",
        "Genesis 1:29 and he said Mighty Ones behold gives of to them you all herb sow seed which upon face all the land and you all the tree which in it fruit tree sow seed to them let there be to food",
        "Genesis 1:30 and to all lives the land and to all bird the heavens and to all move upon the land which in it soul live you all green herb to food and there came to be so",
        "Genesis 1:31 and he see Mighty Ones you all which make and behold good very and there came to be evening and there came to be morning day the sixth",
        "Genesis 2:1 and he completed the heavens and the land and all hosts",
        "Genesis 2:2 and he complete Mighty Ones in day the seventh work of him which make and he cease in day the seventh from all work of him which make",
        "Genesis 2:3 and he bless Mighty Ones you day the seventh and he sanctify him for in it shabbat from all work of him which create Mighty Ones to accomplish"
    );
  } // נוח

  @Test
  public void translateChoiceFew() {
    assertThat(translate("Isa 9:6, Psa 83:18, Exo 12:11, Gen 10:1, Lev 25:4, Exo 20:2, Lev 23:14", "he")).containsExactly(
        "Isaiah 9:6 for child born to of us son given to of us and you will be the government upon shoulder of him and he call name of him wonderful counselor God mighty I in he forever prince peace",
        "Psalms 83:18 and know of him for you are name of you יהוה to alone of you high upon all the land",
        "Exodus 12:11 and like this you and eat of him him waists of yous belts shoes of yous in foots of yous and rod of yous in hand of yous and eats him in haste passover he to יהוה",
        "Genesis 10:1 and these generations sons of Noah Shem Ham and Japheth and born of him to them sons after the flood",
        "Leviticus 25:4 and in year the sevenths shabbat shabbat of it let there be to land shabbat to יהוה field of you not you sow and vineyard of you not you prune",
        "Exodus 20:2 I יהוה Mighty Ones of you which the bring outss of you from land Mizraim from house servants",
        "Leviticus 23:14 and bread and corn and fruitful field not you and eat of him until bone the day this until the brought of yous you offering Mighty Ones of yous statutes ever to generationss of yous in all dwellingss of yous"
    );
  }

  @Test
  public void translateEccChapter12() {
    assertThat(translate("Ecc 12", "he")).containsExactly(
        "Ecclesiastes 12:1 and remember you creators of you in days of youths of you until which not he come of him days of the evil and the reaching of him years which you said none for me beast desire",
        "Ecclesiastes 12:2 until which not you darken the sun and the light and the moon and the stars and dwell of him the clouds after the rain",
        "Ecclesiastes 12:3 in day that which he moved of him that which and rebellious the house and the you pervert of him mans of the army and cease of him the daub of uss for little of him and darken of him the saws in windows",
        "Ecclesiastes 12:4 and shut of him doors in street in low sound the grinding and he up to sound the bird and he worship of him all daughters the song",
        "Ecclesiastes 12:5 also from high fear of him and fears in way and he despise the almond and he burden the grasshopper and you bullock the desire for walk the man unto house ever of him and about of him in street the mourners",
        "Ecclesiastes 12:6 until which not he far [UNK] sorrow the silver and you run springs the gold and you break pitcher upon the fountain and we run the wheel unto the pit",
        "Ecclesiastes 12:7 and he return the dust upon the land like that which was and the spirit you return unto the Mighty Ones which given",
        "Ecclesiastes 12:8 vanity vanitys said the preacher the all vanity",
        "Ecclesiastes 12:9 and more that which was preacher wise again teach knowledge you the people and give ear and search make straight proverbs the multiply",
        "Ecclesiastes 12:10 seek preacher to find words of desire and write uprightness words of truth",
        "Ecclesiastes 12:11 words of wises like goads and like nails planters mans of assemblys given of him from shepherd one",
        "Ecclesiastes 12:12 and more from they sons of the warn accomplish books the multiply none end and study the multiply wearys flesh",
        "Ecclesiastes 12:13 flags word the all we listen you the Mighty Ones fear and you commandments of him guard for this all the man",
        "Ecclesiastes 12:14 for you all work the Mighty Ones he enter in judgment upon all we hide if good and if evil"
    );
  }

  @Test
  public void translateMattChapter5() {
    assertThat(translate("Matt 5:17-19", "gk")).containsExactly(
        "Matthew 5:17 Not think that come of I to abolish the law or their prophets not come of I to abolish but to fulfill",
        "Matthew 5:18 truly for speak you till whosoever shall pass away which heaven also or earth jot of he or one tittle no not shall pass away from his law till whosoever all things come to pass",
        "Matthew 5:19 Whoever if therefore shall break one of the commandments these of the least also teaching so their men least is called by the kingdom of the heavens whoever now whosoever shall keep also teaching this great is called by the kingdom of the heavens"
    );
  }

  @Test
  public void translateGreek() {
    assertThat(translate("Matt 26:17, Mark 14:12, Luke 22:7, John 13:1", "gk")).containsExactly(
            "Matthew 26:17 The then first of the unleavened came the disciples to יהושע saying Where want of you prepare of we thee eat of the Passover",
            "Mark 14:12 Also the first day of the unleavened when the Passover kill of I said to him the disciples there Where want of you go of we prepare of we that eat of the Passover",
            "Luke 22:7 Come of he then or day of the unleavened by or necessary kill of him the Passover",
            "John 13:1 Before then theing feasting his Passover knowing which יהושע that come of he there or hour that [UNK] of his [UNK] this unto the [UNK] [UNK] their [UNK] their by to [UNK] into end [UNK] them");

  }

  @Test
  public void rawTranslation_Jub() {
    assertThat(translate("Jub 1:1-2,6:47,49:1-2", "he")).containsExactly(
        "Jubilees 1:1 Mighty One words of [UNK] the days upon mouth the law and the testimony to generations the years to weeks of us and to jubilees of us all days of the heavens upon the land like which word unto Moses in mountain Sinai",
        "Jubilees 1:2 and there came to be in year the first to go out sons of Yasharael from land Mizraim in month the third in six ten in it and he word [UNK] unto Moses to say",
        "Jubilees 6:47 and she command you sons of Yasharael and keep of him you the years upon mouth the number this four and sixty day and three hundred day",
        "Jubilees 49:1 remember you the commandment which given Yah for yourself upon word the passover to guard him in feast of him in four ten to month the first",
        "Jubilees 49:2 for you kill him between the evenings and he eat of him him in night [UNK] the five ten from time come the sun"
    );
  }

  @Test @Ignore
  public void geezLetterCount() {
    assertThat(scriptureStore.getScriptures("ISR", "gez", "Gen 1:1-2:30, Exo 15,20, Matt 5", View.Text).getItems().stream()
        .map(s -> HebrewConverter.toRestored(s.getText().toString()))
        .flatMapToInt(String::chars)
        .distinct()
        .filter(c -> c != ' ')
        .sorted()
        .mapToObj(c -> (char) c)
        .collect(toImmutableList())).hasSize(22);
  }

  @Test
  public void geezTranslation() {
    assertThat(translate("Lev 8:1,5, Num 28:21, Psa 83:18, Exo 16:36", "gez"))
        .containsExactly(
            "Leviticus 8:1 יהוה and to Mosheh as this said he spoke",
            "Leviticus 8:5 Mosheh and the assembly was יהוה to do that which command word this it is he said",
            "Numbers 28:21 the of them seven lamb each lamb tenth mouth man",
            "Psalms 83:18 you master that which name of you alone at high all land high like that which it was of you let know to them",
            "Exodus 16:36 Gomer however tenth mouth mouth it is");
  }

  @Test
  public void geezTranslation_greatWords() {
    assertThat(translate("Exo 20", "gez")).containsExactly(
            "Exodus 20:1 יהוה and this words this all as this said he spoke",
        "Exodus 20:2 out of land Mitsrayim out of house slavery I brought out of you I יהוה the Mighty One of you it is",
        "Exodus 20:3 others the Mighty Ones at before me there is no of you",
        "Exodus 20:4 at high at sky out of existing at down and at land from the existing at water said at down land from the existing carve of him image anything of him for of you not you do",
        "Exodus 20:5 not you bow down of them not you serve of them of him and I יהוה the Mighty One of you jealous the Mighty One it is so visiting of him sins fathers at son until third until fourth of him generation who punishes",
        "Exodus 20:6 to that which decide exist of him commandments me to that which keep exist however until the thousand mercys that which do it is",
        "Exodus 20:7 יהוה name in vain to that which high I cause cut of him the he complete of him it is so name יהוה the Mighty One of you in vain not you high",
        "Exodus 20:8 days Shabbat holy set-apart remember",
        "Exodus 20:9 six days work action all do",
        "Exodus 20:10 was the seventh however of the יהוה the Mighty One of you Shabbat it is by her you of him son of you daughter of you servant of you servants of you animal of you at inside gate of you that which dwell stranger of him some through work not you work",
        "Exodus 20:11 יהוה in six days sky of him land of him sea of him in them desire all of him made it is so in the seventh days said he rested thus יהוה to days Shabbat blessed of him set-apart of him",
        "Exodus 20:12 father יהוה the Mighty One of you that which give of you land age of you in order that prolong at of you mother of you the honor",
        "Exodus 20:13 not you kill",
        "Exodus 20:14 not you commit adultery",
        "Exodus 20:15 not you steal",
        "Exodus 20:16 at in neighbour in lies not you testify",
        "Exodus 20:17 house in neighbour not you covet wife in neighbour servant servants bull of him donkey money in neighbour anything all and not you covet",
        "Exodus 20:18 all of them people and to sound of him intensities of him sound Mighty Ones of him was mountain of him to existing of him seen them people this Mosheh seen and trembled afar of them and weak he said",
        "Exodus 20:19 to Mosheh and you word of him to name of him I the Mighty One however in order that not to death the he speak of him they said",
        "Exodus 20:20 Mosheh and the of them people sins in order that not to do fears at before of you of them to exist the Mighty One in order that to worship of them it is that which come so not you fear he said of them",
        "Exodus 20:21 was people at afar stand Mosheh exists the Mighty One existing darkness draw near",
        "Exodus 20:22 יהוה and to Mosheh he said to son Yasharael as this he said of them I from the sky like the he spoke of you of you of them foods of them see of you of them",
        "Exodus 20:23 Mosheh me the Mighty Ones not you do the Mighty Ones silver of him the Mighty Ones gold of him for of yous of them not you do",
        "Exodus 20:24 alter from soil stretch out of him me at upon and burnt offerings altersss of you alters praise of you the sheep of you [UNK] man name me at that which remember of you of him all house exist of you come honor of you of you it is",
        "Exodus 20:25 alter so of him you do of you of him me [UNK] you [UNK] [UNK] you so [UNK] so of him not you to son",
        "Exodus 20:26 exist alter me me at upon nakeds of you not ascendhis in carve of him not you climb");
  }

  @Test
  public void geezTranslation_Psa117() {
    assertThat(translate("Psalms 117", "gez")).containsExactly(
        "Psalms 117:1 all of you of them the people to יהוה the praiseand all of you of them nations and rejoice",
        "Psalms 117:2 afflicted at of him great it is so truth יהוה and to desire them he dwell it is release me");
  }

  @Test
  public void geezTranslation_randomFew() {
    assertThat(translate("Gen 35:23, Exo 15:2", "gez")).containsExactly(
        "Genesis 35:23 son Leah Ruban first born Jacob of him Simeon of him Levi of him Yuda of him Issachar of him Zebulun of him",
        "Exodus 15:2 יהוה [UNK] [UNK] it is [UNK] [UNK] said came to be of him [UNK] this the Mighty One me it is so [UNK] it is [UNK] the Mighty One at me it is out of out of of him it is");
  }

  @Test
  public void geezTranslation_7Days() {
    assertThat(translate("Genesis 1:1-2:3", "gez")).containsExactly(
        "Genesis 1:1 the Mighty One in beginnings sky of him land of him created",
        "Genesis 1:2 land and blessed of him empty dwells darkness said at high abyss dwell spirit the Mighty One and at high waters hovering dwell",
        "Genesis 1:3 the Mighty One said light let it be he said light and it was",
        "Genesis 1:4 the Mighty One and was light good like that which it was seen the Mighty One said the light out of darkness divided",
        "Genesis 1:5 the Mighty One the light days he called the darkness said night he called evening came to be morning came to be first days",
        "Genesis 1:6 the Mighty One and to waters out of waters separate firmament at between waters let it be he said",
        "Genesis 1:7 the Mighty One the firmament do the at down firmament desire waters and from thes at high firmament desire waters divided likewise came to be",
        "Genesis 1:8 the Mighty One said the firmament sky he called evening came to be morning came to be seconds days",
        "Genesis 1:9 the Mighty One and was to clean in order that mountainthe his was at down sky desire waters exist first house he to the out of he said as it is and it was",
        "Genesis 1:10 the Mighty One said the to clean land he called the the out of waters and sea he called the Mighty One said good like that which it was seen",
        "Genesis 1:11 the Mighty One and was land hair of him that which see that which give in word of him that which see at seehis existing fruit just as kinds at land that which fruit so to go out he said as it is and it was",
        "Genesis 1:12 was land hair of him just as kinds that which see that which give in word of him that which see at seehis existing fruit that which fruit himand  of them said the go outs the Mighty One and good like that which it was seen",
        "Genesis 1:13 evening came to be morning came to be thirds days",
        "Genesis 1:14 the Mighty One and to days from the night separate me lights at firmament sky let it be to flocks of him to appointed times of him yearss of him said to chosen ones let it be",
        "Genesis 1:15 at land to blessing at firmament sky lights let it be he said as it is and it was",
        "Genesis 1:16 the Mighty One said two greats lights do was great light in days to bind of him was lesser light and in night to bind of him to and from thes exist do",
        "Genesis 1:17 at high land in order that to bless at days of him at night of him and to bind of him to light exist out of darkness to divided the Mighty One at firmament sky do of them the Mighty One and good like that which it was seen",
        "Genesis 1:19 evening came to be morning came to be fourths days",
        "Genesis 1:20 the Mighty One and waters life soul existing swarming anything the creature he go out at high land at down firmament sky said the birds he to fruit he said",
        "Genesis 1:21 the Mighty One and the of them greats the creature sea of him the waters that which go out in in kinds life soul existing swarming anything all of him bird existing of him in in kinds of him all of him the birds of him created the Mighty One said good like that which it was seen",
        "Genesis 1:22 the Mighty One and fruit me of him to many of him to waters sea animalthe and the birds said at land he many said blessed of them",
        "Genesis 1:23 evening came to be morning came to be fifths days",
        "Genesis 1:24 the Mighty One and land life soul existing in in kinds the creature of him creeps of him beasts land of him in in kinds to go out he said likewise came to be",
        "Genesis 1:25 the Mighty One said beasts land in in kinds of him the creature in in kinds of him all creeps land and in in kinds do the Mighty One and good like that which it was seen",
        "Genesis 1:26 the Mighty One said in image of him as carve of him man to do fish sea of him to birds sky of him to the creature of him existthat which  of him land of him at land creep to anything all creeps of him he servant he said",
        "Genesis 1:27 the Mighty One and in image man created in image the Mighty One created to male of him the female of him made created of them",
        "Genesis 1:28 the Mighty One said blessed of them the Mighty One and fruit me of him to many of him to land and animalthe  exist Mighty One exist fish sea of him to birds sky of him at land creep to anything all the creature of him said servant he said of them",
        "Genesis 1:29 the Mighty One and behold at high all land that which see existing all the protects of him that which that which see that which see existing fruit so of the all himand  of them of him give of them said of you so food let it be of them",
        "Genesis 1:30 to all beasts land of him to all of him the birds sky of him life soul to existing at land creep to anything all and all for animal of them hair to food of them give me of them said of you he said as it is and it was",
        "Genesis 1:31 the Mighty One said that which do anything all seen behold many good came to be evening came to be morning came to be sixths days",
        "Genesis 2:1 as it is sky of him land of him all hosts of them of him to complete of them",
        "Genesis 2:2 the Mighty One said the that which do do house the seventh days complete of them in the seventh days and from thes that which do all do he rested",
        "Genesis 2:3 the Mighty One said from thes that which created of him that which do of him all do by her because that which he rested the the seventh days blessed of him set-apart of him");
  }

  private ImmutableList<String> translate(String ref, String lang) {
    return scriptureStore.getScriptures("ISR", lang, ref, View.Interlinear).getItems().stream()
        .map(s -> (InterlinearScripture) s)
        .map(is -> is.reference() + " " + is.getInterlinears().stream().map(i -> i.getSubTokens().stream().map(SubToken::getTranslation).collect(joining()).trim()).collect(joining(" ")).trim())
        .collect(toImmutableList());
  }
}

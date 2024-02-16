package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.LexiconStore;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import com.digitald4.biblical.tools.TranslationTool;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOFileBasedImpl;
import com.digitald4.common.storage.DAOInMemoryImpl;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;

public class MachineTranslationIntegrationTest {
  private static final DAOInMemoryImpl inMemoryDao = new DAOInMemoryImpl();
  private static final LexiconStore lexiconStore = new LexiconStore(() -> inMemoryDao, null);
  private final DAOFileBasedImpl fileDao = new DAOFileBasedImpl("data/interlinear.db").loadFromFile();
  private final APIConnector apiConnector =
      new APIConnector(Constants.API_URL, Constants.API_VERSION, 50);
  private final StaticDataDAO staticDataDAO = new StaticDataDAO();
  private final BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
  private final InterlinearFetcher interlinearFetcher = new ScriptureFetcherBibleHub(apiConnector);
  private final InterlinearStore interlinearStore = new InterlinearStore(
      () -> fileDao, new ScriptureReferenceProcessorSplitImpl(bibleBookStore), interlinearFetcher);

  private final TokenWordStore tokenWordStore =
      new TokenWordStore(() -> inMemoryDao, TranslationTool::tokenWordProvider, lexiconStore);
  private final MachineTranslator machineTranslator =
      new MachineTranslator(tokenWordStore, new HebrewTokenizer(tokenWordStore));

  @Test
  public void translate7Days() {
    ImmutableList<String> translations = IntStream.range(1, 35)
        .mapToObj(v -> String.format("Genesis %d:%d", v < 32 ? 1 : 2, v < 32 ? v : v - 31))
        .map(this::translate)
        .collect(toImmutableList());

    assertThat(translations).containsExactly(
        "Genesis 1:1 in beginning created Mighty Ones you the heavens and you the earth",
        "Genesis 1:2 and the earth was formless and void and darkness upon face deep and spirit Mighty Ones hovering upon face the waters",
        "Genesis 1:3 and he said Mighty Ones let there be light and there came to be light",
        "Genesis 1:4 and he saw Mighty Ones you the light for good and he separate Mighty Ones between the light and between the darkness",
        "Genesis 1:5 and he call Mighty Ones to light day and to darkness call night and there came to be evening and there came to be morning day one",
        "Genesis 1:6 and he said Mighty Ones let there be firmament in midst the waters and there came to be division between waters to waters",
        "Genesis 1:7 and he made Mighty Ones you the firmament and he separate between the waters which from under to firmament and between the waters which from upon to firmament and there came to be so",
        "Genesis 1:8 and he call Mighty Ones to firmament heavens and there came to be evening and there came to be morning day second",
        "Genesis 1:9 and he said Mighty Ones let be gathered the waters from under the heavens unto places one and you see the dry land and there came to be so",
        "Genesis 1:10 and he call Mighty Ones to dry land earth and to collection the waters call seas and he saw Mighty Ones for good",
        "Genesis 1:11 and he said Mighty Ones you spring the earth grass herb from sowing seed tree fruit made fruit to kind of him which seed of him in it upon the earth and there came to be so",
        "Genesis 1:12 and you bring out the earth grass herb from sowing seed to kind of it and tree made fruit which seed of him in it to kind of it and he saw Mighty Ones for good",
        "Genesis 1:13 and there came to be evening and there came to be morning day third",
        "Genesis 1:14 and he said Mighty Ones let there be lumination of you in firmament the heavens to the separation between the day and between the night and let it be to sign of you and to feasts and to days and years",
        "Genesis 1:15 and let it be to lumination of you in firmament the heavens to shine upon the earth and there came to be so",
        "Genesis 1:16 and he made Mighty Ones you two the lumination of you the greats you the lumination the great to dominate of you the day and you the lumination the small to dominate of you the night and you the stars",
        "Genesis 1:17 and he set them Mighty Ones in firmament the heavens to shine upon the earth",
        "Genesis 1:18 and to rule in day and in night and to the separation between the light and between the darkness and he saw Mighty Ones for good",
        "Genesis 1:19 and there came to be evening and there came to be morning day fourth",
        "Genesis 1:20 and he said Mighty Ones let abound the waters creeping soul live and bird he fly upon the earth upon face firmament the heavens",
        "Genesis 1:21 and he created Mighty Ones you the dragons the greats and you all soul the live the tread of you which abound the waters to kind of them and you all bird wing to kind of it and he saw Mighty Ones for good",
        "Genesis 1:22 and he bless them Mighty Ones to said increase and multiply and fill of him you the waters in seas and the bird let multiply in earth",
        "Genesis 1:23 and there came to be evening and there came to be morning day fifth",
        "Genesis 1:24 and he said Mighty Ones you bring out the earth soul live to kind beast and creeping and his life earth to kind and there came to be so",
        "Genesis 1:25 and he made Mighty Ones you live of you the earth to kind and you the beast to kind and you all creeping the land to kind of it and he saw Mighty Ones for good",
        "Genesis 1:26 and he said Mighty Ones we made man in image of us like likeness of us and he rule in fish the sea and in bird the heavens and in beast and in all the earth and in all the creeping the tread upon the earth",
        "Genesis 1:27 and he created Mighty Ones you the man in image of him in image Mighty Ones created him male and female created them",
        "Genesis 1:28 and he bless them Mighty Ones and he said to them Mighty Ones increase and multiply and fill of him you the earth and subdue and rule in fish the sea and in bird the heavens and in all live the tread of you upon the earth",
        "Genesis 1:29 and he said Mighty Ones behold give of yous of to them you all herb sow seed which upon face all the earth and you all the tree which in it fruit tree sow seed to them let there be to food",
        "Genesis 1:30 and to all live of you the earth and to all bird the heavens and to all that move upon the earth which in it soul live you all green herb to food and there came to be so",
        "Genesis 1:31 and he saw Mighty Ones you all which made and behold good very and there came to be evening and there came to be morning day the sixth",
        "Genesis 2:1 and he completed the heavens and the earth and all hosts",
        "Genesis 2:2 and he complete Mighty Ones in day the seventh work of him which made and he shabbat in day the seventh from all work of him which made",
        "Genesis 2:3 and he bless Mighty Ones you day the seventh and he sanctify him for in it shabbat from all work of him which created Mighty Ones to accomplish"
    );
  }

  @Test
  public void translateChoiceFew() {
    ImmutableList<String> translations = Stream.of("Isa 9:6", "Psa 83:18", "Exo 12:11", "Gen 10:1")
        .map(this::translate).collect(toImmutableList());
    assertThat(translations).containsExactly(
        "Isa 9:6 for child beget to of us son given to of us and you will be the government upon shoulder of him and he call name of him wonderful counselor God mighty I in he forever prince peace",
        "Psa 83:18 and know of him for you are name of you יהוה to alone of you high upon all the earth",
        "Exo 12:11 and like this you eat of him him waists of yous belts shoes of yous in foots of yous and rod of yous in hand of yous and eat of yous him in haste passover he to יהוה",
        "Gen 10:1 and these generations of you sons of Noah Shem Ham and Japheth and born of him to them sons after the flood"
    );
  }

  private String translate(String ref) {
    return ref + " " + machineTranslator.translate(interlinearStore.getInterlinear(ref)).stream()
        .map(i -> i.getSubTokens().stream().map(SubToken::getTranslation).collect(joining()))
        .collect(joining(" ")).trim();
  }
}

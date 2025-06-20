package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.model.ScriptureVersion;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.LexiconStore;
import com.digitald4.biblical.store.ScriptureStore;
import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.util.Constants;
import com.digitald4.biblical.util.HebrewTokenizer;
import com.digitald4.biblical.util.InterlinearFetcher;
import com.digitald4.biblical.util.Language;
import com.digitald4.biblical.util.LexiconFetcher;
import com.digitald4.biblical.util.LexiconFetcherBlueLetterImpl;
import com.digitald4.biblical.util.MachineTranslator;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.biblical.util.ScriptureFetcher;
import com.digitald4.biblical.util.ScriptureFetcherBibleGateway;
import com.digitald4.biblical.util.ScriptureFetcherBibleHub;
import com.digitald4.biblical.util.ScriptureFetcherJWOrg;
import com.digitald4.biblical.util.ScriptureFetcherKJV1611;
import com.digitald4.biblical.util.ScriptureFetcherOneOff;
import com.digitald4.biblical.util.ScriptureFetcherPseudepigrapha;
import com.digitald4.biblical.util.ScriptureFetcherRouter;
import com.digitald4.biblical.util.ScriptureFetcherSefariaOrg;
import com.digitald4.biblical.util.ScriptureFetcherStepBibleOrg;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.View;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.*;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Objects;

public class TranslationTool {
  private final ImmutableSet.Builder<Interlinear> hasTranslationDiff = ImmutableSet.builder();
  private final ScriptureStore scriptureStore;

  public TranslationTool(ScriptureStore scriptureStore) {
    this.scriptureStore = scriptureStore;
  }

  public void translateAndPrint(String reference) {
    scriptureStore.getScriptures("WLC", Language.EN, reference, View.Interlinear).getItems()
        .stream()
        .map(s -> (InterlinearScripture) s)
        .forEach(this::printTranslation);
  }

  public void printTranslation(InterlinearScripture scripture) {
    System.out.println("\n" + scripture.reference());
    ImmutableList<Interlinear> translated = scripture.getInterlinears();
    System.out.println(translated.stream().map(Interlinear::getStrongsId).collect(joining(" ")));
    System.out.println(translated.stream()
        .map(i -> i.getSubTokens().stream().map(SubToken::getStrongsId).filter(Objects::nonNull).collect(joining(".")))
        .collect(joining(" ")));
    System.out.println(translated.stream().map(Interlinear::getTranslation).collect(joining(" ")));
    System.out.println(translated.stream().map(Interlinear::getConstantsOnly).collect(joining(" ")));
    System.out.println(
        translated.stream()
            .map(i -> i.getSubTokens().stream().map(SubToken::getWord).collect(joining(".")))
            .collect(joining(" ")));
    System.out.println(
        translated.stream()
            .map(i -> i.getSubTokens().stream().map(SubToken::getTranslation).collect(joining("")))
            .collect(joining(" ")));
    hasTranslationDiff.addAll(
        translated.stream()
            .filter(i -> i.getConstantsOnly().length() > 1)
            .filter(i ->
                i.getSubTokens().stream().map(SubToken::getStrongsId).noneMatch(s -> Objects.equals(s, i.getStrongsId()))
                    || i.getSubTokens().get(0).getTranslation().equals("[UNK]"))
            .collect(toImmutableList()));
  }

  public static ImmutableList<TokenWord> tokenWordProvider() {
    return Constants.VOCAB_FILES.stream()
        .flatMap(file -> loadFromFile(file).stream())
        .collect(toImmutableList());
  }

  public static ImmutableMap<String, Lexicon> lexiconProvider() {
    return readFile("lexicon.csv").stream()
        .skip(1)
        .map(line -> line.split(","))
        .collect(toImmutableMap(values -> values[0], values -> new Lexicon().setId(values[0])
                .setReferenceCount(Integer.parseInt(values[1]))
                .setPartOfSpeech(values.length >= 3 ? values[2] : null)
        ));
  }

  public static ImmutableList<String> readFile(String filename) {
    String file = String.format("src/main/python/services/translation/files/%s", filename);
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (!line.isEmpty()) {
          lines.add(line);
        }
      }
    } catch (Exception ioe) {
      throw new DD4StorageException("Error reading file: " + file, ioe);
    }
    return lines.build();
  }

  public static ImmutableList<TokenWord> loadFromFile(String filename) {
    return readFile(filename).stream()
        .filter(line -> !line.startsWith("*"))
        .map(line -> JSONUtil.toObject(TokenWord.class, line))
        .collect(toImmutableList());
  }

  public static void main(String[] args) {
    ChangeTracker changeTracker = new ChangeTracker(null, null, null, null);
    DAOFileDBImpl daoFileDB = new DAOFileDBImpl(changeTracker);

    APIConnector apiConnector = new APIConnector(Constants.API_URL, Constants.API_VERSION, 50);
    DAOApiImpl apiDao = new DAOApiImpl(apiConnector);
    BibleBookStore bibleBookStore = new BibleBookStore(() -> daoFileDB);
    InterlinearFetcher interlinearFetcher = new ScriptureFetcherBibleHub(apiConnector);
    InterlinearStore interlinearStore = new InterlinearStore(
        () -> daoFileDB, new ScriptureReferenceProcessorSplitImpl(bibleBookStore), interlinearFetcher);
    ScriptureReferenceProcessor scriputureRefProcessor = new ScriptureReferenceProcessorSplitImpl(bibleBookStore);
    ScriptureFetcher scriptureFetcher = new ScriptureFetcherRouter(apiConnector);

    LexiconFetcher lexiconFetcher = new LexiconFetcherBlueLetterImpl(apiConnector);
    LexiconStore lexiconStore = new LexiconStore(() -> apiDao, lexiconFetcher);
    TokenWordStore tokenWordStore =
        new TokenWordStore(TranslationTool::tokenWordProvider, TranslationTool::lexiconProvider);
    var machineTranslator = new MachineTranslator(tokenWordStore, new HebrewTokenizer(tokenWordStore));
    ScriptureStore scriptureStore = new ScriptureStore(() -> daoFileDB, null, bibleBookStore,
        scriputureRefProcessor, scriptureFetcher, interlinearStore, machineTranslator);

    TranslationTool translationTool = new TranslationTool(scriptureStore);
    translationTool.translateAndPrint("Gen 48:17-18");
    // translationTool.translateAndPrint("Isa 9:6, Psa 83:18, Gen 14:18,16:3,19:8,19:12");
    // translationTool.translateAndPrint("Isa 1-66");
    /* translationTool.translateAndPrint("Exo 20");
    translationTool.translateAndPrint("Lev 23");
    translationTool.translateAndPrint("Dan 8:11");
    translationTool.translateAndPrint("Gen 10:1");
    translationTool.translateAndPrint("Isa 9:6");
    translationTool.translateAndPrint("Isa 9:6 (WLCO)",
        "כי־ילד ילד־לנו בן נתן־לנו ותהי המשרה על־שכמו ויקרא שמו פלא יועץ אל גבור אביעד שר־שלום");
    translationTool.translateAndPrint("Isa 9:6 (DSS)",
        "כי ילד יולד לנו בן נתן לנו ותהי המשורה על שכמו וקרא שמו פלא יועץ אל גבור אבי עד שר השלום");
    translationTool.translateAndPrint("Jub 1:1",
        "אלה דברי חלוקת הימים על פי התורה והעדות לתולדות השנים לשבועיהן וליובליהן כל ימי השמים על הארץ כאשר דבר אל משה בהר סיני:\n"
            + "ויהי בשנה הראשונה לצאת בני ישראל מארץ מצרים בחודש השלישי בשישה עשר בו וידבר ה' אל משה לאמור:");
    translationTool.translateAndPrint("Jub 1:2",
        "עלה אלי פה ההרה ואתנה לך את שתי לוחות האבן והתורה והמצווה אשר כתבתי להורותם:"); */
    // translationTool.translateAndPrint("Jub 6:45-46");

    System.out.println("\nIndex, Hebrew, KJV Translation, StrongsId, Strongs Hebrew, StrongsIds, Breakdown, MT");
    translationTool.hasTranslationDiff.build().stream()
        // .sorted(Comparator.comparing(Interlinear::getId))
        .filter(i -> i.getSubTokens().get(0).getTranslation().equals("[UNK]"))
        .forEach(i -> System.out.printf("%s, %s, %s, %s, %s, %s, %s, %s\n",
            i.getId(), i.getConstantsOnly(), i.getTranslation(), i.getStrongsId(),
            i.getStrongsId() == null ? null : lexiconStore.get(i.getStrongsId()).restored(),
            i.getSubTokens().stream().map(SubToken::getStrongsId).filter(Objects::nonNull).collect(joining(" ")),
            i.getSubTokens().stream().map(SubToken::getWord).collect(joining(".")),
            i.getSubTokens().stream().map(SubToken::getTranslation).collect(joining(""))));

    /* printTranslation(
        machineTranslator.translate(
            new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(9).setVerse(6).setText(
                "כי ילד יולד לנו בן נתן לנו ותהי המשורה על שכמו וקרא שמו פלא יועץ אל גבור אבי עד שר השלום"))); */

    daoFileDB.saveFiles();
  }
}

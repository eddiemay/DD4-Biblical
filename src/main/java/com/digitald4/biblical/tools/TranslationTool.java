package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.LexiconStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import com.digitald4.biblical.util.Constants;
import com.digitald4.biblical.util.LexiconFetcher;
import com.digitald4.biblical.util.LexiconFetcherBlueLetterImpl;
import com.digitald4.biblical.util.MachineTranslator;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.biblical.util.ScriptureReferenceProcessorSplitImpl;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOFileBasedImpl;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

public class TranslationTool {
  private final ImmutableSet.Builder<Interlinear> hasTranslationDiff = ImmutableSet.builder();
  private final MachineTranslator machineTranslator;
  private final InterlinearStore interlinearStore;

  public TranslationTool(MachineTranslator machineTranslator, InterlinearStore interlinearStore) {
    this.machineTranslator = machineTranslator;
    this.interlinearStore = interlinearStore;
  }

  public void printTranslation(String reference) {
    machineTranslator.translate(interlinearStore.getInterlinear(reference)).stream()
        .collect(toImmutableListMultimap(Interlinear::reference, identity())).asMap()
        .forEach(this::printTranslation);
  }

  public void printTranslation(String reference, Collection<Interlinear> translated) {
    System.out.println("\n" + reference);
    System.out.println(translated.stream().map(Interlinear::getStrongsId).collect(joining(" ")));
    System.out.println(translated.stream()
        .map(i -> i.getSubTokens().stream()
            .map(SubToken::getStrongsId).filter(Objects::nonNull).collect(joining(".")))
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
            .filter(
                i -> i.getSubTokens().stream().map(SubToken::getStrongsId)
                    .noneMatch(s -> Objects.equals(s, i.getStrongsId())))
            .collect(toImmutableList()));
  }

  public static ImmutableList<TokenWord> tokenWordProvider() {
    return Constants.VOCAB_FILES.stream()
        .flatMap(file -> loadFromFile(file).stream())
        .collect(toImmutableList());
  }

  public static ImmutableList<TokenWord> loadFromFile(String filename) {
    String tokenFile = String.format("src/main/webapp/ml/%s", filename);
    ImmutableList.Builder<TokenWord> tokenWords = ImmutableList.builder();
    try (BufferedReader br = new BufferedReader(new FileReader(tokenFile))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.length() > 0 && !line.startsWith("*")) {
          tokenWords.add(JSONUtil.toObject(TokenWord.class, line));
        }
      }
    } catch (Exception ioe) {
      throw new DD4StorageException("Error reading token file: " + tokenFile, ioe);
    }
    return tokenWords.build();
  }

  public static void main(String[] args) {
    MachineTranslator machineTranslator = new MachineTranslator(TranslationTool::tokenWordProvider);
    DAOFileBasedImpl fileDao = new DAOFileBasedImpl("data/interlinear.db").loadFromFile();

    APIConnector apiConnector = new APIConnector(Constants.API_URL, Constants.API_VERSION, 50);
    StaticDataDAO staticDataDAO = new StaticDataDAO();
    BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
    LexiconFetcher lexiconFetcher = new LexiconFetcherBlueLetterImpl(apiConnector, true);
    InterlinearStore interlinearStore = new InterlinearStore(
        () -> fileDao, new ScriptureReferenceProcessorSplitImpl(bibleBookStore), lexiconFetcher);

    DAOFileBasedImpl lexiconDAO = new DAOFileBasedImpl("data/lexicon.db").loadFromFile();
    LexiconStore lexiconStore = new LexiconStore(() -> lexiconDAO, lexiconFetcher);

    TranslationTool translationTool = new TranslationTool(machineTranslator, interlinearStore);
    // translationTool.printTranslation("Gen 1:1-2:3");
    // translationTool.printTranslation("Isa 9:6, Psa 83:18, Gen 14:18,16:3,19:8,19:12");
    translationTool.printTranslation("Exo 20");
    // translationTool.printTranslation("Lev 23");

    System.out.println(
        "\nIndex, Hebrew, KJV Translation, StrongsId, Strongs Hebrew, StrongsIds, Breakdown, MT");
    translationTool.hasTranslationDiff.build() // .stream()
        // .sorted(Comparator.comparing(Interlinear::getId))
        .forEach(i -> System.out.printf("%s, %s, %s, %s, %s, %s, %s, %s\n",
            i.getId(), i.getConstantsOnly(), i.getTranslation(), i.getStrongsId(),
            i.getStrongsId() == null ? null : lexiconStore.get(i.getStrongsId()).getConstantsOnly(),
            i.getSubTokens().stream().map(SubToken::getStrongsId).filter(Objects::nonNull).collect(joining(" ")),
            i.getSubTokens().stream().map(SubToken::getWord).collect(joining(".")),
            i.getSubTokens().stream().map(SubToken::getTranslation).collect(joining(""))));

    /* printTranslation(
        machineTranslator.translate(
            new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(9).setVerse(6).setText(
                "כי ילד יולד לנו בן נתן לנו ותהי המשורה על שכמו וקרא שמו פלא יועץ אל גבור אבי עד שר השלום"))); */

    fileDao.saveToFile();
  }
}

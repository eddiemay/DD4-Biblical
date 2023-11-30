package com.digitald4.biblical.util;

import static com.digitald4.biblical.util.HebrewConverter.toConstantsOnly;
import static com.digitald4.biblical.util.HebrewConverter.toStrongsId;
import static com.digitald4.biblical.util.HebrewConverter.unfinalize;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.AncientLexicon;
import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Interlinear.SubToken;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.store.BibleBookStore;
import com.digitald4.biblical.store.InterlinearStore;
import com.digitald4.biblical.store.testing.StaticDataDAO;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.storage.DAOFileBasedImpl;
import com.digitald4.common.util.JSONUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Provider;

public class MachineTranslator {
  private final Provider<Iterable<TokenWord>> tokenWordsProvider;
  private ImmutableListMultimap<String, TokenWord> tokenWordsByWord;
  private SubwordTokenizer subwordTokenizer;

  @Inject
  public MachineTranslator(Provider<Iterable<TokenWord>> tokenWordsProvider) {
    this.tokenWordsProvider = tokenWordsProvider;
  }

  public synchronized void init() {
    if (subwordTokenizer != null) {
      return;
    }

    ImmutableListMultimap.Builder<String, TokenWord> builder = ImmutableListMultimap.builder();
    subwordTokenizer = new SubwordTokenizer(
        stream(tokenWordsProvider.get())
            .peek(tokenWord -> builder.put(tokenWord.getWord(), tokenWord))
            // .flatMap(tokenWord -> Stream.of(tokenWord.getWord(), "##" + tokenWord.getWord()))
            .collect(toImmutableSet()));
    tokenWordsByWord = builder.build();
  }

  public SubToken getTranslation(String word, String strongsId, boolean suffix) {
    if (word.startsWith("##")) {
      word = word.substring(2);
    }

    SubToken subToken = new SubToken().setWord(word);
    ImmutableList<TokenWord> options = tokenWordsByWord.get(word);
    if (options.isEmpty()) {
      return subToken.setTranslation(word);
    }

    TokenWord option = options.size() == 1 ? options.get(0) : options.stream()
        .filter(o -> Objects.equals(toStrongsId(strongsId), o.getStrongsId())).findFirst()
        .orElse(options.get(0));

    return subToken.setTranslation(suffix ? option.asSuffix() : option.getTranslation())
        .setStrongsId(option.getStrongsId());
  }

  public Interlinear translate(Interlinear interlinear) {
    if (subwordTokenizer == null) init();

    AtomicBoolean wordFound = new AtomicBoolean();
    interlinear.setSubtokens(
        subwordTokenizer.tokenizeWord(unfinalize(interlinear.getConstantsOnly())).stream()
            .map(subWord -> getTranslation(subWord, interlinear.getStrongsId(), wordFound.get()))
            .peek(subToken -> wordFound.set(wordFound.get() || subToken.getStrongsId() != null))
            .collect(toImmutableList()));
    return interlinear;
  }

  public ImmutableList<Interlinear> translate(Iterable<Interlinear> interlinears) {
    return stream(interlinears).map(this::translate).collect(toImmutableList());
  }

  public ImmutableList<Interlinear> translate(String text) {
    return translate(
        stream(text.split(" "))
            .map(word -> new Interlinear().setBook("").setWord(word).setConstantsOnly(toConstantsOnly(word)))
            .collect(toImmutableList()));
  }

  public ImmutableList<Interlinear> translate(Scripture scripture) {
    if (scripture instanceof InterlinearScripture) {
      return translate(((InterlinearScripture) scripture).getInterlinears());
    }

    return translate(
        stream(scripture.getText().toString().split(" "))
            .map(
                word ->
                    new Interlinear()
                        .setBook(scripture.getBook())
                        .setChapter(scripture.getChapter()).setVerse(scripture.getVerse())
                        .setWord(word).setConstantsOnly(toConstantsOnly(word)))
            .collect(toImmutableList()));
  }

  public static class TokenWord {
    private String word;
    private String translation;
    private String strongsId;
    private String asSuffix;
    enum TokenType {PREFIX, PREFIX_ONLY, SUFFIX, SUFFIX_ONLY, WORD, WHOLE_WORD_ONLY}
    private TokenType tokenType;

    public String getWord() {
      return word;
    }

    public TokenWord setWord(String word) {
      this.word = word;
      return this;
    }

    public String getTranslation() {
      return translation;
    }

    public TokenWord setTranslation(String translation) {
      this.translation = translation;
      return this;
    }

    public String getStrongsId() {
      return strongsId;
    }

    public TokenWord setStrongsId(String strongsId) {
      this.strongsId = strongsId;
      return this;
    }

    public String getAsSuffix() {
      return asSuffix;
    }

    public TokenWord setAsSuffix(String asSuffix) {
      this.asSuffix = asSuffix;
      return this;
    }

    public String asSuffix() {
      return asSuffix == null ? translation : asSuffix;
    }

    public TokenType getTokenType() {
      return tokenType;
    }

    public TokenWord setTokenType(TokenType tokenType) {
      this.tokenType = tokenType;
      return this;
    }

    public TokenType tokenType() {
      return tokenType == null ? TokenType.WORD : tokenType;
    }

    @Override
    public String toString() {
      return String.format(
          "%s,%s,%s%s", word, translation, strongsId, asSuffix == null ? "" : "," + asSuffix);
    }

    public static TokenWord from(Lexicon lexicon) {
      return new TokenWord()
          .setWord(unfinalize(lexicon.getConstantsOnly()))
          .setTranslation(lexicon.translation())
          .setStrongsId(lexicon.getStrongsId());
    }

    public static ImmutableList<TokenWord> from(AncientLexicon ancientLexicon) {
      if (ancientLexicon.getStrongIds() == null || ancientLexicon.getTranslation() == null) {
        return ImmutableList.of();
      }

      return ancientLexicon.getWords().stream()
          .flatMap(
              word -> ancientLexicon.getStrongIds().stream().map(
                  strongsId -> new TokenWord()
                      .setWord(unfinalize(word))
                      .setTranslation(ancientLexicon.translation())
                      .setStrongsId(strongsId.replaceAll("A", "H"))))
          .collect(toImmutableList());
    }
  }

  private static ImmutableList<TokenWord> tokenWordProvider() {
    ImmutableList.Builder<TokenWord> tokenWords = ImmutableList.builder();
    Stream.of("data/heb_vocab_prefix.txt", "data/heb_vocab_from_ancient_lexicon.txt").forEach(tokenFile -> {
      try (BufferedReader br = new BufferedReader(new FileReader(tokenFile))) {
        String line;
        while ((line = br.readLine()) != null) {
          if (!line.startsWith("*")) {
            tokenWords.add(JSONUtil.toObject(TokenWord.class, line));
          }
        }
      } catch (IOException ioe) {
        throw new DD4StorageException("Error reading token file: " + tokenFile, ioe);
      }
    });
    return tokenWords.build();
  }

  private final static String BASE_URL = "https://dd4-biblical.appspot.com/";
  private final static String API_URL = BASE_URL + "_api";
  private final static String API_VERSION = "v1";

  public static void main(String[] args) {
    MachineTranslator machineTranslator = new MachineTranslator(MachineTranslator::tokenWordProvider);
    DAOFileBasedImpl fileDao = new DAOFileBasedImpl("data/interlinear.db").loadFromFile();

    APIConnector apiConnector = new APIConnector(API_URL, API_VERSION, 50);
    StaticDataDAO staticDataDAO = new StaticDataDAO();
    BibleBookStore bibleBookStore = new BibleBookStore(() -> staticDataDAO);
    LexiconFetcher lexiconFetcher = new LexiconFetcherBlueLetterImpl(apiConnector, true);
    InterlinearStore interlinearStore = new InterlinearStore(
        () -> fileDao, new ScriptureReferenceProcessorSplitImpl(bibleBookStore), lexiconFetcher);

    /* printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:1")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:2")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:3")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:4")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:5")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:6"))); */
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:7")));
    /* printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:8")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:9")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:10")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:11")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:12")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:13")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:14")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:15")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:16")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:17")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:18")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:19")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:20")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:21")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:22")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:23")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:24")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:25")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:26")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:27")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:28")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:29")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:30")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 1:31")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 2:1")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 2:2")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Gen 2:3")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Isa 9:6")));
    printTranslation(
        machineTranslator.translate(
            new Scripture().setVersion("DSS").setBook("Isaiah").setChapter(9).setVerse(6).setText(
                "כי ילד יולד לנו בן נתן לנו ותהי המשורה על שכמו וקרא שמו פלא יועץ אל גבור אבי עד שר השלום")));
    printTranslation(machineTranslator.translate(interlinearStore.getInterlinear("Psa 83:18"))); */

    fileDao.saveToFile();
  }

  public static void printTranslation(ImmutableList<Interlinear> translated) {
    System.out.println("\n" + translated.get(0).reference());
    System.out.println(translated.stream().map(Interlinear::getStrongsId).collect(joining(" ")));
    System.out.println(translated.stream().map(Interlinear::getTranslation).collect(joining(" ")));
    System.out.println(translated.stream().map(Interlinear::getConstantsOnly).collect(joining(" ")));
    System.out.println(
        translated.stream()
            .flatMap(i -> i.getSubTokens().stream().map(SubToken::getWord))
            .collect(joining(" ")));
    System.out.println(
        translated.stream()
            .map(i -> i.getSubTokens().stream().map(SubToken::getTranslation).collect(joining("")))
            .collect(joining(" ")));
  }
}

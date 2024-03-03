package com.digitald4.biblical.store;

import static com.digitald4.biblical.model.ScriptureVersion.INTERLINEAR;
import static com.digitald4.biblical.util.HebrewConverter.removePunctuation;
import static com.digitald4.biblical.util.HebrewConverter.toConstantsOnly;
import static com.digitald4.common.storage.Query.forList;
import static com.digitald4.biblical.util.HebrewConverter.toAncient;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Interlinear;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.Scripture.AuditScripture;
import com.digitald4.biblical.model.Scripture.InterlinearScripture;
import com.digitald4.biblical.model.ScriptureVersion;
import com.digitald4.biblical.util.Language;
import com.digitald4.biblical.util.MachineTranslator;
import com.digitald4.biblical.util.ScriptureFetcher;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.LanguageRequest;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.VerseRange;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SearchIndexer;
import com.digitald4.common.storage.SearchableStoreImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Provider;

public class ScriptureStore extends SearchableStoreImpl<Scripture, String> {
  public static final String DEFAULT_ORDER_BY = "bookNum,chapter,verse,versionNum,version";
  private static final String SINGLE_CHAPTER = "%s %d";

  private final SearchIndexer searchIndexer;
  private final BibleBookStore bibleBookStore;
  private final ScriptureReferenceProcessor scriptureRefProcessor;
  private final ScriptureFetcher scriptureFetcher;
  private final InterlinearStore interlinearStore;
  private final MachineTranslator machineTranslator;

  @Inject
  public ScriptureStore(
      Provider<DAO> daoProvider, SearchIndexer searchIndexer, BibleBookStore bibleBookStore,
      ScriptureReferenceProcessor scriptureRefProcessor, ScriptureFetcher scriptureFetcher,
      InterlinearStore interlinearStore, MachineTranslator machineTranslator) {
    super(Scripture.class, daoProvider);
    this.searchIndexer = searchIndexer;
    this.bibleBookStore = bibleBookStore;
    this.scriptureRefProcessor = scriptureRefProcessor;
    this.scriptureFetcher = scriptureFetcher;
    this.interlinearStore = interlinearStore;
    this.machineTranslator = machineTranslator;
  }

  public GetOrSearchResponse getScriptures(String version, String language, String reference) {
    ImmutableList<VerseRange> verseRanges = scriptureRefProcessor.computeVerseRanges(reference);
    String prevChapter = null;
    String nextChapter = null;
    if (verseRanges.size() == 1 && verseRanges.get(0).getStartVerse() == 1 && verseRanges.get(0).getEndVerse() == 400) {
      VerseRange verseRange = verseRanges.get(0);
      // We have 1 full chapter, so we can set previous and next chapters.
      prevChapter = verseRange.getChapter() > 1
          ? String.format(SINGLE_CHAPTER, verseRange.getBook(), verseRange.getChapter() - 1) : null;
      nextChapter = verseRange.getChapter() < verseRange.getBook().getChapterCount()
          ? String.format(SINGLE_CHAPTER, verseRange.getBook(), verseRange.getChapter() + 1) : null;
    }

    return GetOrSearchResponse.getResult(
        verseRanges.stream()
            .flatMap(verseRange -> getScriptures(version, language, verseRange).stream())
            .collect(toImmutableSet()),
        prevChapter,
        nextChapter);
  }

  private ImmutableList<Scripture> getScriptures(
      String version, String language, VerseRange verseRange) {
    String book = verseRange.getBook().name();
    int chapter = verseRange.getChapter();
    int startVerse = verseRange.getStartVerse();
    int endVerse = verseRange.getEndVerse();

    if (INTERLINEAR.equalsIgnoreCase(version) || INTERLINEAR.equalsIgnoreCase(language)) {
      ImmutableMap<Integer, String> dssReferences = !book.equals(BibleBook.ISAIAH)
          ? ImmutableMap.of()
          : list(new LanguageRequest("DSS", Language.HEBREW, false), book, chapter, startVerse, endVerse)
              .getItems().stream()
              .collect(toImmutableMap(Scripture::getVerse, s -> s.getText().toString()));

      ImmutableList<Interlinear> interlinears = verseRange.getBook().getNumber() < 67
          ? interlinearStore.getInterlinear(verseRange)
          : list(new LanguageRequest("Sefaria", Language.HEBREW, false), book, chapter, startVerse, endVerse)
              .getItems().stream()
              .flatMap(s -> {
                AtomicInteger index = new AtomicInteger();
                return stream(s.getText().toString().split(" "))
                    .map(w -> new Interlinear().setBook(s.getBook()).setChapter(s.getChapter())
                        .setVerse(s.getVerse()).setIndex(index.incrementAndGet())
                        .setWord(w).setConstantsOnly(toConstantsOnly(w)));
              })
              .collect(toImmutableList());

      if (!interlinears.isEmpty()) {
        return interlinears.stream()
            .peek(machineTranslator::translate)
            .collect(groupingBy(Interlinear::getVerse)).values().stream()
            .map(InterlinearScripture::new)
            .peek(s -> InterlinearStore.fillDss(s, dssReferences.get(s.getVerse())))
            .sorted(comparing(Scripture::getVerse))
            .collect(toImmutableList());
      }
    }

    HashMap<String, String> verseMap = new HashMap<>();
    return expandSelection(version, language, verseRange.getBook())
        .stream()
        .flatMap(languageRequest -> {
          if ("Audit".equals(languageRequest.getVersion())) {
            return IntStream.range(startVerse, endVerse + 1)
                .mapToObj(
                    verse -> AuditScripture.of(book, chapter, verse,
                        verseMap.getOrDefault("DSS-" + verse, ""),
                        verseMap.getOrDefault(String.valueOf(verse), "")));
          } else if (Language.HEBREW.equals(languageRequest.getLanguage())) {
            // if we find Hebrew, index it in case we are going to be doing an Audit.
            return list(languageRequest, book, chapter, startVerse, endVerse).getItems().stream()
                .peek(s -> verseMap.put(
                    s.getVersion().equals("DSS") ? s.getVersion() + "-" + s.getVerse()
                        : String.valueOf(s.getVerse()), s.getText().toString()));
          } else if (Language.HEBREW_ANCIENT.equals(languageRequest.getLanguage())) {
            return list(new LanguageRequest(version, Language.HEBREW, false), book, chapter, startVerse, endVerse)
                .getItems().stream()
                .peek(s -> s.setLanguage(Language.HEBREW_ANCIENT).setText(toAncient(s.getText())));
          }
          return list(languageRequest, book, chapter, startVerse, endVerse).getItems().stream();
        })
        .sorted(comparing(Scripture::getVerse))
        .collect(toImmutableList());
  }

  private static ImmutableList<LanguageRequest> expandSelection(
      String version, String language, BibleBook book) {
    if (Language.INTERLACED.equals(language)) {
      return book.name().equals(BibleBook.ISAIAH)
          ? ImmutableList.of(
              new LanguageRequest(version, Language.EN, true),
              new LanguageRequest(version, Language.HEBREW, true),
              new LanguageRequest("DSS", Language.HEBREW, false),
              new LanguageRequest("Audit", Language.HEBREW, false),
              new LanguageRequest("DSS", Language.EN, false))
          : ImmutableList.of(
              new LanguageRequest(version, Language.EN, true),
              new LanguageRequest(version, Language.HEBREW, false),
              new LanguageRequest(version, Language.GREEK, false));
    }

    return ImmutableList.of(new LanguageRequest(version, language, true));
  }

  public String getScripturesTextAllVersions(String lang, String reference) {
    return scriptureRefProcessor.computeVerseRanges(reference).stream()
        .map(verseRange -> getScripturesTextAllVersions(lang, verseRange))
        .collect(joining("\n\n"));
  }

  private String getScripturesTextAllVersions(String lang, VerseRange verseRange) {
    return getScriptures(null, lang, verseRange).stream()
        .map(
            script ->
                String.format("(%s) %s %d:%d %s",
                    script.getVersion(), script.getBook(), script.getChapter(), script.getVerse(), script.getText()))
        .collect(joining("\n"));
  }

  public void reindex(String version, String lang, String book, int chapter) {
    searchIndexer.index(list(new LanguageRequest(version, lang, true), book, chapter, 1, 200).getItems());
  }

  public int migrate(String version, String language, String book, int chapter) {
    Query.List query = Query.forList()
        .setFilters(Filter.of("version", version), Filter.of("book", book), Filter.of("chapter", chapter));
    if (language != null) {
      query.addFilter(Filter.of("language", language));
    }

    return create(list(query).getItems()).size();
  }

  public int searchAndDelete(String searchText) {
    ImmutableList<Scripture> results =
        search(Query.forSearch(searchText, DEFAULT_ORDER_BY, 1000, 1)).getItems();
    delete(results.stream().map(Scripture::getId).collect(toImmutableList()));
    return results.size();
  }

  public ImmutableList<Scripture> searchAndReplace(String phrase, String replacement, String filter, boolean preview) {
    ImmutableList<Scripture> candidates = getSearchAndReplaceCandidates(phrase, replacement, filter);

    if (!preview) {
      update(
          candidates.stream().map(Scripture::getId).collect(toImmutableList()),
          scripture ->
              scripture.setText(scripture.getText().toString().replace(phrase, replacement)));
    }

    return candidates;
  }

  private static final Pattern CHAPTER_PATTERN = Pattern.compile("CHAPTER (\\d+)");
  private static final Pattern VERSE_PATTERN = Pattern.compile("(\\d+)\\s+(\\D+)");
  public ImmutableList<Scripture> uploadScripture(
      String version, String language, String book,  int chapter, String text, boolean preview) {
    ScriptureVersion sv = ScriptureVersion.get(version);
    if (sv == null) {
      throw new DD4StorageException("Unknown scriptrue version: " + version, ErrorCode.BAD_REQUEST);
    }

    BibleBook bibleBook = bibleBookStore.get(book);

    if (!bibleBookStore.meetsCriteria(sv.getVersion(), bibleBook, language)) {
      throw new DD4StorageException(
          String.format("Scripture version: %s does not support %s:%s", sv, bibleBook, language));
    }

    Matcher chapterMatcher = CHAPTER_PATTERN.matcher(text);
    if (chapterMatcher.find()) {
      chapter = Integer.parseInt(chapterMatcher.group(1));
    }

    if (chapter > bibleBook.getChapterCount()) {
      throw new DD4StorageException("Chapter " + chapter + " out of bounds for book: " + bibleBook);
    }

    text = text.replaceAll("\u00a0", " ");

    ImmutableList.Builder<Scripture> scriptures = ImmutableList.builder();
    Matcher matcher = VERSE_PATTERN.matcher(text);
    while (matcher.find()) {
      String scriptureText = matcher.group(2).trim();
      if (!scriptureText.isEmpty()) {
        scriptures.add(
            new Scripture()
                .setVersion(version)
                .setLanguage(language)
                .setBook(bibleBook.name())
                .setChapter(chapter)
                .setVerse(Integer.parseInt(matcher.group(1)))
                .setText(scriptureText));
      }
    }

    if (preview) {
      return scriptures.build();
    }

    return create(scriptures.build());
  }

  private ImmutableList<Scripture> getSearchAndReplaceCandidates(String phrase, String replacement, String filter) {
    return search(
        Query.forSearch(String.format("\"%s\" %s", phrase, filter), DEFAULT_ORDER_BY, 1000, 1)).getItems()
        .stream()
        // The search is case-insensitive, but we want to be case-sensitive so need to filter the results.
        .filter(scripture -> scripture.getText().toString().contains(phrase))
        .map(scripture -> scripture.setText(scripture.getText().toString().replace(phrase, replacement)))
        .collect(toImmutableList());
  }

  private QueryResult<Scripture> list(
      LanguageRequest languageRequest, String book, int chapter, int startVerse, int endVerse) {
    String version = languageRequest.getVersion();
    String language = languageRequest.getLanguage();

    BibleBook bibleBook = bibleBookStore.get(book, chapter);
    if (bibleBook.name().equals(BibleBook.APOCRYPHAL_PSALMS) && chapter > 150) {
      chapter -= 150;
    }

    if (version == null) {
      return list(languageRequest.getLanguage(), bibleBook, chapter, startVerse, endVerse);
    }

    ScriptureVersion scriptureVersion =
        bibleBookStore.getOrFallback(version, language, bibleBook, languageRequest.isRequired());
    if (scriptureVersion == null) {
      return QueryResult.of(ImmutableList.of(), 0, null);
    }

    version = scriptureVersion.getVersion();

    Query.List query = forList()
        .setFilters(
            Filter.of("version", version),
            Filter.of("book", bibleBook.name()), Filter.of("chapter", chapter),
            Filter.of("verse", ">=", startVerse), Filter.of("verse", "<=", endVerse));
    if (language != null) {
      query.addFilter(Filter.of("language", language));
    }
    query.setOrderBys(OrderBy.of("verse"));

    QueryResult<Scripture> queryResult = list(query);
    if (queryResult.getItems().isEmpty()) {
      Query.List query2 = forList().setFilters(
          Filter.of("book", bibleBook.name()),
          Filter.of("chapter", chapter), Filter.of("verse", ">=",  1),
          Filter.of("version", version));
      if (language != null) {
        query2.addFilter(Filter.of("language", language));
      }
      if (startVerse > 1 && !list(query2).getItems().isEmpty()) {
        throw new DD4StorageException(
            String.format(
                "Verse %d out of bounds for: (%s) %s %d", startVerse, version, book, chapter));
      }
      ImmutableList<Scripture> fetched =
          fetchFromWeb(version, language, bibleBook, chapter, startVerse, endVerse);
      queryResult = QueryResult.of(fetched, fetched.size(), query);
    }

    return queryResult;
  }

  private QueryResult<Scripture> list(
      String language, BibleBook bibleBook, int chapter, int startVerse, int endVerse) {
    Query.List query = forList()
        .setFilters(
            Filter.of("book", bibleBook.name()), Filter.of("chapter", chapter),
            Filter.of("verse", ">=", startVerse), Filter.of("verse", "<=", endVerse));
    if (language != null) {
      query.addFilter(Filter.of("language", language));
    }
    query.setOrderBys(OrderBy.of("verse"));

    QueryResult<Scripture> queryResult = list(query);

    return QueryResult.of(
        queryResult.getItems().stream()
            .sorted(
                comparing(Scripture::getVerse)
                    .thenComparing(s -> ScriptureVersion.get(s.getVersion()).getVersionNum()))
            .collect(toImmutableList()),
        queryResult.getTotalSize(),
        queryResult.query());
  }

  private ImmutableList<Scripture> fetchFromWeb(
      String version, String language, BibleBook book, int chapter, int startVerse, int endVerse) {
    return create(scriptureFetcher.fetch(version, language, book, chapter)).stream()
        .filter(scripture -> scripture.getChapter() == chapter)
        .filter(scripture -> scripture.getVerse() >= startVerse && scripture.getVerse() <= endVerse)
        .sorted(comparing(Scripture::getVerse))
        .collect(toImmutableList());
  }

  public static class GetOrSearchResponse extends QueryResult<Scripture> {
    private enum RESULT_TYPE {GET, SEARCH}
    private final RESULT_TYPE resultType;
    private final String prevChapter;
    private final String nextChapter;

    private GetOrSearchResponse(
        RESULT_TYPE resultType, Iterable<Scripture> scriptures, int totalSize, Query query,
        String prevChapter, String nextChapter) {
      super(scriptures, totalSize, query);
      this.resultType = resultType;
      this.prevChapter = prevChapter;
      this.nextChapter = nextChapter;
    }

    public RESULT_TYPE getResultType() {
      return resultType;
    }

    public String getPrevChapter() {
      return prevChapter;
    }

    public String getNextChapter() {
      return nextChapter;
    }

    public static GetOrSearchResponse getResult(
        Iterable<Scripture> scriptures, String prevChapter, String nextChapter) {
      return new GetOrSearchResponse(
          RESULT_TYPE.GET, scriptures, Iterables.size(scriptures), null, prevChapter, nextChapter);
    }

    public static GetOrSearchResponse searchResult(QueryResult<Scripture> queryResult) {
      return new GetOrSearchResponse(
          RESULT_TYPE.SEARCH, ImmutableSet.copyOf(queryResult.getItems()),
          queryResult.getTotalSize(), queryResult.query(), null, null);
    }
  }
}

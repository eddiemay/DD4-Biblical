package com.digitald4.biblical.store;

import static com.digitald4.common.storage.Query.forList;
import static com.digitald4.biblical.util.HebrewConverter.toAncient;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.ScriptureVersion;
import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.biblical.util.ScriptureFetcher;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
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
import com.digitald4.common.util.Calculate;
import com.digitald4.common.util.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Provider;

public class ScriptureStore extends SearchableStoreImpl<Scripture, String> {
  public static final String DEFAULT_ORDER_BY = "bookNum,chapter,verse,versionNum,version";
  private static final String SINGLE_CHAPTER = "%s %d";

  private final SearchIndexer searchIndexer;
  private final ScriptureReferenceProcessor scriptureRefProcessor;
  private final ScriptureFetcher scriptureFetcher;

  @Inject
  public ScriptureStore(
      Provider<DAO> daoProvider, SearchIndexer searchIndexer,
      ScriptureReferenceProcessor scriptureRefProcessor, ScriptureFetcher scriptureFetcher) {
    super(Scripture.class, daoProvider);
    this.searchIndexer = searchIndexer;
    this.scriptureRefProcessor = scriptureRefProcessor;
    this.scriptureFetcher = scriptureFetcher;
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

    HashMap<String, String> verseMap = new HashMap<>();
    return expandSelection(version, language, verseRange.getBook())
        .stream()
        .flatMap(pair -> {
          String ver = pair.getLeft();
          String lang = pair.getRight();
          if ("Audit".equals(ver)) {
            return IntStream.range(startVerse, endVerse + 1)
                .mapToObj(
                    verse -> new Scripture()
                        .setVersion("Audit")
                        .setBook(book)
                        .setLanguage("he")
                        .setChapter(chapter)
                        .setVerse(verse)
                        .setText(
                            getDiffHtml(
                                verseMap.getOrDefault("DSS-" + verse, ""),
                                verseMap.getOrDefault(String.valueOf(verse), ""))))
                .filter(scripture -> scripture.getText().length() > 0);
          } else if (BibleBook.HEBREW.equals(lang)) {
            return list(ver, lang, book, chapter, startVerse, endVerse).getItems().stream()
                .peek(
                    s ->
                        verseMap.put(
                            s.getVersion().equals("DSS")
                                ? s.getVersion() + "-" + s.getVerse() : String.valueOf(s.getVerse()),
                    s.getText().toString()));
          }
          return list(ver, lang, book, chapter, startVerse, endVerse).getItems().stream();
        })
        .sorted(comparing(Scripture::getVerse))
        .collect(toImmutableList());
  }

  private static String getDiffHtml(String original, String revised) {
    if (original.isEmpty() && revised.isEmpty()) {
      return "";
    }

    return String.format("%s accuracy: %d%%",
        Calculate.getDiffHtml(original, revised),
        original.isEmpty() ? 0 : ((original.length() - Calculate.LD(original, revised)) * 100 / original.length()));
  }

  private static ImmutableList<Pair<String, String>> expandSelection(
      String version, String language, BibleBook book) {
    if (BibleBook.INTERLACED.equals(language)) {
      return book != BibleBook.ISAIAH
          ? ImmutableList.of(Pair.of(version, BibleBook.HEBREW), Pair.of(version, BibleBook.EN))
          : ImmutableList.of(
              Pair.of(version, BibleBook.EN),
              Pair.of(version, BibleBook.HEBREW),
              Pair.of("DSS", BibleBook.HEBREW),
              Pair.of("Audit", BibleBook.HEBREW),
              Pair.of("DSS", BibleBook.EN));
    }

    return ImmutableList.of(Pair.of(version, language));
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
    searchIndexer.index(list(version, lang, book, chapter, 1, 200).getItems());
  }

  public int migrate(String version, String language, String book, int chapter) {
    Query.List query = Query.forList().setFilters(
        Filter.of("version", version), Filter.of("book", book), Filter.of("chapter", chapter));
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

    BibleBook bibleBook = BibleBook.get(book);

    if (!sv.meetsCriteria(bibleBook, language)) {
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
      String version, String language, String book, int chapter, int startVerse, int endVerse) {

    BibleBook bibleBook = BibleBook.get(book, chapter);
    if (bibleBook == BibleBook.Psalms && chapter > 150) {
      bibleBook = BibleBook.APOCRYPHAL_PSALMS;
    }
    if (bibleBook == BibleBook.APOCRYPHAL_PSALMS && chapter > 150) {
      chapter -= 150;
    }

    if (version == null) {
      return list(language, bibleBook, chapter, startVerse, endVerse);
    }

    String lang = BibleBook.HEBREW_ANCIENT.equals(language) ? BibleBook.HEBREW : language;
    version = ScriptureVersion.getOrFallback(version, lang, bibleBook).getVersion();

    Query.List query = forList()
        .setFilters(
            Filter.of("version", version),
            Filter.of("book", bibleBook.name()), Filter.of("chapter", chapter),
            Filter.of("verse", ">=", startVerse), Filter.of("verse", "<=", endVerse));
    if (lang != null) {
      query.addFilter(Filter.of("language", lang));
    }
    query.setOrderBys(OrderBy.of("verse"));

    QueryResult<Scripture> queryResult = list(query);
    if (queryResult.getItems().isEmpty()) {
      Query.List query2 = forList().setFilters(
          Filter.of("book", bibleBook.name()),
          Filter.of("chapter", chapter), Filter.of("verse", ">=",  1),
          Filter.of("version", version));
      if (lang != null) {
        query2.addFilter(Filter.of("language", lang));
      }
      if (startVerse > 1 && !list(query2).getItems().isEmpty()) {
        throw new DD4StorageException(
            String.format(
                "Verse %d out of bounds for: (%s) %s %d", startVerse, version, book, chapter));
      }
      ImmutableList<Scripture> fetched =
          fetchFromWeb(version, lang, bibleBook, chapter, startVerse, endVerse);
      queryResult = QueryResult.of(fetched, fetched.size(), query);
    }

    if (BibleBook.HEBREW_ANCIENT.equals(language)) {
      queryResult.getItems().forEach(s -> s.setLanguage(language).setText(toAncient(s.getText())));
    }
    return queryResult;
  }

  private QueryResult<Scripture> list(
      String lang, BibleBook bibleBook, int chapter, int startVerse, int endVerse) {
    Query.List query = forList()
        .setFilters(
            Filter.of("book", bibleBook.name()), Filter.of("chapter", chapter),
            Filter.of("verse", ">=", startVerse), Filter.of("verse", "<=", endVerse));
    if (lang != null) {
      query.addFilter(
          Filter.of("language", BibleBook.HEBREW_ANCIENT.equals(lang) ? BibleBook.HEBREW : lang));
    }
    query.setOrderBys(OrderBy.of("verse"));

    QueryResult<Scripture> queryResult = list(query);

    return QueryResult.of(
        queryResult.getItems().stream()
            .peek(s -> {
              if (BibleBook.HEBREW_ANCIENT.equals(lang)) {
                s.setLanguage(lang).setText(HebrewConverter.toAncient(s.getText()));
              }
            })
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

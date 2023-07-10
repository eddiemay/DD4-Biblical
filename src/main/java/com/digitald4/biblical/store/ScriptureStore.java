package com.digitald4.biblical.store;

import static com.digitald4.common.storage.Query.forList;
import static com.digitald4.biblical.util.HebrewConverter.toAncient;
import static com.google.common.collect.ImmutableList.toImmutableList;
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
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SearchIndexer;
import com.digitald4.common.storage.SearchableStoreImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
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

  public GetOrSearchResponse getScriptures(String version, String locale, String reference) {
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
            .flatMap(verseRange -> getScriptures(version, locale, verseRange).stream())
            .collect(toImmutableSet()),
        prevChapter,
        nextChapter);
  }

  private ImmutableList<Scripture> getScriptures(
      String version, String locale, VerseRange verseRange) {
    String book = verseRange.getBook().getName();
    int chapter = verseRange.getChapter();
    int startVerse = verseRange.getStartVerse();
    int endVerse = verseRange.getEndVerse();

    return expandLanguages(locale).stream()
        .flatMap(lan -> list(version, lan, book, chapter, startVerse, endVerse).getItems().stream())
        .sorted(comparing(Scripture::getVerse))
        .collect(toImmutableList());
  }

  private List<String> expandLanguages(String locale) {
    if (locale == null) {
      ArrayList<String> list = new ArrayList<>();
      list.add(null);
      return list;
    }

    return BibleBook.INTERLACED.equals(locale)
        ? ImmutableList.of(BibleBook.HEBREW, BibleBook.EN) : ImmutableList.of(locale);
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

  public int migrate(String version, String lang, String book, int chapter) {
    return create(list(version, lang, book, chapter, 1, 200).getItems()).size();
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
      String version, String lang, String book, int chapter, int startVerse, int endVerse) {
    BibleBook bibleBook = BibleBook.get(book, chapter);
    if (bibleBook == BibleBook.PSALMS_151 && chapter == 151) {
      chapter = 1;
    }

    if (version == null) {
      return list(lang, bibleBook, chapter, startVerse, endVerse);
    }

    String locale = BibleBook.HEBREW_ANCIENT.equals(lang) ? BibleBook.HEBREW : lang;
    version = ScriptureVersion.getOrFallback(version, locale, bibleBook).getVersion();

    Query.List query = forList()
        .setFilters(
            Filter.of("version", version),
            Filter.of("book", bibleBook.getName()), Filter.of("chapter", chapter),
            Filter.of("verse", ">=", startVerse), Filter.of("verse", "<=", endVerse));
    if (locale != null) {
      query.addFilter(Filter.of("locale", locale));
    }
    query.setOrderBys(OrderBy.of("verse"));

    QueryResult<Scripture> queryResult = list(query);
    if (queryResult.getItems().isEmpty()) {
      Query.List query2 = forList().setFilters(
          Filter.of("book", bibleBook.getName()),
          Filter.of("chapter", chapter), Filter.of("verse", ">=",  1),
          Filter.of("version", version));
      if (locale != null) {
        query2.addFilter(Filter.of("locale", locale));
      }
      if (startVerse > 1 && !list(query2).getItems().isEmpty()) {
        throw new DD4StorageException(
            String.format(
                "Verse %d out of bounds for: (%s) %s %d", startVerse, version, book, chapter));
      }
      ImmutableList<Scripture> fetched =
          fetchFromWeb(version, locale, bibleBook, chapter, startVerse, endVerse);
      queryResult = QueryResult.of(fetched, fetched.size(), query);
    }

    if (BibleBook.HEBREW_ANCIENT.equals(lang)) {
      queryResult.getItems().forEach(s -> s.setLocale(lang).setText(toAncient(s.getText())));
    }
    return queryResult;
  }

  private QueryResult<Scripture> list(
      String lang, BibleBook bibleBook, int chapter, int startVerse, int endVerse) {
    Query.List query = forList()
        .setFilters(
            Filter.of("book", bibleBook.getName()), Filter.of("chapter", chapter),
            Filter.of("verse", ">=", startVerse), Filter.of("verse", "<=", endVerse));
    if (lang != null) {
      query.addFilter(Filter.of("locale", BibleBook.HEBREW_ANCIENT.equals(lang) ? BibleBook.HEBREW : lang));
    }
    query.setOrderBys(OrderBy.of("verse"));

    QueryResult<Scripture> queryResult = list(query);

    return QueryResult.of(
        queryResult.getItems().stream()
            .peek(s -> {
              if (BibleBook.HEBREW_ANCIENT.equals(lang)) {
                s.setLocale(lang).setText(HebrewConverter.toAncient(s.getText()));
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
      String version, String locale, BibleBook book, int chapter, int startVerse, int endVerse) {
    return create(scriptureFetcher.fetch(version, locale, book, chapter)).stream()
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

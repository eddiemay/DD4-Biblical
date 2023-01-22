package com.digitald4.biblical.store;

import static com.digitald4.common.storage.Query.forList;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.ScriptureVersion;
import com.digitald4.biblical.store.Annotations.ScriptureIndex;
import com.digitald4.biblical.util.ScriptureFetcher;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.VerseRange;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SearchableStoreImpl;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import javax.inject.Inject;
import javax.inject.Provider;

public class ScriptureStore extends SearchableStoreImpl<Scripture, String> {
  public static final String DEFAULT_ORDER_BY = "bookNum,chapter,verse,versionNum,version";
  private static final String SINGLE_CHAPTER = "%s %d";

  private final ScriptureReferenceProcessor scriptureRefProcessor;
  private final ScriptureFetcher scriptureFetcher;

  @Inject
  public ScriptureStore(
      Provider<DAO> daoProvider, @ScriptureIndex Index searchIndex,
      ScriptureReferenceProcessor scriptureRefProcessor, ScriptureFetcher scriptureFetcher) {
    super(Scripture.class, daoProvider, searchIndex);
    this.scriptureRefProcessor = scriptureRefProcessor;
    this.scriptureFetcher = scriptureFetcher;
  }

  public GetOrSearchResponse getScriptures(String version, String reference) {
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
            .flatMap(verseRange -> getScriptures(version, verseRange).stream())
            .collect(toImmutableList()),
        prevChapter,
        nextChapter);
  }

  private ImmutableList<Scripture> getScriptures(String version, VerseRange verseRange) {
    String book = verseRange.getBook().getName();
    int chapter = verseRange.getChapter();
    return list(version, book, chapter, verseRange.getStartVerse(), verseRange.getEndVerse()).getItems();
  }

  public String getScripturesTextAllVersions(String reference) {
    return scriptureRefProcessor.computeVerseRanges(reference).stream()
        .map(this::getScripturesTextAllVersions)
        .collect(joining("\n\n"));
  }

  private String getScripturesTextAllVersions(VerseRange verseRange) {
    return getScriptures(null, verseRange).stream()
        .map(
            script ->
                String.format("(%s) %s %d:%d %s",
                    script.getVersion(), script.getBook(), script.getChapter(), script.getVerse(), script.getText()))
        .collect(joining("\n"));
  }

  @Override
  public com.google.appengine.api.search.Document toDocument(Scripture scripture) {
    ScriptureVersion scriptureVersion = ScriptureVersion.get(scripture.getVersion());
    BibleBook bibleBook = BibleBook.get(scripture.getBook());

    return com.google.appengine.api.search.Document.newBuilder()
        .setId(scripture.getId())
        .addField(Field.newBuilder().setName("book").setAtom(scripture.getBook()))
        .addField(Field.newBuilder().setName("bookNum").setNumber(bibleBook.getBookNum()))
        .addField(Field.newBuilder().setName("chapter").setNumber(scripture.getChapter()))
        .addField(Field.newBuilder().setName("verse").setNumber(scripture.getVerse()))
        .addField(Field.newBuilder().setName("version").setAtom(scripture.getVersion()))
        .addField(Field.newBuilder().setName("versionNum").setNumber(scriptureVersion.getVersionNum()))
        .addField(Field.newBuilder().setName("text").setHTML(scripture.getText().toString()))
        .addField(Field.newBuilder().setName("bookTags").setText(bibleBook.getTags()))
        .addField(Field.newBuilder().setName("bookAltNames").setText(String.join(",", bibleBook.getAltNames())))
        .build();
  }

  @Override
  public Scripture fromDocument(com.google.appengine.api.search.Document document) {
    return new Scripture()
        .setVersion(document.getOnlyField("version").getAtom())
        .setBook(document.getOnlyField("book").getAtom())
        .setChapter(document.getOnlyField("chapter").getNumber().intValue())
        .setVerse(document.getOnlyField("verse").getNumber().intValue())
        .setText(document.getOnlyField("text").getHTML());
  }

  public void reindex(String version, String book, int chapter) {
    QueryResult<Scripture> queryResult = list(version, book, chapter, 1, 200);
    reindex(queryResult.getItems());
  }

  public int searchAndDelete(String searchText) {
    ImmutableList<Scripture> results = search(Query.forSearch(searchText, DEFAULT_ORDER_BY, 1000, 1)).getItems();
    delete(results.stream().map(Scripture::getId).collect(toImmutableList()));
    return results.size();
  }

  public ImmutableList<Scripture> searchAndReplace(String phrase, String replacement, String filter, boolean preview) {
    ImmutableList<Scripture> candidates = getSearchAndReplaceCandidates(phrase, replacement, filter);

    if (!preview) {
      update(
          candidates.stream().map(Scripture::getId).collect(toImmutableList()),
          scripture -> scripture.setText(scripture.getText().toString().replace(phrase, replacement)));
    }

    return candidates;
  }

  private ImmutableList<Scripture> getSearchAndReplaceCandidates(String phrase, String replacement, String filter) {
    return search(
        Query.forSearch(String.format("\"%s\" %s", phrase, filter), DEFAULT_ORDER_BY, 1000, 1)).getItems()
        .stream()
        // The search is case-insensitive, but we want to be case sensitive so need to filter the results.
        .filter(scripture -> scripture.getText().toString().contains(phrase))
        .map(scripture -> scripture.setText(scripture.getText().toString().replace(phrase, replacement)))
        .collect(toImmutableList());
  }

  private QueryResult<Scripture> list(String version, String book, int chapter, int startVerse, int endVerse) {
    BibleBook bibleBook = BibleBook.get(book, chapter);
    if (bibleBook == BibleBook.PSALMS_151 && chapter == 151) {
      chapter = 1;
    }

    if (version == null) {
      return list(bibleBook, chapter, startVerse, endVerse);
    }

    version = ScriptureVersion.getOrFallback(version, bibleBook).getVersion();

    Query.List query = forList()
        .setFilters(
            Filter.of("version", version),
            Filter.of("book", bibleBook.getName()), Filter.of("chapter", chapter),
            Filter.of("verse", ">=", startVerse), Filter.of("verse", "<=", endVerse));
    query.setOrderBys(OrderBy.of("verse"));

    QueryResult<Scripture> queryResult = list(query);
    if (queryResult.getItems().isEmpty()) {
      if (startVerse > 1 &&
          !list(
              forList().setFilters(
                  Filter.of("book", bibleBook.getName()), Filter.of("chapter", chapter),
                  Filter.of("verse", ">=",  1), Filter.of("version", version))).getItems().isEmpty()) {
        throw new DD4StorageException(
            String.format("Verse %d out of bounds for: (%s) %s %d", startVerse, version, book, chapter));
      }
      ImmutableList<Scripture> fetched = fetchFromWeb(version, bibleBook, chapter, startVerse, endVerse);
      queryResult = QueryResult.of(fetched, fetched.size(), query);
    }

    return queryResult;
  }

  private QueryResult<Scripture> list(BibleBook bibleBook, int chapter, int startVerse, int endVerse) {
    Query.List query = forList()
        .setFilters(
            Filter.of("book", bibleBook.getName()), Filter.of("chapter", chapter),
            Filter.of("verse", ">=", startVerse), Filter.of("verse", "<=", endVerse));
    query.setOrderBys(OrderBy.of("verse"));

    QueryResult<Scripture> queryResult = list(query);

    return QueryResult.of(
        queryResult.getItems().stream()
            .sorted(
                comparing(Scripture::getVerse).thenComparing(s -> ScriptureVersion.get(s.getVersion()).getVersionNum()))
            .collect(toImmutableList()),
        queryResult.getTotalSize(),
        queryResult.query());
  }

  private ImmutableList<Scripture> fetchFromWeb(String version, BibleBook book, int chapter, int startVerse, int endVerse) {
    return create(scriptureFetcher.fetch(version, book, chapter)).stream()
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
        RESULT_TYPE resultType, Iterable<Scripture> scriptures, int totalSize, Query query, String prevChapter,
        String nextChapter) {
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
          RESULT_TYPE.SEARCH, queryResult.getItems(), queryResult.getTotalSize(), queryResult.query(), null, null);
    }
  }
}

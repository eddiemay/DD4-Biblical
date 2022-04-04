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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import javax.inject.Inject;
import javax.inject.Provider;

public class ScriptureStore extends SearchableStoreImpl<Scripture> {
  public static final String REFERENCE_ATTR = "scripture-reference";
  public static final String DEFAULT_ORDER_BY = "bookNum,chapter,verse,versionNum,version";

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

  public ImmutableList<Scripture> getScriptures(String version, String reference) {
    return scriptureRefProcessor.computeVerseRanges(reference).stream()
        .flatMap(verseRange -> getScriptures(version, verseRange).stream())
        .collect(toImmutableList());
  }

  public String getScripturesHtml(String version, String reference, boolean includeLinks, boolean spaceVerses) {
    return scriptureRefProcessor.computeVerseRanges(reference).stream()
        .map(verseRange -> getScripturesHtml(version, verseRange, includeLinks, spaceVerses))
        .collect(joining("</p><p>", "<p>", "</p>"));
  }

  public String getScripturesText(String version, String reference) {
    return scriptureRefProcessor.computeVerseRanges(reference).stream()
        .map(verseRange -> getScripturesText(version, verseRange))
        .collect(joining("\n\n"));
  }

  public String getScripturesTextAllVersions(String reference) {
    return scriptureRefProcessor.computeVerseRanges(reference).stream()
        .map(this::getScripturesTextAllVersions)
        .collect(joining("\n\n"));
  }

  public String expandScriptures(String version, String html, boolean includeLinks) {
    Document doc = Jsoup.parse(html.trim(), "", Parser.xmlParser());
    doc.outputSettings().prettyPrint(false);
    doc.getElementsByAttribute(REFERENCE_ATTR).forEach(ref -> {
      ref.html(getScripturesHtml(version, ref.attr(REFERENCE_ATTR), includeLinks, false));
      if (includeLinks) {
        ref.removeAttr(REFERENCE_ATTR);
      }
    });

    return doc.toString();
  }

  @Override
  public com.google.appengine.api.search.Document toDocument(Scripture scripture) {
    ScriptureVersion scriptureVersion = ScriptureVersion.get(scripture.getVersion());
    BibleBook bibleBook = BibleBook.get(scripture.getBook());

    return com.google.appengine.api.search.Document.newBuilder()
        .setId(scripture.documentId())
        .addField(Field.newBuilder().setName("scriptureId").setAtom(String.valueOf(scripture.getId())))
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
        .setId(
            document.getFieldNames().contains("scriptureId")
                ? Long.parseLong(document.getOnlyField("scriptureId").getAtom()) : 0)
        .setVersion(document.getOnlyField("version").getAtom())
        .setBook(document.getOnlyField("book").getAtom())
        .setChapter(document.getOnlyField("chapter").getNumber().intValue())
        .setVerse(document.getOnlyField("verse").getNumber().intValue())
        .setText(document.getOnlyField("text").getHTML());
  }

  public void reindex(String version, String book, int chapter) {
    list(version, book, chapter, 1, 200, true);
  }

  public void searchAndDelete(String searchText) {
    delete(search(Query.forSearch(searchText)).getItems().stream().map(Scripture::getId).collect(toImmutableList()));
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

  public ImmutableList<Scripture> searchAndReplace(String phrase, String replacement, String filter, boolean preview) {
    ImmutableList<Scripture> candidates = getSearchAndReplaceCandidates(phrase, replacement, filter);

    if (!preview) {
      update(
          candidates.stream().map(Scripture::getId).collect(toImmutableList()),
          scripture -> scripture.setText(scripture.getText().toString().replace(phrase, replacement)));
    }

    return candidates;
  }

  public QueryResult<Scripture> list(
      String version, String book, int chapter, int startVerse, int endVerse, boolean reindex) {
    BibleBook bibleBook = BibleBook.get(book, chapter);
    if (bibleBook == BibleBook.PSALMS_151 && chapter == 151) {
      chapter = 1;
    }

    if (version == null) {
      return list(bibleBook, chapter, startVerse, endVerse, reindex);
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
    } else if (reindex) {
      reindex(queryResult.getItems());
    }

    return queryResult;
  }

  private QueryResult<Scripture> list(BibleBook bibleBook, int chapter, int startVerse, int endVerse, boolean reindex) {
    Query.List query = forList()
        .setFilters(
            Filter.of("book", bibleBook.getName()), Filter.of("chapter", chapter),
            Filter.of("verse", ">=", startVerse), Filter.of("verse", "<=", endVerse));
    query.setOrderBys(OrderBy.of("verse"));

    QueryResult<Scripture> queryResult = list(query);
    if (!queryResult.getItems().isEmpty() && reindex) {
      reindex(queryResult.getItems());
    }

    return QueryResult.of(
        queryResult.getItems().stream()
            .sorted(
                comparing(Scripture::getVerse).thenComparing(s -> ScriptureVersion.get(s.getVersion()).getVersionNum()))
            .collect(toImmutableList()),
        queryResult.getTotalSize(),
        queryResult.query());
  }

  private ImmutableList<Scripture> getScriptures(String version, VerseRange verseRange) {
    String book = verseRange.getBook().getName();
    int chapter = verseRange.getChapter();
    return list(version, book, chapter, verseRange.getStartVerse(), verseRange.getEndVerse(), false).getItems();
  }

  private String getScripturesHtml(String version, VerseRange verseRange, boolean includeLinks, boolean spaceVerses) {
    String bookChapter = String.format("%s %d", verseRange.getBook().getName(), verseRange.getChapter());
    ImmutableList<Scripture> scriptures = getScriptures(version, verseRange);
    if (scriptures.isEmpty()) {
      return "";
    }
    // We must reassign the version for it may have been overwritten.
    version = scriptures.get(0).getVersion();

    return scriptures.stream()
        .map(script -> (includeLinks ? getVerseLink(script) : script.getVerse()) + " " + script.getText())
        .collect(joining(spaceVerses ? "</p><p>" : " ", (includeLinks ? getChapterLink(version, verseRange) : bookChapter) + ":", ""));
  }

  private String getScripturesText(String version, VerseRange verseRange) {
    return getScriptures(version, verseRange).stream()
        .map(script -> script.getVerse() + " " + script.getText())
        .collect(joining(" ",  String.format("%s %d:", verseRange.getBook().getName(), verseRange.getChapter()), ""));
  }

  private String getScripturesTextAllVersions(VerseRange verseRange) {
    return getScriptures(null, verseRange).stream()
        .map(
            script ->
                String.format("(%s) %s %d:%d %s",
                    script.getVersion(), script.getBook(), script.getChapter(), script.getVerse(), script.getText()))
        .collect(joining("\n"));
  }

  private ImmutableList<Scripture> fetchFromWeb(String version, BibleBook book, int chapter, int startVerse, int endVerse) {
    return create(scriptureFetcher.fetch(version, book, chapter)).stream()
        .filter(scripture -> scripture.getVerse() >= startVerse && scripture.getVerse() <= endVerse)
        .sorted(comparing(Scripture::getVerse))
        .collect(toImmutableList());
  }

  @VisibleForTesting String getChapterLink(String version, VerseRange verseRange) {
    return String.format("<a href=\"%s\" target=\"scripture\">%s %d</a>",
        scriptureFetcher.getChapterUrl(version, verseRange), verseRange.getBook(), verseRange.getChapter());
  }

  @VisibleForTesting String getVerseLink(Scripture scripture) {
    return String.format("<a href=\"%s\" target=\"scripture\">%d</a>",
        scriptureFetcher.getVerseUrl(scripture), scripture.getVerse());
  }
}

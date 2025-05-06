package com.digitald4.biblical.store;

import static com.digitald4.biblical.util.HebrewConverter.toConstantsOnly;
import static com.digitald4.biblical.util.HebrewConverter.toRestored;
import static com.digitald4.biblical.util.HebrewConverter.unfinalize;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Commandment;
import com.digitald4.biblical.model.Lexicon;
import com.digitald4.biblical.model.Scripture;
import com.digitald4.biblical.model.ScriptureVersion;
import com.digitald4.biblical.util.Language;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.VerseRange;
import com.digitald4.common.storage.DAOCloudDS.Context;
import com.digitald4.common.storage.SearchIndexerAppEngineImpl;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javax.inject.Inject;
import javax.inject.Provider;

public class SearchIndexImpl extends SearchIndexerAppEngineImpl {
  private final ScriptureReferenceProcessor scriptureRefProcessor;
  private final Provider<ScriptureStore> scriptureStore;
  private final BibleBookStore bibleBookStore;

  @Inject
  public SearchIndexImpl(Provider<Context> contextProvider, ScriptureReferenceProcessor scriptureRefProcessor,
      Provider<ScriptureStore> scriptureStore, BibleBookStore bibleBookStore) {
    super(contextProvider);
    this.scriptureRefProcessor = scriptureRefProcessor;
    this.scriptureStore = scriptureStore;
    this.bibleBookStore = bibleBookStore;
  }

  @Override
  protected <T> Document.Builder toDocumentBuilder(T t) {
    if (t instanceof Commandment) {
      return toDocumentBuilder((Commandment) t);
    } else if (t instanceof Scripture) {
      return toDocumentBuilder((Scripture) t);
    } else if (t instanceof Lexicon) {
      return toDocumentBuilder((Lexicon) t);
    }

    return super.toDocumentBuilder(t);
  }

  public Document.Builder toDocumentBuilder(Scripture scripture) {
    ScriptureVersion scriptureVersion = ScriptureVersion.get(scripture.getVersion());
    BibleBook bibleBook = bibleBookStore.get(scripture.getBook());
    String text = scripture.getText().toString();

    Document.Builder builder = Document.newBuilder()
        .setId(scripture.getId())
        .addField(Field.newBuilder().setName("book").setAtom(scripture.getBook()))
        .addField(Field.newBuilder().setName("bookNum").setNumber(bibleBook.getNumber()))
        .addField(Field.newBuilder().setName("chapter").setNumber(scripture.getChapter()))
        .addField(Field.newBuilder().setName("verse").setNumber(scripture.getVerse()))
        .addField(Field.newBuilder().setName("version").setAtom(scripture.getVersion()))
        .addField(Field.newBuilder().setName("language").setAtom(scripture.getLanguage()))
        .addField(Field.newBuilder().setName("versionNum").setNumber(scriptureVersion.getVersionNum()))
        .addField(Field.newBuilder().setName("text").setHTML(scripture.getText().toString()))
        .addField(Field.newBuilder().setName("bookTags").setText(bibleBook.getTags()))
        .addField(Field.newBuilder().setName("bookAltNames").setText(String.join(",", bibleBook.getAltNames())));

    if (Language.HEBREW.equals(scripture.getLanguage())) {
      String constantsOnly = toConstantsOnly(text);
      builder
          .addField(Field.newBuilder().setName("unfinalized").setHTML(unfinalize(constantsOnly)))
          .addField(Field.newBuilder().setName("constantsOnly").setHTML(constantsOnly))
          .addField(Field.newBuilder().setName("restored").setHTML(toRestored(text)));
    }

    return builder;
  }

  public Document.Builder toDocumentBuilder(Lexicon lexicon) {
    String constantsOnly = toConstantsOnly(lexicon.getWord());
    return super.toDocumentBuilder(lexicon)
        .addField(Field.newBuilder().setName("unfinalized").setAtom(unfinalize(constantsOnly)))
        .addField(Field.newBuilder().setName("constantsOnly").setAtom(constantsOnly))
        .addField(Field.newBuilder().setName("restored").setAtom(lexicon.restored()));
  }

  public Document.Builder toDocumentBuilder(Commandment commandment) {
    ImmutableList<VerseRange> verseRanges = scriptureRefProcessor.computeVerseRanges(commandment.getScriptures());
    VerseRange firstDeclared = verseRanges.get(0);
    ImmutableSet<BibleBook> bibleBooks = verseRanges.stream().map(VerseRange::getBook).collect(toImmutableSet());

    return super.toDocumentBuilder(commandment)
        .addField(Field.newBuilder().setName("bookNum").setNumber(firstDeclared.getBook().getNumber()))
        .addField(Field.newBuilder().setName("book").setAtom(firstDeclared.getBook().name()))
        .addField(Field.newBuilder().setName("chapter").setNumber(firstDeclared.getChapter()))
        .addField(Field.newBuilder().setName("verse").setNumber(firstDeclared.getStartVerse()))
        .addField(Field.newBuilder().setName("scriptureText").setText(
            scriptureStore.get().getScripturesTextAllVersions(Language.EN, commandment.getScriptures())))
        .addField(Field.newBuilder().setName("bookTags").setText(
            bibleBooks.stream()
                .flatMap(bibleBook -> stream(bibleBook.getTags().split(","))).distinct().collect(joining(","))))
        .addField(Field.newBuilder().setName("bookAltNames").setText(
            bibleBooks.stream().flatMap(bibleBook -> bibleBook.getAltNames().stream()).collect(joining(","))));
  }

  @Override
  protected Index computeIndex(Class<?> c) {
    return c == Scripture.class
        ? SearchServiceFactory.getSearchService().getIndex(IndexSpec.newBuilder().setName("scripture-index").build())
        : super.computeIndex(c);
  }
}

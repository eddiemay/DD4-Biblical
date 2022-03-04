package com.digitald4.biblical.store;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.BibleBook;
import com.digitald4.biblical.model.Commandment;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.biblical.util.ScriptureReferenceProcessor.VerseRange;
import com.digitald4.common.storage.*;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.Index;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Arrays;

public class CommandmentStore extends SearchableStoreImpl<Commandment> {
  private final ScriptureReferenceProcessor scriptureRefProcessor;
  private final ScriptureStore scriptureStore;

  @Inject
  public CommandmentStore(
      Provider<DAO> daoProvider, @Annotations.CommandmentsIndex Index searchIndex,
      ScriptureReferenceProcessor scriptureRefProcessor, ScriptureStore scriptureStore) {
    super(Commandment.class, daoProvider, searchIndex);
    this.scriptureRefProcessor = scriptureRefProcessor;
    this.scriptureStore = scriptureStore;
  }

  public void reindex() {
    update(list(Query.forList()).getItems().stream().map(Commandment::getId).collect(toImmutableList()), cur -> cur);
    // reindex(list(Query.forList()).getItems());
  }

  @Override
  protected Iterable<Commandment> preprocess(Iterable<Commandment> commandments) {
    return Streams.stream(commandments)
        // Try and parse the scriptures to make sure it is valid.
        .peek(commandment -> scriptureRefProcessor.computeVerseRanges(commandment.getScriptures()))
        .map(this::fixTags)
        .collect(toImmutableList());
  }

  private Commandment fixTags(Commandment commandment) {
    return commandment
        .setTags(Arrays.stream(commandment.getTags().split(",")).map(String::trim).collect(joining(" ")));
  }

  public Document toDocument(Commandment commandment) {
    ImmutableList<VerseRange> verseRanges = scriptureRefProcessor.computeVerseRanges(commandment.getScriptures());
    VerseRange firstDeclared = verseRanges.get(0);
    ImmutableSet<BibleBook> bibleBooks = verseRanges.stream().map(VerseRange::getBook).collect(toImmutableSet());

    return Document.newBuilder()
        .setId(String.valueOf(commandment.getId()))
        .addField(Field.newBuilder().setName("summary").setText(commandment.getSummary()))
        .addField(Field.newBuilder().setName("scriptures").setText(commandment.getScriptures()))
        .addField(Field.newBuilder().setName("tags").setText(commandment.getTags()))
        .addField(Field.newBuilder().setName("bookNum").setNumber(firstDeclared.getBook().getBookNum()))
        .addField(Field.newBuilder().setName("book").setAtom(firstDeclared.getBook().getName()))
        .addField(Field.newBuilder().setName("chapter").setNumber(firstDeclared.getChapter()))
        .addField(Field.newBuilder().setName("verse").setNumber(firstDeclared.getStartVerse()))
        .addField(Field.newBuilder().setName("scriptureText").setText(
            scriptureStore.getScripturesTextAllVersions(commandment.getScriptures())))
        .addField(Field.newBuilder().setName("bookTags").setText(
            bibleBooks.stream()
                .flatMap(bibleBook -> Arrays.stream(bibleBook.getTags().split(",")))
                .distinct()
                .collect(joining(","))))
        .addField(Field.newBuilder().setName("bookAltNames").setText(
            bibleBooks.stream().flatMap(bibleBook -> bibleBook.getAltNames().stream()).collect(joining(","))))
        .build();
  }

  public Commandment fromDocument(Document document) {
    return new Commandment()
        .setId(Long.parseLong(document.getId()))
        .setSummary(document.getOnlyField("summary").getText())
        .setScriptures(document.getOnlyField("scriptures").getText())
        .setTags(document.getOnlyField("tags").getText());
  }
}

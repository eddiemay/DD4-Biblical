package com.digitald4.biblical.store;

import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.digitald4.biblical.model.Commandment;
import com.digitald4.biblical.util.ScriptureReferenceProcessor;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericLongStore;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.SearchIndexer;
import com.digitald4.common.storage.Transaction.Op;

import javax.inject.Inject;
import javax.inject.Provider;

public class CommandmentStore extends GenericLongStore<Commandment> {
  private final SearchIndexer searchIndexer;
  private final ScriptureReferenceProcessor scriptureRefProcessor;

  @Inject
  public CommandmentStore(Provider<DAO> daoProvider, SearchIndexer searchIndexer,
      ScriptureReferenceProcessor scriptureRefProcessor) {
    super(Commandment.class, daoProvider);
    this.searchIndexer = searchIndexer;
    this.scriptureRefProcessor = scriptureRefProcessor;
  }

  public void reindex() {
    searchIndexer.index(list(Query.forList()).getItems());
  }

  @Override
  protected Op<Commandment> preprocess(Op<Commandment> op) {
    // Try and parse the scriptures to make sure it is valid.
    scriptureRefProcessor.computeVerseRanges(op.getEntity().getScriptures());
    fixTags(op.getEntity());
    return op;
  }

  private void fixTags(Commandment commandment) {
    commandment.setTags(stream(commandment.getTags().split(",")).map(String::trim).collect(joining(" ")));
  }
}

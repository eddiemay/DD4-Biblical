package com.digitald4.biblical.store;

import static com.digitald4.biblical.store.ScriptureStore.REFERENCE_ATTR;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.biblical.model.BiblicalEvent;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.Query;
import com.google.common.collect.ImmutableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Comparator;
import java.util.function.UnaryOperator;

public class BiblicalEventStore extends GenericStore<BiblicalEvent, Long> {
  @Inject
  public BiblicalEventStore(Provider<DAO> daoProvider) {
    super(BiblicalEvent.class, daoProvider);
  }

  public ImmutableList<BiblicalEvent> getBiblicalEvents(int month) {
    return list(Query.forList().setFilters(Query.Filter.of("month", month))).getItems().stream()
        // Temp sorting until we get all the years filled in.
        .sorted(Comparator.comparing(BiblicalEvent::getDay).thenComparing(BiblicalEvent::getYear))
        .collect(toImmutableList());
  }

  @Override
  public BiblicalEvent create(BiblicalEvent biblicalEvent) {
    return super.create(clearScriptureReferences(biblicalEvent));
  }

  @Override
  public ImmutableList<BiblicalEvent> create(Iterable<BiblicalEvent> entities) {
    return super.create(stream(entities).map(BiblicalEventStore::clearScriptureReferences).collect(toImmutableList()));
  }

  @Override
  public BiblicalEvent update(Long id, UnaryOperator<BiblicalEvent> updater) {
    return super.update(id, current -> clearScriptureReferences(updater.apply(current)));
  }

  @Override
  public ImmutableList<BiblicalEvent> update(Iterable<Long> ids, UnaryOperator<BiblicalEvent> updater) {
    return super.update(ids, current -> clearScriptureReferences(updater.apply(current)));
  }

  private static BiblicalEvent clearScriptureReferences(BiblicalEvent biblicalEvent) {
    if (biblicalEvent.getSummary() == null) {
      return biblicalEvent;
    }

    Document doc = Jsoup.parse(biblicalEvent.getSummary().trim(), "", Parser.xmlParser());
    doc.outputSettings().prettyPrint(false);
    doc.getElementsByAttribute(REFERENCE_ATTR)
        .forEach(ref -> ref.html(String.format("<p>%s</p>", ref.attr(REFERENCE_ATTR))));

    return biblicalEvent.setSummary(doc.toString());
  }
}

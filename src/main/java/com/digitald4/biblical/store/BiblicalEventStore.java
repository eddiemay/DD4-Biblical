package com.digitald4.biblical.store;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.biblical.model.BiblicalEvent;
import com.digitald4.biblical.model.BiblicalEvent.Dependency.Relationship;
import com.digitald4.biblical.util.ScriptureMarkupProcessor;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.Query;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

public class BiblicalEventStore extends GenericStore<BiblicalEvent, Long> {
  private final ScriptureMarkupProcessor scriptureMarkupProcessor;
  @Inject
  public BiblicalEventStore(Provider<DAO> daoProvider, ScriptureMarkupProcessor scriptureMarkupProcessor) {
    super(BiblicalEvent.class, daoProvider);
    this.scriptureMarkupProcessor = scriptureMarkupProcessor;
  }

  public ImmutableList<BiblicalEvent> getAll() {
    return list(Query.forList()).getItems().stream()
        .sorted(
            Comparator.comparing(BiblicalEvent::getYear)
                .thenComparing(BiblicalEvent::getMonth)
                .thenComparing(BiblicalEvent::getDay)
                .thenComparing(BiblicalEvent::getEndYear, Comparator.reverseOrder()))
        .collect(toImmutableList());
  }

  public ImmutableList<BiblicalEvent> getBiblicalEvents(int month) {
    return list(Query.forList().setFilters(Query.Filter.of("month", month))).getItems().stream()
        .filter(event -> event.getYear() == event.getEndYear())
        // Temp sorting until we get all the years filled in.
        .sorted(Comparator.comparing(BiblicalEvent::getDay).thenComparing(BiblicalEvent::getYear))
        .collect(toImmutableList());
  }

  public ImmutableList<BiblicalEvent> getBiblicalEvents(int startYear, int endYear) {
    // Optimized query for reading events. Because only allows a single greater/less than operation, select the
    // operation that will result in the lesser amount of initial results then do secondary filter.
    return (startYear < 3000 ?
        list(Query.forList().setFilters(Query.Filter.of("year", "<=", endYear))).getItems().stream()
            .filter(event -> event.getEndYear() >= startYear) :
        list(Query.forList().setFilters(Query.Filter.of("end_year", ">=", startYear))).getItems().stream()
            .filter(event -> event.getYear() <= endYear))
        .sorted(
            Comparator.comparing(BiblicalEvent::getYear)
                .thenComparing(BiblicalEvent::getMonth)
                .thenComparing(BiblicalEvent::getDay)
                .thenComparing(BiblicalEvent::getEndYear, Comparator.reverseOrder()))
        .collect(toImmutableList());
  }

  @Override
  public BiblicalEvent create(BiblicalEvent biblicalEvent) {
    return super.create(preprocess(biblicalEvent));
  }

  @Override
  public ImmutableList<BiblicalEvent> create(Iterable<BiblicalEvent> entities) {
    return super.create(stream(entities).map(this::preprocess).collect(toImmutableList()));
  }

  @Override
  public BiblicalEvent update(Long id, UnaryOperator<BiblicalEvent> updater) {
    AtomicBoolean timesChanged = new AtomicBoolean();
    BiblicalEvent result = super.update(
        id,
        current -> {
          BiblicalEvent updated = preprocess(updater.apply(current));
          if (Objects.equals(id, updated.getDepEventId())) {
            throw new DD4StorageException("Dependent Loop detected", DD4StorageException.ErrorCode.BAD_REQUEST);
          }
          timesChanged.set(
              updated.getYear() != current.getYear() || !Objects.equals(updated.getEndYear(), current.getEndYear()));
          return updated;
        });

    if (timesChanged.get()) {
      updateDependents(result);
    }

    return result;
  }

  public void updateDependents(BiblicalEvent parentEvent) {
    list(Query.forList().setFilters(Query.Filter.of("depEventId", parentEvent.getId()))).getItems()
        .forEach(event -> {
          int startYear = event.getYear();
          int endYear = event.getEndYear();
          BiblicalEvent updated = updateYears(event, parentEvent);
          if (startYear != updated.getYear() || endYear != updated.getEndYear()) {
            super.update(event.getId(), current -> updated);
            updateDependents(updated);
          }
        });
  }

  @Override
  public ImmutableList<BiblicalEvent> update(Iterable<Long> ids, UnaryOperator<BiblicalEvent> updater) {
    return stream(ids).map(id -> update(id, updater)).collect(toImmutableList());
  }

  public ImmutableList<BiblicalEvent> migrate() {
    return update(
        list(Query.forList()).getItems().stream().map(BiblicalEvent::getId).collect(toImmutableList()),
        UnaryOperator.identity());
  }

  public BiblicalEvent preprocess(BiblicalEvent event) {
    return updateYears(
        event.setSummary(scriptureMarkupProcessor.replaceScriptures(event.getSummary())),
        event.getDepEventId() != null ? get(event.getDepEventId()) : null);
  }

  private static BiblicalEvent updateYears(BiblicalEvent event, BiblicalEvent parentEvent) {
    int offsetYears = (event.getOffset() == null) ? 0 : event.getOffset().years();
    int duration = (event.getDuration() == null) ? 0 : event.getDuration().years();
    int startYear = offsetYears;
    if (parentEvent != null) {
      Relationship relationship = event.getDepRelationship();
      switch (relationship) {
        case START_TO_START:
          startYear = parentEvent.getYear() + offsetYears;
          break;
        case FINISH_TO_START:
          startYear = parentEvent.getEndYear() + offsetYears;
          break;
        case START_TO_FINISH:
          startYear = parentEvent.getYear() + offsetYears - duration;
          break;
        case FINISH_TO_FINISH:
          startYear = parentEvent.getEndYear() + offsetYears - duration;
          break;
      }
    }

    return event.setYear(startYear).setEndYear(startYear + duration);
  }
}

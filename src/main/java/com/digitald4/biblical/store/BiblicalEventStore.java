package com.digitald4.biblical.store;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

import com.digitald4.biblical.model.BiblicalEvent;
import com.digitald4.biblical.model.BiblicalEvent.Dependency.Relationship;
import com.digitald4.biblical.util.ScriptureMarkupProcessor;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Transaction.Op;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.function.UnaryOperator;

public class BiblicalEventStore extends GenericStore<BiblicalEvent, Long> {
  private final ScriptureMarkupProcessor scriptureMarkupProcessor;
  private final FamilyTreeNodeStore familyTreeNodeStore;
  @Inject
  public BiblicalEventStore(Provider<DAO> daoProvider,
      ScriptureMarkupProcessor scriptureMarkupProcessor, FamilyTreeNodeStore familyTreeNodeStore) {
    super(BiblicalEvent.class, daoProvider);
    this.scriptureMarkupProcessor = scriptureMarkupProcessor;
    this.familyTreeNodeStore = familyTreeNodeStore;
  }

  public ImmutableList<BiblicalEvent> getAll() {
    return list(Query.forList()).getItems().stream()
        .sorted(
            comparing(BiblicalEvent::getYear)
                .thenComparing(BiblicalEvent::getMonth)
                .thenComparing(BiblicalEvent::getDay)
                .thenComparing(BiblicalEvent::getEndYear, reverseOrder()))
        .collect(toImmutableList());
  }

  public ImmutableList<BiblicalEvent> getBiblicalEvents(int month) {
    return list(Query.forList().setFilters(Query.Filter.of("month", month))).getItems().stream()
        // For the calendar we don't show events that last for years, such as the life of person.
        .filter(event -> event.getYear() == event.getEndYear())
        // Temp sorting until we get all the years filled in.
        .sorted(comparing(BiblicalEvent::getDay).thenComparing(BiblicalEvent::getYear))
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
            comparing(BiblicalEvent::getYear)
                .thenComparing(BiblicalEvent::getMonth)
                .thenComparing(BiblicalEvent::getDay)
                .thenComparing(BiblicalEvent::getEndYear))
        .collect(toImmutableList());
  }

  public ImmutableList<BiblicalEvent> migrate() {
    return update(
        list(Query.forList()).getItems().stream().map(BiblicalEvent::getId).collect(toImmutableList()),
        UnaryOperator.identity());
  }

  @Override
  protected Op<BiblicalEvent> preprocess(Op<BiblicalEvent> op) {
    preprocess(op.getEntity());
    return op;
  }

  @VisibleForTesting void preprocess(BiblicalEvent event) {
    updateYears(
        event.setSummary(scriptureMarkupProcessor.replaceScriptures(event.getSummary())),
        event.getDepEventId() != null ? get(event.getDepEventId()) : null);
  }

  @Override
  protected Op<BiblicalEvent> postprocess(Op<BiblicalEvent> op) {
    var parentEvent = op.getEntity();
    var start = op.getCurrent();
    if (start != null && (parentEvent.getYear() != start.getYear() || parentEvent.getEndYear() != start.getEndYear())) {
      list(Query.forList(Filter.of("depEventId", parentEvent.getId()))).getItems()
          .forEach(event -> {
            int startYear = event.getYear();
            int endYear = event.getEndYear();
            BiblicalEvent updated = updateYears(event, parentEvent);
            if (startYear != updated.getYear() || endYear != updated.getEndYear()) {
              update(event.getId(), current -> updated);
            }
          });
      familyTreeNodeStore.migrate(familyTreeNodeStore.list(
          Query.forList(Filter.of("eventId", parentEvent.getId()))).getItems());
    }
    return op;
  }

  @VisibleForTesting
  static BiblicalEvent updateYears(BiblicalEvent event, BiblicalEvent parentEvent) {
    int offsetYears = (event.getOffset() == null) ? 0 : event.getOffset().years();
    int duration = (event.getDuration() == null) ? 0 : event.getDuration().years();
    int startYear = offsetYears;
    if (parentEvent != null) {
      Relationship relationship = event.getDepRelationship();
      startYear = switch (relationship) {
        case START_TO_START -> parentEvent.getYear() + offsetYears;
        case FINISH_TO_START -> parentEvent.getEndYear() + offsetYears;
        case START_TO_FINISH -> parentEvent.getYear() + offsetYears - duration;
        case FINISH_TO_FINISH -> parentEvent.getEndYear() + offsetYears - duration;
      };
    }

    return event.setYear(startYear).setEndYear(startYear + duration);
  }
}

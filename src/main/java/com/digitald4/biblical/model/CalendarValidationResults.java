package com.digitald4.biblical.model;

import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.common.collect.Streams.stream;
import static java.util.function.Function.identity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;

public class CalendarValidationResults {
  private final ImmutableList<CalendarValidationResult> results;
  private final ImmutableListMultimap<CalendarValidationResult.Type, CalendarValidationResult> resultMap;

  public CalendarValidationResults(Iterable<CalendarValidationResult> validationResults) {
    results = ImmutableList.copyOf(validationResults);
    resultMap = stream(validationResults)
        .collect(toImmutableListMultimap(CalendarValidationResult::getType, identity()));
  }

  public ImmutableList<CalendarValidationResult> getResults() {
    return results;
  }

  public ImmutableList<CalendarValidationResult> getMatching() {
    return resultMap.get(CalendarValidationResult.Type.MATCHING);
  }

  public ImmutableList<CalendarValidationResult> getErrors() {
    return resultMap.get(CalendarValidationResult.Type.ERROR);
  }

  public ImmutableList<CalendarValidationResult> getWarnings() {
    return resultMap.get(CalendarValidationResult.Type.WARNING);
  }
}

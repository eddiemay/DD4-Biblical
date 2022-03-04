package com.digitald4.biblical.tools;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;

import com.digitald4.biblical.model.*;
import com.digitald4.biblical.util.SunTimeUtil;
import com.digitald4.biblical.util.SunTimeUtilSunriseSunsetOrg;
import com.digitald4.common.server.APIConnector;
import org.joda.time.DateTime;

import javax.inject.Inject;

public class CalendarValidator {
  private static final String DATE_FORMAT = "%sT00:00:00.00Z";

  private final SunTimeUtil suntimeUtil;

  @Inject
  public CalendarValidator(SunTimeUtil suntimeUtil) {
    this.suntimeUtil = suntimeUtil;
  }

  public CalendarValidationResults validate(BiblicalCalendar calendar, Iterable<CalendarRule> calRules) {
    return new CalendarValidationResults(
        stream(calRules)
            .map(calendarRule -> validate(calendar, calendarRule))
            .collect(toImmutableList()));
  }

  public CalendarValidationResult validate(BiblicalCalendar calendar, CalendarRule calendarRule) {
    if (calendarRule instanceof DayLengthCalendarRule) {
      return validate(calendar, (DayLengthCalendarRule) calendarRule);
    }

    throw new UnsupportedOperationException("Unimplemented CalRule type: " + calendarRule.getClass());
  }

  public CalendarValidationResult validate(BiblicalCalendar calendar, DayLengthCalendarRule calRule) {
    SunTimeData timeData = suntimeUtil.getSunTimeData(calendar.getDate(calRule.getMonth(), calRule.getDay()));
    return (timeData.getDayParts() == calRule.getExpectedDayParts())
        ? CalendarValidationResult.createMatching(
            String.format(
                "Month %d, day %d has day parts of %d as expected (%s)",
                calRule.getMonth(), calRule.getDay(), calRule.getExpectedDayParts(), calRule.getScripture()))
        : CalendarValidationResult.createError(
            String.format(
                "Expected month %d, day %d to have day parts of %d (%s) however it has day parts of %2f (%s)",
                calRule.getMonth(), calRule.getDay(), calRule.getExpectedDayParts(), calRule.getScripture(),
                timeData.getSunDayParts(), timeData.getDate().toString().substring(0, 10)));
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.printf("Usage: java %s calendar_start_date [calendar_type]%n", CalendarValidator.class);
      return;
    }

    BiblicalCalendar calendar = args.length == 1 || "enoch".equals(args[1]) ?
        new EnochCalendar(DateTime.parse(String.format(DATE_FORMAT, args[0]))) :
        new LunarCalendar(DateTime.parse(String.format(DATE_FORMAT, args[0])));

    int callInterval = 100;
    CalendarValidationResults results = new CalendarValidator(
            new SunTimeUtilSunriseSunsetOrg(new APIConnector(null, null, callInterval)))
        .validate(calendar, DayLengthCalendarRule.ALL);

    results.getResults().forEach(result -> System.out.printf("%n%s%n", result));
    System.out.printf("%n%d Matching, %d Errors%n", results.getMatching().size(), results.getErrors().size());
  }
}

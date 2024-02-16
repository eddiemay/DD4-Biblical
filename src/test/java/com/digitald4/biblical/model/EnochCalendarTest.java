package com.digitald4.biblical.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableListMultimap;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;

public class EnochCalendarTest {

  @Test @Ignore
  public void getDate_monthDay() {
    DateTime start = DateTime.parse("2018-12-30T19:34:50.63Z");

    EnochCalendar enochCalendar = new EnochCalendar(start);

    assertEquals(start, enochCalendar.getDate(1, 1));
    assertEquals(start.plusDays(-1), enochCalendar.getDate(1, -1));
    assertEquals(start.plusDays(30), enochCalendar.getDate(2, 1));
    assertEquals(start.plusDays(91), enochCalendar.getDate(4, 1));
    assertEquals(start.plusDays(182), enochCalendar.getDate(7, 1));
    assertEquals(start.plusDays(182 + 9), enochCalendar.getDate(7, 10));
    assertEquals(start.plusDays(363), enochCalendar.getDate(12, 31));
  }

  @Test @Ignore
  public void getDate_monthDay2021() {
    DateTime start = DateTime.parse("2021-03-21T06:52:00.00Z");

    EnochCalendar enochCalendar = new EnochCalendar(start);

    assertEquals(DateTime.parse("2021-03-20T06:52:00.00Z"), enochCalendar.getDate(1, -1));
    assertEquals(DateTime.parse("2021-03-21T06:52:00.00Z"), enochCalendar.getDate(1, 1));
    assertEquals(DateTime.parse("2021-04-20T06:52:00.00Z"), enochCalendar.getDate(2, 1));
    assertEquals(DateTime.parse("2021-05-30T06:52:00.00Z"), enochCalendar.getDate(3, 11));
    assertEquals(DateTime.parse("2021-09-19T06:52:00.00Z"), enochCalendar.getDate(7, 1));
    assertEquals(DateTime.parse("2021-09-28T06:52:00.00Z"), enochCalendar.getDate(7, 10));
    assertEquals(DateTime.parse("2022-03-19T06:52:00.00Z"), enochCalendar.getDate(12, 31));
  }

  @Test @Ignore
  public void getAbibOne() {
    int year = 1600;
    DateTime rollingDate = new DateTime(1600, 3, 19, 4, 0);
    DateTime[] earlist = new DateTime[6];
    DateTime[] latest = new DateTime[6];
    ImmutableListMultimap.Builder<String, DateTime> builder = ImmutableListMultimap.builder();
    for (; year < 3201; year++) {
      DateTime abibOne40YearSkip = EnochCalendar.getAbibOne40YearSkip(year);
      DateTime abibOne40_50YearSkip = EnochCalendar.getAbibOne40_50YearSkip(year);
      DateTime abibOne45YearSkip = EnochCalendar.getAbibOne45YearSkip(year);
      DateTime abibOne50Then40YearSkip = EnochCalendar.getAbibOne50Then40YearSkip(year);
      DateTime abibOne50YearSkip = EnochCalendar.getAbibOne50YearSkip(year);
      DateTime abibOneKingdomPreppers = EnochCalendar.getAbibOneKingdomPreppers(year);
      checkEarlistLatestAndViolations(earlist, latest, 0, builder, "40 Year Skip Violations: ", abibOne40YearSkip);
      checkEarlistLatestAndViolations(earlist, latest, 1, builder, "40/50 Year Skip Violations: ", abibOne40_50YearSkip);
      checkEarlistLatestAndViolations(earlist, latest, 2, builder, "45 Year Skip Violations: ", abibOne45YearSkip);
      checkEarlistLatestAndViolations(earlist, latest, 3, builder, "50 then 40 Year Skip Violations: ", abibOne50Then40YearSkip);
      checkEarlistLatestAndViolations(earlist, latest, 4, builder, "50 Year Skip Violations: ", abibOne50YearSkip);
      checkEarlistLatestAndViolations(earlist, latest, 5, builder, "KingdomPreppers Violations: ", abibOneKingdomPreppers);
      /*System.out.printf("%s, %s, %s, %s, %s, %s%n",
          getDateOutput(abibOne40YearSkip),
          getDateOutput(abibOne40_50YearSkip),
          getDateOutput(abibOne45YearSkip),
          getDateOutput(abibOne50Then40YearSkip),
          getDateOutput(abibOne50YearSkip),
          getDateOutput(abibOneKingdomPreppers));*/
      DateTime abibOne = EnochCalendar.getAbibOne(year);
      assertEquals(rollingDate, abibOne);
      rollingDate = rollingDate.plusWeeks(
          year % 5 != 0 || (year % 50 == 0 && year % 400 <= 200) || (year % 40 == 0 && year % 400 > 200) ? 52 : 53);
      assertTrue("Expected: " + rollingDate + " > 11, < 28",
          rollingDate.getDayOfMonth() > 11 && rollingDate.getDayOfMonth() < 28);
    }
    System.out.printf("Earliest %s, %s, %s, %s, %s, %s%n", getDateOutput(earlist[0]), getDateOutput(earlist[1]),
        getDateOutput(earlist[2]), getDateOutput(earlist[3]), getDateOutput(earlist[4]), getDateOutput(earlist[5]));
    System.out.printf("Latest %s, %s, %s, %s, %s, %s%n", getDateOutput(latest[0]), getDateOutput(latest[1]),
        getDateOutput(latest[2]), getDateOutput(latest[3]), getDateOutput(latest[4]), getDateOutput(latest[5]));

    ImmutableListMultimap<String, DateTime> violations = builder.build();
    violations.keySet().forEach(key -> {
      System.out.println("\n" + key + violations.get(key).size());
      violations.get(key).stream().limit(50).forEach(dateTime -> System.out.println(getDateOutput(dateTime)));
    });
  }

  private static String getDateOutput(DateTime dateTime) {
    return dateTime.toString().substring(0, 10);
  }

  private static void checkEarlistLatestAndViolations(
      DateTime[] earlist,
      DateTime[] latest,
      int index,
      ImmutableListMultimap.Builder<String, DateTime> violations,
      String violationBucket,
      DateTime abibOne) {
    if (earlist[index] == null || earlist[index].getDayOfMonth() > abibOne.getDayOfMonth()) {
      earlist[index] = abibOne;
    }
    if (latest[index] == null || latest[index].getDayOfMonth() < abibOne.getDayOfMonth()) {
      latest[index] = abibOne;
    }
    if (abibOne.getMonthOfYear() < 3 ||
        abibOne.getMonthOfYear() == 3 && (abibOne.getDayOfMonth() < 13 || abibOne.getDayOfMonth() > 27)) {
      violations.put(violationBucket, abibOne);
    }
  }

  @Test
  public void getAbibOne_canGoBackInTime() {
    assertEquals(new DateTime(1969, 3, 16, 4, 0), EnochCalendar.getAbibOne(1969));
    assertEquals(new DateTime(1949, 3, 20, 4, 0), EnochCalendar.getAbibOne(1949));
    assertEquals(new DateTime(1948, 3, 21, 4, 0), EnochCalendar.getAbibOne(1948));
    assertEquals(new DateTime(1947, 3, 23, 4, 0), EnochCalendar.getAbibOne(1947));
    assertEquals(new DateTime(1946, 3, 24, 4, 0), EnochCalendar.getAbibOne(1946));
    assertEquals(new DateTime(1945, 3, 18, 4, 0), EnochCalendar.getAbibOne(1945));
    // assertEquals(new DateTime(1910, 3, 16, 4, 0), EnochCalendar.getAbibOne(1910));
    assertEquals(new DateTime(1900, 3, 18, 4, 0), EnochCalendar.getAbibOne(1900));
    // assertEquals(new DateTime(1776, 3, 16, 4, 0), EnochCalendar.getAbibOne(1776));
    // assertEquals(new DateTime(1619, 3, 16, 4, 0), EnochCalendar.getAbibOne(1619));
    assertEquals(new DateTime(1996, 3, 24, 4, 0), EnochCalendar.getAbibOne(1996));
    assertEquals(new DateTime(1995, 3, 19, 4, 0), EnochCalendar.getAbibOne(1995));
    assertEquals(new DateTime(800, 3, 19, 4, 0), EnochCalendar.getAbibOne(800));
    assertEquals(new DateTime(1, 3, 18, 4, 0), EnochCalendar.getAbibOne(1));
  }

  @Test
  public void getAbibOne_canFarIntoThefuture() {
    assertEquals(new DateTime(2050, 3, 20, 4, 0), EnochCalendar.getAbibOne(2050));
    assertEquals(new DateTime(2100, 3, 21, 4, 0), EnochCalendar.getAbibOne(2100));
    assertEquals(new DateTime(2200, 3, 23, 4, 0), EnochCalendar.getAbibOne(2200));
    assertEquals(new DateTime(2201, 3, 22, 4, 0), EnochCalendar.getAbibOne(2201));
    assertEquals(new DateTime(2500, 3, 21, 4, 0), EnochCalendar.getAbibOne(2500));
    assertEquals(new DateTime(3000, 3, 23, 4, 0), EnochCalendar.getAbibOne(3000));
  }
}

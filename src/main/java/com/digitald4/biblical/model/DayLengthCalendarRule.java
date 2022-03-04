package com.digitald4.biblical.model;

import com.google.common.collect.ImmutableList;

public class DayLengthCalendarRule implements CalendarRule {
  public static final DayLengthCalendarRule LAST_DAY_OF_LAST_YEAR_EQUAL_PARTS =
      DayLengthCalendarRule.of(1, -1, 9,
          "Enoch 72.32 And on that day the night becomes shorter, and amounts to nine " +
              "parts, and the day amounts to nine parts, and the night becomes equal with " +
              "the day. And the year amounts to exactly 364 days.");
  public static final DayLengthCalendarRule LAST_DAY_OF_FIRST_MONTH_TEN_PARTS =
      DayLengthCalendarRule.of(1, 30, 10,
          "Enoch 72.10 And on that day the day becomes longer than the night by a double " +
          "part, and the day amounts to exactly ten parts, and the night amounts to eight parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_SECOND_MONTH_ELEVEN_PARTS =
      DayLengthCalendarRule.of(2, 30, 11,
          "Enoch 72.12 And then the day becomes longer by two parts, and the day amounts " +
          "to eleven parts, and the night becomes shorter and amounts to seven parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_THIRD_MONTH_TWELVE_PARTS =
      DayLengthCalendarRule.of(3, 31, 12,
          "Enoch 72.14 And on that day the day becomes longer than the night, and the day becomes double the "
          + "night; and the day amounts to twelve parts, and the night becomes shorter and amounts to six parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_FOURTH_MONTH_ELEVEN_PARTS =
      DayLengthCalendarRule.of(4, 30, 11,
          "Enoch 72.16 And when thirty mornings have been completed the day becomes " +
              "shorter, by exactly one part; and the day amounts to eleven parts, and the night to seven parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_FIFTH_MONTH_TEN_PARTS =
      DayLengthCalendarRule.of(5, 30, 10,
          "Enoch 72.18 On that day the day becomes shorter by two parts, and the day " +
              "amounts to ten parts, and the night to eight parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_SIXTH_MONTH_NINE_PARTS =
      DayLengthCalendarRule.of(6, 31, 9,
          "Enoch 72.20 On that day the day becomes equal with the night, and is of equal " +
              "length; and the night amounts to nine parts, and the day to nine parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_SEVENTH_MONTH_EIGHT_PARTS =
      DayLengthCalendarRule.of(7, 30, 8,
          "Enoch 72.20 On that day the day becomes equal with the night, and is of equal " +
              "length; and the night amounts to nine parts, and the day to nine parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_EIGHTH_MONTH_SEVEN_PARTS =
      DayLengthCalendarRule.of(8, 30, 7,
          "Enoch 72.24 And on that day the night amounts to eleven parts and the day to seven parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_NINETH_MONTH_SIX_PARTS =
      DayLengthCalendarRule.of(9, 31, 6,
          "Enoch 72.26 And on that day the night becomes longer, and becomes double the " +
              "day; and the night amounts to exactly twelve parts, and the day to six parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_TENTH_MONTH_SEVEN_PARTS =
      DayLengthCalendarRule.of(10, 30, 7,
          "Enoch 72.28 And on that day the night becomes shorter in length by one part, and " +
              "amounts to eleven parts, and the day to seven parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_ELEVENTH_MONTH_EIGHT_PARTS =
      DayLengthCalendarRule.of(11, 30, 8,
          "Enoch 72.30 And on that day the night becomes shorter in length and the night " +
              "amounts to ten parts and the day to eight parts.");
  public static final DayLengthCalendarRule LAST_DAY_OF_TWELVETH_MONTH_NINE_PARTS =
      DayLengthCalendarRule.of(12, 31, 9,
          "Enoch 72.32 And on that day the night becomes shorter, and amounts to nine " +
              "parts, and the day amounts to nine parts, and the night becomes equal with " +
              "the day. And the year amounts to exactly 364 days.");

  public static final ImmutableList<CalendarRule> ALL = ImmutableList.of(
      LAST_DAY_OF_LAST_YEAR_EQUAL_PARTS,
      LAST_DAY_OF_FIRST_MONTH_TEN_PARTS,
      LAST_DAY_OF_SECOND_MONTH_ELEVEN_PARTS,
      LAST_DAY_OF_THIRD_MONTH_TWELVE_PARTS,
      LAST_DAY_OF_FOURTH_MONTH_ELEVEN_PARTS,
      LAST_DAY_OF_FIFTH_MONTH_TEN_PARTS,
      LAST_DAY_OF_SIXTH_MONTH_NINE_PARTS,
      LAST_DAY_OF_SEVENTH_MONTH_EIGHT_PARTS,
      LAST_DAY_OF_EIGHTH_MONTH_SEVEN_PARTS,
      LAST_DAY_OF_NINETH_MONTH_SIX_PARTS,
      LAST_DAY_OF_TENTH_MONTH_SEVEN_PARTS,
      LAST_DAY_OF_ELEVENTH_MONTH_EIGHT_PARTS,
      LAST_DAY_OF_TWELVETH_MONTH_NINE_PARTS);

  private int month;
  private int day;
  private int expectedDayParts;
  private String scripture;

  public DayLengthCalendarRule() {
  }

  public int getMonth() {
    return month;
  }

  public DayLengthCalendarRule setMonth(int month) {
    this.month = month;
    return this;
  }

  public int getDay() {
    return day;
  }

  public DayLengthCalendarRule setDay(int day) {
    this.day = day;
    return this;
  }

  public int getExpectedDayParts() {
    return expectedDayParts;
  }

  public DayLengthCalendarRule setExpectedDayParts(int expectedDayParts) {
    this.expectedDayParts = expectedDayParts;
    return this;
  }

  public String getScripture() {
    return scripture;
  }

  public DayLengthCalendarRule setScripture(String scripture) {
    this.scripture = scripture;
    return this;
  }

  public static DayLengthCalendarRule of(int month, int day, int expectedDayParts, String scripture) {
    return new DayLengthCalendarRule()
        .setMonth(month)
        .setDay(day)
        .setExpectedDayParts(expectedDayParts)
        .setScripture(scripture);
  }
}

package com.digitald4.biblical.model;

public class EventDate {
  /*LAST_DAY_OF_LAST_YEAR(1, -1),
  FIRST_DAY_OF_YEAR(1, 1),
  PASSOVER(1, 14),
  UNLEAVENED_BREAD(1, 15),
  PASSOVER_ALTERNATIVE(2, 14),
  PENTECOST(3, 10),
  FEAST_OF_TRUMPETS(7, 1),
  DAY_OF_ATONEMENT(7, 10),
  SUKKOTH(7, 15),
  FEAST_OF_DECICATION(9, 25),
  LAST_DAY_OF_YEAR(12, 31); */

  private final int month;
  private final int day;

  public EventDate(int month, int day) {
    this.month = month;
    this.day = day;
  }

  public int getMonth() {
    return month;
  }

  public int getDay() {
    return day;
  }

  public int getDayOfYear() {
    return (int) Math.floor((getMonth() - 1) * 30.334) + getDay();
  }

  public int getWeekOfYear() {
    return (getDayOfYear() - 1) / 7 + 1;
  }

  public static EventDate fromDayOfYear(int dayOfYear) {
    int month = (int) Math.ceil(dayOfYear / 30.334);
    int day = dayOfYear - (int) Math.floor((month - 1) * 30.334);
    return new EventDate(month, day);
  }
}

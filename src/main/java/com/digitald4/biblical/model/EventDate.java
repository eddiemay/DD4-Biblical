package com.digitald4.biblical.model;

public enum EventDate {
  LAST_DAY_OF_LAST_YEAR(1, -1),
  FIRST_DAY_OF_YEAR(1, 1),
  PASSOVER(1, 14),
  UNLEAVENED_BREAD(1, 15),
  PASSOVER_ALTERNATIVE(2, 14),
  PENTECOST(3, 10),
  FEAST_OF_TRUMPETS(7, 1),
  DAY_OF_ATONEMENT(7, 10),
  SUKKOTH(7, 15),
  FEAST_OF_DECICATION(9, 25),
  LAST_DAY_OF_YEAR(12, 31);

  private final int month;
  private final int day;

  EventDate(int month, int day) {
    this.month = month;
    this.day = day;
  }

  public int getMonth() {
    return month;
  }

  public int getDay() {
    return day;
  }
}

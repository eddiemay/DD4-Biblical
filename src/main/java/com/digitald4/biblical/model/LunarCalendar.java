package com.digitald4.biblical.model;

import org.joda.time.DateTime;

public class LunarCalendar implements BiblicalCalendar {
  // A lunar calendar rotates between 30 and 29 day months. Thus the average months is 29.5 days
  private static final double ONE_MONTH = (30 + 29) / 2.0 ;

  private final DateTime startDate;
  public LunarCalendar(DateTime startDate) {
    this.startDate = startDate;
  }

  @Override
  public DateTime getDate(int month, int day) {
    if (day > 0) day--;
    if (month > 0) month--;

    return startDate.plusDays((int) (ONE_MONTH * month + day));
  }

  @Override
  public DateTime getDate(EventDate eventDate) {
    return getDate(eventDate.getMonth(), eventDate.getDay());
  }
}

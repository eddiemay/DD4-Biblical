package com.digitald4.biblical.model;

import com.google.common.annotations.VisibleForTesting;
import org.joda.time.DateTime;

public class EnochCalendar implements BiblicalCalendar {
  // Enoch months are a cycle of 30, 30, 31 days thus the average month is 91/3 or 30 and 1/3 days
  private static final double ONE_MONTH = (30 + 30 + 31) / 3.0;
  private static final int BASE_YEAR = 1600;
  private static final DateTime BASE_ABIB = new DateTime(1600, 3, 19, 4, 0);

  private final DateTime startDate;
  public EnochCalendar(DateTime startDate) {
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

  @VisibleForTesting
  static DateTime getAbibOne(int year) {
    return getAbibOne50Then40YearSkip(year);
  }

  @VisibleForTesting
  static DateTime getAbibOne40YearSkip(int year) {
    int yearDiff = year - BASE_YEAR;
    int weeks = yearDiff * 52;
    if (yearDiff > 0) {
      yearDiff--;
    }

    // 400 years * 365.2425 / 7 = 20,871 weeks, 20,800 standard plus 71 leap weeks
    return BASE_ABIB.plusWeeks(weeks + yearDiff / 5 - yearDiff / 40 + yearDiff / 200 - yearDiff / 400);
  }

  @VisibleForTesting
  static DateTime getAbibOne45YearSkip(int year) {
    int yearDiff = year - BASE_YEAR;
    int weeks = yearDiff * 52;
    if (yearDiff > 0) {
      yearDiff--;
    }

    // 400 years * 365.2425 / 7 = 20,871 weeks, 20,800 standard plus 71 leap weeks
    return BASE_ABIB.plusWeeks(weeks + yearDiff / 5 - yearDiff / 45);
  }

  @VisibleForTesting
  static DateTime getAbibOne40_50YearSkip(int year) {
    int yearDiff = year - BASE_YEAR;
    int weeks = yearDiff * 52;
    if (yearDiff > 0) {
      yearDiff--;
    }

    weeks += yearDiff / 5 - yearDiff / 90 * 2;

    if (yearDiff % 90 > 40) {
      weeks--;
    }

    // 400 years * 365.2425 / 7 = 20,871 weeks, 20,800 standard plus 71 leap weeks
    return BASE_ABIB.plusWeeks(weeks);
  }

  @VisibleForTesting
  static DateTime getAbibOne50Then40YearSkip(int year) {
    int baseYear = year - (year % 400);
    int yearDiff = year - baseYear;
    int weeks = yearDiff * 52;

    DateTime baseAbib;
    if (baseYear == 0) {
      weeks -= 52;
      baseAbib = new DateTime(1, 3, 18, 4, 0);
    } else {
      baseAbib = new DateTime(baseYear, 3, 19, 4, 0);
    }
    yearDiff--;

    weeks += yearDiff / 5;

    // Skip every 50th year for the first 200 years.
    weeks -= yearDiff > 200 ? 4 : yearDiff / 50;

    // Skip every 40th year for the second 200 years.
    weeks -= yearDiff < 200 ? 0 : (yearDiff - 200) / 40;

    return baseAbib.plusWeeks(weeks);
  }

  @VisibleForTesting
  static DateTime getAbibOne50YearSkip(int year) {
    int baseYear = year - (year % 400);
    int yearDiff = year - baseYear;
    int weeks = yearDiff * 52;

    DateTime baseAbib;
    if (baseYear == 0) {
      weeks -= 52;
      baseAbib = new DateTime(1, 3, 18, 4, 0);
    } else {
      baseAbib = new DateTime(baseYear, 3, 19, 4, 0);
    }
    yearDiff--;

    // 400 years * 365.2425 / 7 = 20,871 weeks, 20,800 standard plus 71 leap weeks
    return baseAbib.plusWeeks(weeks + yearDiff / 5 - yearDiff / 50 - yearDiff / 275);
  }

  @VisibleForTesting
  static DateTime getAbibOneKingdomPreppers(int year) {
    DateTime baseAbib = new DateTime(2012, 4, 8, 4, 0);
    int yearDiff = year - 2000;
    int weeks = yearDiff * 52;
    if (yearDiff > 0) {
      yearDiff--;
    }

    // 400 years * 365.2425 / 7 = 20,871 weeks, 20,800 standard plus 71 leap weeks
    return baseAbib.plusWeeks(weeks + yearDiff / 6);
  }
}

package com.digitald4.biblical.model;

import org.joda.time.DateTime;

import java.time.Duration;

public class SunTimeData {
  private static final String DATE_TIME_FORMAT = "%sT%s.00Z";

  private final DateTime date;
  private final DateTime twilightStart;
  private final DateTime sunrise;
  private final DateTime sunset;
  private final DateTime twilightEnd;
  private final DateTime solarNoon;

  public SunTimeData(
      DateTime date, DateTime twilightStart, DateTime sunrise, DateTime sunset, DateTime twilightEnd, DateTime solarNoon) {
    this.date = date;
    this.twilightStart = twilightStart;
    this.sunrise = sunrise;
    this.sunset = sunset;
    this.twilightEnd = twilightEnd;
    this.solarNoon = solarNoon;
  }

  public DateTime getDate() {
    return date;
  }

  public DateTime getTwilightStart() {
    return twilightStart;
  }

  public DateTime getSunrise() {
    return sunrise;
  }

  public DateTime getSunset() {
    return sunset;
  }

  public DateTime getTwilightEnd() {
    return twilightEnd;
  }

  public DateTime getSolarNoon() {
    return solarNoon;
  }

  public Duration getDayLength() {
    return getSunDayLength();
  }

  public Duration getSunDayLength() {
    return Duration.ofMillis(sunset.getMillis() - sunrise.getMillis());
  }

  public Duration getTwilightDayLength() {
    return Duration.ofMillis(twilightEnd.getMillis() - twilightStart.getMillis());
  }

  public int getDayParts() {
    return (int) Math.round(getSunDayParts());
  }

  public double getSunDayParts() {
    // 14.25 -> +2.25 -> +4.00 -> 16.00 -> 12 (1.777)
    // 13.80 -> +1.80 -> +2.67 -> 14.67 -> 11 (1.483)
    // 13.00 -> +1.00 -> +1.33 -> 13.33 -> 10 (1.333)
    // 12.00 -> +0.00 -> +0.00 -> 12.00 -> 09
    // 11.33 -> -0.67 -> -1.33 -> 10.66 -> 08 (2.000)
    // 10.67 -> -1.33 -> -2.67 -> 09.33 -> 07 (2.000)
    // 10.00 -> -2.00 -> -4.00 -> 08.00 -> 06 (2.000)
    double hourLength = getDayLength().getSeconds() / 60.0 / 60;
    double distanceTo12 = hourLength - 12;
    double adder = (distanceTo12 > 0 ? 1.53 : 1.8) * distanceTo12;
    return (adder + 12) / 24 * 18;
  }

  public double getTwilightDayParts() {
    double hourLength = getTwilightDayLength().getSeconds() / 60.0 / 60;
    double distanceTo12 = hourLength - 12;
    double adder = 1.263 * distanceTo12;
    return (adder + 12) / 24 * 18;
  }

  public static SunTimeData create(
      String date, String twilightStart, String sunRise, String sunSet, String twilightEnd) {
    return new SunTimeData(
        DateTime.parse(String.format(DATE_TIME_FORMAT, date, "00:00:00")),
        DateTime.parse(String.format(DATE_TIME_FORMAT, date, twilightStart)),
        DateTime.parse(String.format(DATE_TIME_FORMAT, date, sunRise)),
        DateTime.parse(String.format(DATE_TIME_FORMAT, date, sunSet)),
        DateTime.parse(String.format(DATE_TIME_FORMAT, date, twilightEnd)),
        DateTime.parse(String.format(DATE_TIME_FORMAT, date, "12:00:00")));
  }
}

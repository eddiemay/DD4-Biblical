package com.digitald4.biblical.tools;

import com.digitald4.biblical.model.SunTimeData;
import com.digitald4.biblical.util.SunTimeUtil;
import com.digitald4.biblical.util.SunTimeUtilSunriseSunsetOrg;
import com.digitald4.common.server.APIConnector;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;

import java.util.stream.Collectors;

public class SunTimePrinter {
  private final SunTimeUtil sunTimeUtil;

  SunTimePrinter(SunTimeUtil sunTimeUtil) {
    this.sunTimeUtil = sunTimeUtil;
  }

  public void run(DateTime start, DateTime end) {
    DateTime current = start;
    ImmutableList.Builder<SunTimeData> closestSunDaysToEqual = ImmutableList.builder();
    ImmutableList.Builder<SunTimeData> closestTwilightDaysToEqual = ImmutableList.builder();
    SunTimeData longestSunDay = sunTimeUtil.getSunTimeData(current);
    SunTimeData shortestSunDay = sunTimeUtil.getSunTimeData(current);
    SunTimeData longestTwilightDay = sunTimeUtil.getSunTimeData(current);
    SunTimeData shortestTwilightDay = sunTimeUtil.getSunTimeData(current);
    System.out.println("Date, Twilight Start, Sunrise, Sunset, Twilight End, Sun");
    while (current.isBefore(end)) {
      SunTimeData sunTimeData = sunTimeUtil.getSunTimeData(current);
      System.out.printf("%s%n", getOutput(sunTimeData));
      if (longestSunDay.getSunDayLength().getSeconds() < sunTimeData.getSunDayLength().getSeconds()) {
        longestSunDay = sunTimeData;
      } else if (shortestSunDay.getSunDayLength().getSeconds() > sunTimeData.getSunDayLength().getSeconds()) {
        shortestSunDay = sunTimeData;
      }
      if (longestTwilightDay.getTwilightDayLength().getSeconds() < sunTimeData.getTwilightDayLength().getSeconds()) {
        longestTwilightDay = sunTimeData;
      } else if (shortestTwilightDay.getTwilightDayLength().getSeconds() > sunTimeData.getTwilightDayLength().getSeconds()) {
        shortestTwilightDay = sunTimeData;
      }
      if (Math.abs(12 * 60 - sunTimeData.getSunDayLength().getSeconds() / 60) < 8) {
        closestSunDaysToEqual.add(sunTimeData);
      }
      if (Math.abs(12 * 60 - sunTimeData.getTwilightDayLength().getSeconds() / 60) < 8) {
        closestTwilightDaysToEqual.add(sunTimeData);
      }
      current = current.plusDays(1);
    }
    System.out.printf("%nLongest Sun Day: %s%n", getOutput(longestSunDay));
    System.out.printf("Shortest Sun Day: %s%n", getOutput(shortestSunDay));
    System.out.printf("Longest Twilight Day: %s%n", getOutput(longestTwilightDay));
    System.out.printf("Shortest Twilight Day: %s%n", getOutput(shortestTwilightDay));
    System.out.printf("%n Closest Sun days to equal:%n%s",
        closestSunDaysToEqual.build().stream().map(SunTimePrinter::getOutput).collect(Collectors.joining("\n")));
    System.out.printf("%n Closest Twilight days to equal:%n%s",
        closestTwilightDaysToEqual.build().stream().map(SunTimePrinter::getOutput).collect(Collectors.joining("\n")));
  }

  private static String getOutput(SunTimeData sunTimeData) {
    return String.format("%s, %s, %s, %s, %s, %f, %f, %f, %f",
        sunTimeData.getDate().toString().substring(0, 10),
        getTime(sunTimeData.getTwilightStart()),
        getTime(sunTimeData.getSunrise()),
        getTime(sunTimeData.getSunset()),
        getTime(sunTimeData.getTwilightEnd()),
        sunTimeData.getSunDayLength().getSeconds() / 60.0 / 60,
        sunTimeData.getTwilightDayLength().getSeconds() / 60.0 / 60,
        sunTimeData.getSunDayParts(),
        sunTimeData.getTwilightDayParts());
  }

  private static String getTime(DateTime instant) {
    return instant.toString().substring(11, 19);
  }

  public static void main(String[] args) {
    new SunTimePrinter(new SunTimeUtilSunriseSunsetOrg(new APIConnector(null, null, 100)))
        .run(DateTime.parse("2021-01-01T00:00:00.00Z"), DateTime.parse("2021-12-31T00:00:00.00Z"));
  }
}

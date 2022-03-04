package com.digitald4.biblical.util;

import com.digitald4.biblical.model.SunTimeData;
import com.digitald4.common.server.APIConnector;
import com.google.common.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class SunTimeUtilSunriseSunsetOrg implements SunTimeUtil {
  private static final String URL = "https://sunrise-sunset.org/search?location=jerusalem&year=%d&month=%d#calendar";
  private static final String PARSE_TEMPLATE = "%sT%s.00Z";
  private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)(:\\d+:\\d+) ([ap]m)");

  private final Map<String, SunTimeData> cache = new HashMap<>();
  private final APIConnector apiConnector;

  @Inject
  public SunTimeUtilSunriseSunsetOrg(APIConnector apiConnector) {
    this.apiConnector = apiConnector;
  }

  @Override
  public SunTimeData getSunTimeData(DateTime date) {
    String strDate = date.toString().substring(0, 10);
    if (!cache.containsKey(strDate)) {
      fetchData(date);
    }

    return cache.get(strDate);
  }

  private synchronized void fetchData(DateTime date) {
    String strDate = date.toString().substring(0, 10);
    if (cache.containsKey(strDate)) {
      return;
    }

    String htmlResult = apiConnector.sendGet(String.format(URL, date.getYear(), date.getMonthOfYear()));
    Document doc = Jsoup.parse(htmlResult.trim(), "", Parser.xmlParser());
    Element table = doc.getElementById("month");
    Elements dayRows = table.getElementsByClass("day");
    for (Element dayRow : dayRows) {
      Elements data = dayRow.getElementsByTag("td");
      String entryDate = dayRow.attr("rel");
      cache.put(
          entryDate,
          new SunTimeData(
              DateTime.parse(String.format(PARSE_TEMPLATE, entryDate, "00:00:00")),
              DateTime.parse(String.format(PARSE_TEMPLATE, entryDate, to24Hr(data.get(0).text()))),
              DateTime.parse(String.format(PARSE_TEMPLATE, entryDate, to24Hr(data.get(1).text()))),
              DateTime.parse(String.format(PARSE_TEMPLATE, entryDate, to24Hr(data.get(2).text()))),
              DateTime.parse(String.format(PARSE_TEMPLATE, entryDate, to24Hr(data.get(3).text()))),
              DateTime.parse(String.format(PARSE_TEMPLATE, entryDate, to24Hr(data.get(5).text())))));
    }
  }

  @VisibleForTesting
  static String to24Hr(String time) {
    Matcher matcher = TIME_PATTERN.matcher(time.toLowerCase());
    if (matcher.find()) {
      int hour = Integer.parseInt(matcher.group(1));
      hour = hour == 12 ? 0 : hour;
      String minSec = matcher.group(2);
      hour += matcher.group(3).equals("pm") ? 12 : 0;
      return (hour < 10 ? "0" : "") + hour + minSec;
    }

    throw new IllegalArgumentException(
        "Time format did not match pattern, should be 'hr:min:sec am/pm' but was: " + time);
  }
}

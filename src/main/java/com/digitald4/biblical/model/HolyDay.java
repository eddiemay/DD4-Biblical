package com.digitald4.biblical.model;

import com.google.common.collect.ImmutableList;
import java.util.function.Function;

public class HolyDay {
  private static final String SEPARATE_DAYS = "Enoch 75:1-2";

  private final String title;
  private final String cssCls;
  private final EventDate start;
  private final EventDate end;
  private final Function<EventDate, Boolean> matcher;
  private final String references;
  private final String summary;
  private final String displaySummary;

  public HolyDay(String title, String cssCls, EventDate start, EventDate end, Function<EventDate, Boolean> matcher,
                 String references, String summary) {
    this.title = title;
    this.cssCls = cssCls;
    this.start = start;
    this.end = end;
    this.matcher = matcher;
    this.references = references;
    this.summary = summary == null ? "" : summary;
    this.displaySummary = String.format("<inline-scripture ref=\"%s\"></inline-scripture>%s", references, this.summary);
  }

  public HolyDay(String title, String cssCls, EventDate start, EventDate end, String references, String summary) {
    this(title, cssCls, start, end,
        date -> end == null ? date.getDayOfYear() == start.getDayOfYear() : date.getDayOfYear() >= start.getDayOfYear()
        && date.getDayOfYear() <= end.getDayOfYear(), references, summary);
  }

  public HolyDay(String title, String cssCls, EventDate start, EventDate end, String references) {
    this(title, cssCls, start, end, references, "");
  }

  public HolyDay(String title, String cssCls, EventDate start, String references) {
    this(title, cssCls, start, null, references, "");
  }

  public HolyDay(String title, String cssCls, Function<EventDate, Boolean> matcher, String references) {
    this(title, cssCls, null, null, matcher, references, "");
  }

  public String getTitle() {
    return title;
  }

  public String getCssCls() {
    return cssCls;
  }

  public EventDate getStart() {
    return start;
  }

  public EventDate getEnd() {
    return end;
  }

  public Function<EventDate, Boolean> getMatcher() {
    return matcher;
  }

  public String getReferences() {
    return references;
  }

  public String getSummary() {
    return summary;
  }

  public String getDisplaySummary() {
    return displaySummary;
  }

  public static final ImmutableList<HolyDay> HOLY_DAYS = ImmutableList.of(
      new HolyDay("New Month Feast", "new-month", date -> date.getDay() == 1 && date.getMonth() < 13,
          "Numbers 10:10,28:11-15"),
      new HolyDay("Passover", "passover", new EventDate(1, 14),
          "Exodus 12:6, Leviticus 23:5, Numbers 28:16, Deuteronomy 16:1, Jubilees 49:10"),
      new HolyDay("Festival of Unleavened Bread", "unleavened-bread", new EventDate(1, 15), new EventDate(1, 21),
          "Leviticus 23:6-8"),
      new HolyDay("Weave Offering", "weave-offering", new EventDate(1, 22), "Leviticus 23:10-14"),
      new HolyDay("Makeup Passover", "passover-makeup", new EventDate(2, 14), "Numbers 9:9-13"),
      new HolyDay("Feast of First Fruits", "first-fruits", EventDate.fromDayOfYear(21 + 50),
          "Leviticus 23:15-21, Deuteronomy 16:9-12, Jubilees 6:1,17"),
      new HolyDay("Summer Begins", "summer-begins", new EventDate(3, 31), "Enoch 72:13-14, " + SEPARATE_DAYS),
      new HolyDay("Autumn Begins", "autumn-begins", new EventDate(6, 31), "Enoch 72:19-20, " + SEPARATE_DAYS),
      new HolyDay("Feast of Trumpets", "feast-trumpets", new EventDate(7, 1), "Leviticus 23:24-25"),
      new HolyDay("Atonement Eve", "atonement-eve", new EventDate(7, 9), "Leviticus 23:32"),
      new HolyDay("Atonement Day", "atonement-day", new EventDate(7, 10), "Leviticus 23:27-32"),
      new HolyDay("Sukkot", "festival-booths", new EventDate(7, 15), new EventDate(7, 21), "Leviticus 23:34-43"),
      new HolyDay("Solemn Day of Assembly", "solemn-assembly", new EventDate(7, 22), "Leviticus 23:36"),
      new HolyDay("Festival of Dedication", "festival-dedication", new EventDate(9, 25), new EventDate(10, 1),
          "1 Maccabees 4:50-55,56-59, John 10:22"),
      new HolyDay("Winter Begins / Dedication Day 7", "winter-begins", new EventDate(9, 31),
          "Enoch 72:25-26, 1 Maccabees 4:59, John 10:22, " + SEPARATE_DAYS),
      new HolyDay("New Month Feast / Dedication Day 8", "new-month", new EventDate(10, 1),
          "Numbers 10:10,28:11-15, 1 Maccabees 4:59"),
      new HolyDay("Spring Begins / Last day of year", "spring-begins", new EventDate(12, 31),
          "Enoch 72:31-32, Jubilees 6:31-32, " + SEPARATE_DAYS),
      new HolyDay("Leap Week", "leap-week", new EventDate(13, 1), new EventDate(13, 7), "Enoch 74:11", "<p>"
          + "After 5 years the calendar is 6 days (6.2125 days to be exact 365.2425 - 364 = 1.2425 x 5) behind the "
          + "solar year. We need to do a leap week of 7 days in order to catch up. This leap is done at the end of "
          + "every 5 years except if that year is a Jubilee (50th year). In that case no leap is needed because the "
          + "Jubilee is made perfect from the excess of the .7875 days we have added over the last 9 leap years "
          + "(9 x .7875 = 7.0875)</p>")
  );
}


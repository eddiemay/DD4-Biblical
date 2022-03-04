package com.digitald4.biblical.model;

import org.joda.time.DateTime;

public interface BiblicalCalendar {
  enum Type {Lunar, Enoch}

  DateTime getDate(int month, int day);

  DateTime getDate(EventDate eventDate);
}

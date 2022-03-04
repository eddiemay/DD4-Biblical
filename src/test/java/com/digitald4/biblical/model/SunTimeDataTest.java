package com.digitald4.biblical.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SunTimeDataTest {
  private static final double DELTA = .4;
  private static final SunTimeData MARCH_15TH = // Mon, Mar 15	5:24:37 am	5:47:40 am	5:48:15 pm	6:11:18 pm
      SunTimeData.create("2021-03-15", "05:24:37", "05:47:40", "17:48:15", "18:11:18");
  private static final SunTimeData APRIL_15TH = // Thu, Apr 15	5:45:10 am	6:08:56 am	7:09:15 pm	7:33:01 pm
      SunTimeData.create("2021-04-15", "05:45:10", "06:08:56", "19:09:15", "19:33:01");
  private static final SunTimeData MAY_15TH = // Sat, May 15	5:15:14 am	5:40:41 am	7:30:13 pm	7:55:40 pm
      SunTimeData.create("2021-05-15", "05:15:14", "05:40:41", "19:30:13", "19:55:40");
  private static final SunTimeData JUNE_21TH = // Mon, Jun 21	5:05:58 am	5:32:46 am	7:49:06 pm	8:15:53 pm
      SunTimeData.create("2021-06-18", "05:05:58", "05:32:46", "19:49:06", "20:15:53");
  private static final SunTimeData JULY_22TH = // Thu, Jul 22	5:21:29 am	5:47:16 am	7:43:54 pm	8:09:41 pm
      SunTimeData.create("2021-07-22", "05:21:29", "05:47:16", "19:43:54", "20:09:41");
  private static final SunTimeData AUG_27TH = // Fri, Aug 27	5:46:40 am	6:10:26 am	7:10:43 pm	7:34:30 pm
      SunTimeData.create("2021-08-27", "05:46:40", "06:10:26", "19:10:43", "19:34:30");
  private static final SunTimeData SEPT_22TH = // Wed, Sep 22	6:03:07 am	6:26:12 am	6:37:18 pm	7:00:23 pm
      SunTimeData.create("2021-09-22", "06:03:07", "06:26:12", "18:37:18", "19:00:23");
  private static final SunTimeData OCT_16TH = // Sat, Oct 16	6:18:40 am	6:41:58 am	6:07:16 pm	6:30:35 pm
      SunTimeData.create("2021-10-16", "06:18:40", "06:41:58", "18:07:16", "18:30:35");
  private static final SunTimeData NOV_15TH = // Mon, Nov 15	5:41:44 am	6:06:12 am	4:41:12 pm	5:05:41 pm
      SunTimeData.create("2021-11-15", "05:41:44", "06:06:12", "16:41:12", "17:05:41");
  private static final SunTimeData DEC_21TH = // Tue, Dec 21	6:08:09 am	6:33:41 am	4:40:46 pm	5:06:18 pm
      SunTimeData.create("2021-12-21", "06:08:09", "06:33:41", "16:40:46", "17:06:18");
  private static final SunTimeData JAN_15TH = // Sat, Jan 15	6:13:04 am	6:38:02 am	4:58:53 pm	5:23:52 pm
      SunTimeData.create("2022-01-15", "06:13:04", "06:38:02", "16:58:53", "17:23:52");
  private static final SunTimeData FEB_15TH = // Tue, Feb 15	5:56:06 am	6:19:43 am	5:26:40 pm	5:50:17 pm
      SunTimeData.create("2022-02-15", "05:56:06", "06:19:43", "17:26:40", "17:50:17");

  @Test
  public void correctSunPartsForLongestDay() {
    assertEquals(12.0, JUNE_21TH.getSunDayParts(), DELTA);
  }

  @Test
  public void correctSunPartsForShortestDay() {
    assertEquals(6.5, DEC_21TH.getSunDayParts(), DELTA);
  }

  @Test
  public void correctSunPartsForEqualDays() {
    assertEquals(9, MARCH_15TH.getSunDayParts(), DELTA);
    assertEquals(9, SEPT_22TH.getSunDayParts(), DELTA);
  }

  @Test
  public void correctForNonSpecialLongerMonths() {
    assertEquals(10, APRIL_15TH.getSunDayParts(), DELTA);
    assertEquals(11, MAY_15TH.getSunDayParts(), DELTA);

    assertEquals(11, JULY_22TH.getSunDayParts(), DELTA);
    assertEquals(10, AUG_27TH.getSunDayParts(), DELTA);
  }

  @Test
  public void correctForNonSpecialShorterMonths() {
    assertEquals(8, OCT_16TH.getSunDayParts(), DELTA);
    assertEquals(7, NOV_15TH.getSunDayParts(), DELTA);
    
    assertEquals(7, JAN_15TH.getSunDayParts(), DELTA);
    assertEquals(8, FEB_15TH.getSunDayParts(), DELTA);
  }
}

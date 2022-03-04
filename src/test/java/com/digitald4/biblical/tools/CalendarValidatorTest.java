package com.digitald4.biblical.tools;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.digitald4.biblical.model.*;
import com.digitald4.biblical.util.SunTimeUtil;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class CalendarValidatorTest {
  private static final DateTime JUNE_21TH = DateTime.parse("2021-06-21T00:00:00.00Z");
  private static final DateTime JULY_22TH = DateTime.parse("2021-07-22T00:00:00.00Z");
  private static final SunTimeData JUNE_21TH_SUNTIME = // Mon, Jun 21	5:05:58 am	5:32:46 am	7:49:06 pm	8:15:53 pm
      SunTimeData.create("2021-06-18", "05:05:58", "05:32:46", "19:49:06", "20:15:53");
  private static final SunTimeData JULY_22TH_SUNTIME = // Thu, Jul 22	5:21:29 am	5:47:16 am	7:43:54 pm	8:09:41 pm
      SunTimeData.create("2021-07-22", "05:21:29", "05:47:16", "19:43:54", "20:09:41");
  @Mock private final SunTimeUtil sunTimeUtil = mock(SunTimeUtil.class);
  @Mock private final BiblicalCalendar calendar = mock(BiblicalCalendar.class);

  private CalendarValidator calendarValidator;

  @Before
  public void setup() {
    calendarValidator = new CalendarValidator(sunTimeUtil);

    when(sunTimeUtil.getSunTimeData(JUNE_21TH)).thenReturn(JUNE_21TH_SUNTIME);
    when(sunTimeUtil.getSunTimeData(JULY_22TH)).thenReturn(JULY_22TH_SUNTIME);
  }

  @Test
  public void validate_goodDay() {
    when(calendar.getDate(3, 31)).thenReturn(JUNE_21TH);

    CalendarValidationResults results =
        calendarValidator.validate(calendar, ImmutableList.of(DayLengthCalendarRule.LAST_DAY_OF_THIRD_MONTH_TWELVE_PARTS));

    assertEquals(0, results.getErrors().size());
    assertEquals(0, results.getWarnings().size());
    assertEquals(1, results.getMatching().size());
  }

  @Test
  public void validate_badDay() {
    when(calendar.getDate(3, 31)).thenReturn(JULY_22TH);

    CalendarValidationResults results =
        calendarValidator.validate(calendar, ImmutableList.of(DayLengthCalendarRule.LAST_DAY_OF_THIRD_MONTH_TWELVE_PARTS));

    assertEquals(1, results.getErrors().size());
    assertEquals(0, results.getWarnings().size());
    assertEquals(0, results.getMatching().size());
  }

  @Test
  public void singleValidate_goodDay() {
    when(calendar.getDate(3, 31)).thenReturn(JUNE_21TH);

    CalendarValidationResult result = calendarValidator.validate(calendar, DayLengthCalendarRule.LAST_DAY_OF_THIRD_MONTH_TWELVE_PARTS);

    assertEquals(CalendarValidationResult.Type.MATCHING, result.getType());
  }

  @Test
  public void singleValidate_badDay() {
    when(calendar.getDate(3, 31)).thenReturn(JULY_22TH);

    CalendarValidationResult result = calendarValidator.validate(calendar, DayLengthCalendarRule.LAST_DAY_OF_THIRD_MONTH_TWELVE_PARTS);

    assertEquals(CalendarValidationResult.Type.ERROR, result.getType());
  }
}

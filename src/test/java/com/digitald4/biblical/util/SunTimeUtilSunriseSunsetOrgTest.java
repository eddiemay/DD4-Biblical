package com.digitald4.biblical.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import com.digitald4.biblical.model.SunTimeData;
import com.digitald4.common.server.APIConnector;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Duration;

public class SunTimeUtilSunriseSunsetOrgTest {

  @Mock private final APIConnector apiConnector = mock(APIConnector.class);
  private SunTimeUtilSunriseSunsetOrg suntimeUtil;

  @Before
  public void setup() {
    suntimeUtil = new SunTimeUtilSunriseSunsetOrg(apiConnector);
  }

  @Test
  public void getSunTimeData() {
    when(apiConnector.sendGet(anyString())).thenReturn(
        """
            <html></body><h2>blah blah blah</h2><table id="month"><col><col><col><col><col><col><col><col><col><col><tbody><tr class="headers"><th rowspan="2">Day</th><th rowspan="2">Twilight start</th>
            <th rowspan="2">Sunrise</th><th rowspan="2">Sunset</th><th rowspan="2">Twilight end</th>
            <th rowspan="2">Day length</th><th rowspan="2">Solar noon</th><th colspan="2">Nautical twilight</th><th colspan="2">Astronomical twilight</th></tr>
            <tr class="headers"><th>Start</th><th>End</th><th>Start</th><th>End</th></tr><tr class="day" rel="2021-08-01">
            <th><span class="number">Sun, Aug 1</span></th>
            <td>5:34:59 am</td>
            <td><span class="sunrise" title="Sunrise time 2021-08-01 in Norco, California"><strong>6:00:55 am</strong></span></td>
            <td><span title="Sunset time 2021-08-01 in Norco, California" class="sunset"><strong>7:52:05 pm</strong></span></td>
            <td>8:18:02 pm</td>
            <td>13:51:10</td>
            <td>12:56:30 pm</td>
            <td class="light">5:01 am</td>
            <td class="light">8:51 pm</td>
            <td class="light">4:26 am</td>
            <td class="light">9:26 pm</td>
            </tr>
            <tr class="day" rel="2021-08-02">
            <th><span class="number">Mon, Aug 2</span></th>
            <td>5:35:46 am</td>
            <td><span class="sunrise" title="Sunrise time 2021-08-02 in Norco, California"><strong>6:01:38 am</strong></span></td>
            <td><span title="Sunset time 2021-08-02 in Norco, California" class="sunset"><strong>7:51:13 pm</strong></span></td>
            <td>8:17:06 pm</td>
            <td>13:49:35</td>
            <td>12:56:26 pm</td>
            <td class="light">5:02 am</td>
            <td class="light">8:49 pm</td>
            <td class="light">4:28 am</td>
            <td class="light">9:24 pm</td>
            </tr>
            <tr class="day" rel="2021-08-03">
            <th><span class="number">Tue, Aug 3</span></th>
            <td>5:36:33 am</td>
            <td><span class="sunrise" title="Sunrise time 2021-08-03 in Norco, California"><strong>6:02:22 am</strong></span></td>
            <td><span title="Sunset time 2021-08-03 in Norco, California" class="sunset"><strong>7:50:20 pm</strong></span></td>
            <td>8:16:08 pm</td>
            <td>13:47:58</td>
            <td>12:56:21 pm</td>
            <td class="light">5:03 am</td>
            <td class="light">8:48 pm</td>
            <td class="light">4:29 am</td>
            <td class="light">9:23 pm</td>
            </tr></table></body></html>""");

    SunTimeData sunTimeData = suntimeUtil.getSunTimeData(DateTime.parse("2021-08-02T06:52:00.00Z"));
    assertEquals(DateTime.parse("2021-08-02T00:00:00.00Z"), sunTimeData.getDate());
    assertEquals(DateTime.parse("2021-08-02T05:35:46.00Z"), sunTimeData.getTwilightStart());
    assertEquals(DateTime.parse("2021-08-02T06:01:38.00Z"), sunTimeData.getSunrise());
    assertEquals(DateTime.parse("2021-08-02T19:51:13.00Z"), sunTimeData.getSunset());
    assertEquals(DateTime.parse("2021-08-02T20:17:06.00Z"), sunTimeData.getTwilightEnd());
    assertEquals(DateTime.parse("2021-08-02T12:56:26.00Z"), sunTimeData.getSolarNoon());
    assertEquals(Duration.parse("PT13H49M35S"), sunTimeData.getDayLength());
    assertEquals(11, sunTimeData.getDayParts());

    sunTimeData = suntimeUtil.getSunTimeData(DateTime.parse("2021-08-01T00:00:00.00Z"));
    assertEquals(DateTime.parse("2021-08-01T00:00:00.00Z"), sunTimeData.getDate());
    assertEquals(DateTime.parse("2021-08-01T05:34:59.00Z"), sunTimeData.getTwilightStart());
    assertEquals(DateTime.parse("2021-08-01T06:00:55.00Z"), sunTimeData.getSunrise());
    assertEquals(DateTime.parse("2021-08-01T19:52:05.00Z"), sunTimeData.getSunset());
    assertEquals(DateTime.parse("2021-08-01T20:18:02.00Z"), sunTimeData.getTwilightEnd());
    assertEquals(DateTime.parse("2021-08-01T12:56:30.00Z"), sunTimeData.getSolarNoon());
    assertEquals(Duration.parse("PT13H51M10S"), sunTimeData.getDayLength());
    assertEquals(11, sunTimeData.getDayParts());

    sunTimeData = suntimeUtil.getSunTimeData(DateTime.parse("2021-08-03T00:00:00.00Z"));
    assertEquals(DateTime.parse("2021-08-03T00:00:00.00Z"), sunTimeData.getDate());
    assertEquals(DateTime.parse("2021-08-03T05:36:33.00Z"), sunTimeData.getTwilightStart());
    assertEquals(DateTime.parse("2021-08-03T06:02:22.00Z"), sunTimeData.getSunrise());
    assertEquals(DateTime.parse("2021-08-03T19:50:20.00Z"), sunTimeData.getSunset());
    assertEquals(DateTime.parse("2021-08-03T20:16:08.00Z"), sunTimeData.getTwilightEnd());
    assertEquals(DateTime.parse("2021-08-03T12:56:21.00Z"), sunTimeData.getSolarNoon());
    assertEquals(Duration.parse("PT13H47M58S"), sunTimeData.getDayLength());
    assertEquals(11, sunTimeData.getDayParts());

    verify(apiConnector, times(1))
        .sendGet("https://sunrise-sunset.org/search?location=jerusalem&year=2021&month=8#calendar");
  }

  @Test
  public void to24Hr() {
    assertEquals("09:23:45", SunTimeUtilSunriseSunsetOrg.to24Hr("9:23:45 am"));
    assertEquals("22:41:05", SunTimeUtilSunriseSunsetOrg.to24Hr("10:41:05 pm"));
    assertEquals("10:41:15", SunTimeUtilSunriseSunsetOrg.to24Hr("10:41:15 am"));
    assertEquals("12:02:32", SunTimeUtilSunriseSunsetOrg.to24Hr("12:02:32 pm"));
    assertEquals("00:32:32", SunTimeUtilSunriseSunsetOrg.to24Hr("12:32:32 am"));
  }
}

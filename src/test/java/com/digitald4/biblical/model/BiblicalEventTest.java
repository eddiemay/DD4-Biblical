package com.digitald4.biblical.model;

import static com.google.common.truth.Truth.assertThat;

import com.digitald4.biblical.model.BiblicalEvent.Duration;
import org.junit.Test;

public class BiblicalEventTest {

  @Test
  public void display_correctly() {
    BiblicalEvent event = new BiblicalEvent().setTitle("Event");

    assertThat(event.display()).isEqualTo("Event 0AM (3961BCE)");
    assertThat(event.setYear(1600).display()).isEqualTo("Event 1600AM (2361BCE)");
    assertThat(event.setMonth(3).setDay(17).display()).isEqualTo("Event 3/17/1600AM (2361BCE)");
    assertThat(event.setDuration(new Duration().setValue("50y")).display())
        .isEqualTo("Event 1600-1650AM (2361-2311BCE) (50 years)");
    assertThat(event.setDuration(new Duration().setValue("420d")).display())
        .isEqualTo("Event 1600-1601AM (2361-2360BCE) (420 days)");
    assertThat(event.setDuration(new Duration().setValue("50")).display())
        .isEqualTo("Event 1600-1650AM (2361-2311BCE) (50 years)");
    assertThat(event.setDuration(new Duration().setValue("50y5m")).display())
        .isEqualTo("Event 1600-1650AM (2361-2311BCE) (50 years 5 months)");
    assertThat(event.setDuration(new Duration().setValue("50y6m")).display())
        .isEqualTo("Event 1600-1651AM (2361-2310BCE) (50 years 6 months)");
    assertThat(event.setDuration(new Duration().setValue("3m10d")).display())
        .isEqualTo("Event 3/17/1600AM (2361BCE) (3 months 10 days)");
    assertThat(event.setMonth(0).setDay(0).setDuration(new Duration().setValue("3m10d")).display())
        .isEqualTo("Event 1600AM (2361BCE) (3 months 10 days)");
    assertThat(event.setYear(4030).display()).isEqualTo("Event 4030AM (70CE) (3 months 10 days)");
  }

  @Test
  public void display_acrossEras() {
    BiblicalEvent event = new BiblicalEvent();

    assertThat(event.setTitle("Days of Greeks").setYear(3700).setDuration(new Duration().setValue("2300y")).display())
        .isEqualTo("Days of Greeks 3700-6000AM (261BCE-2040CE) (2300 years)");
    assertThat(event.setTitle("Life of Messiah").setYear(3957).setDuration(new Duration().setValue("33y6m")).display())
        .isEqualTo("Life of Messiah 3957-3991AM (4BCE-31CE) (33 years 6 months)");
  }

  @Test
  public void durationParse() {
    assertThat(new Duration().setValue("-100y").getYears()).isEqualTo(-100);
  }
}

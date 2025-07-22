package com.digitald4.biblical.model;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.ModelObject;
import com.google.api.server.spi.config.ApiResourceProperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BiblicalEvent extends ModelObject<Long> {
  private static final Pattern DURATION_PATTERN = Pattern.compile("(-?\\d+)([ymd])");
  private static final Pattern SCRIPTURE_PATTERN = Pattern.compile("<inline-scripture ref=\"(.+)\"");
  private static final int ONE_BCE = 3960;

  private String title;
  private StringBuilder summary;
  private int month;
  private int day;
  private Long depEventId;
  private Dependency.Relationship depRelationship;
  private Duration offset;
  private int year;
  private Duration duration;
  private int endYear;

  public BiblicalEvent setId(Long id) {
    super.setId(id);
    return this;
  }

  public int getMonth() {
    return month;
  }

  public BiblicalEvent setMonth(int month) {
    this.month = month;
    return this;
  }

  public int getDay() {
    return day;
  }

  public BiblicalEvent setDay(int day) {
    this.day = day;
    return this;
  }

  public int getYear() {
    return year;
  }

  public BiblicalEvent setYear(int year) {
    this.year = year;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public BiblicalEvent setTitle(String title) {
    this.title = title;
    return this;
  }

  public StringBuilder getSummary() {
    return summary;
  }

  public BiblicalEvent setSummary(StringBuilder summary) {
    this.summary = summary;
    return this;
  }

  public BiblicalEvent setSummary(String summary) {
    this.summary = new StringBuilder(summary);
    return this;
  }

  public Long getDepEventId() {
    return depEventId;
  }

  public BiblicalEvent setDepEventId(Long depEventId) {
    this.depEventId = depEventId;
    return this;
  }

  public Dependency.Relationship getDepRelationship() {
    return depRelationship;
  }

  public BiblicalEvent setDepRelationship(Dependency.Relationship depRelationship) {
    this.depRelationship = depRelationship;
    return this;
  }

  public Duration getOffset() {
    return offset;
  }

  public BiblicalEvent setOffset(Duration offset) {
    this.offset = offset;
    return this;
  }

  public Duration getDuration() {
    return duration;
  }

  public BiblicalEvent setDuration(Duration duration) {
    this.duration = duration;
    return this;
  }

  public int getEndYear() {
    if (endYear == 0) {
      return add(getYear(), getDuration());
    }
    return endYear;
  }

  public BiblicalEvent setEndYear(int endYear) {
    this.endYear = endYear;
    return this;
  }

  @ApiResourceProperty
  public String display() {
    int startYear = getYear();
    int endYear = getEndYear();
    int month = getMonth();
    int day = getDay();
    String duration = getDuration() == null || getDuration().toString().isEmpty() ? "" : " (" + getDuration() + ")";

    if (startYear == endYear && month > 0 && day > 0) {
      return String.format(
          "%s %d/%d/%dAM (%s)%s", getTitle(), month, day, startYear, getModernEra(startYear), duration);
    }

    if (startYear == endYear) {
      return String.format("%s %dAM (%s)%s", getTitle(), startYear, getModernEra(startYear), duration);
    }

    boolean showStartEra = startYear <= ONE_BCE && endYear > ONE_BCE;

    return String.format("%s %d-%dAM (%s-%s)%s",
        getTitle(), startYear, endYear, getModernEra(startYear, showStartEra), getModernEra(endYear), duration);
  }

  @ApiResourceProperty
  public String displayAmOnly() {
    int startYear = getYear();
    int endYear = getEndYear();
    int month = getMonth();
    int day = getDay();
    String duration = getDuration() == null || getDuration().toString().isEmpty() ? "" : " (" + getDuration() + ")";

    if (startYear == endYear && month > 0 && day > 0) {
      return String.format("%s %d/%d/%dAM%s", getTitle(), month, day, startYear, duration);
    }

    if (startYear == endYear) {
      return String.format("%s %dAM%s", getTitle(), startYear, duration);
    }

    return String.format("%s %d-%dAM%s", getTitle(), startYear, endYear, duration);
  }

  @ApiResourceProperty
  public String displayEraOnly() {
    int startYear = getYear();
    int endYear = getEndYear();
    int month = getMonth();
    int day = getDay();
    String duration = getDuration() == null || getDuration().toString().isEmpty() ? "" : " (" + getDuration() + ")";

    if (startYear == endYear && month > 0 && day > 0) {
      return String.format(
          "%s %d/%d/%s%s", getTitle(), month, day, getModernEra(startYear), duration);
    }

    if (startYear == endYear) {
      return String.format("%s %s%s", getTitle(), getModernEra(startYear), duration);
    }

    boolean showStartEra = startYear <= ONE_BCE && endYear > ONE_BCE;

    return String.format(
        "%s %s-%s%s", getTitle(), getModernEra(startYear, showStartEra), getModernEra(endYear), duration);
  }

  private static String getModernEra(int year) {
    return getModernEra(year, true);
  }

  private static String getModernEra(int year, boolean showEra) {
    String era;
    int eraYear;
    if (year <= ONE_BCE) {
      eraYear = (ONE_BCE + 1 - year);
      era = "BCE";
    } else {
      eraYear = (year - ONE_BCE);
      era = "CE";
    }
    return String.format("%d%s", eraYear, showEra ? era : "");
  }

  public String referenceScriptures() {
    Matcher matcher = SCRIPTURE_PATTERN.matcher(getSummary());
    return (matcher.find()) ? matcher.group(1) : null;
  }

  public static class Dependency {
    public enum Relationship {START_TO_START, FINISH_TO_START, START_TO_FINISH, FINISH_TO_FINISH}

    private long eventId;
    private Relationship relationship = Relationship.START_TO_START;

    public long getEventId() {
      return eventId;
    }

    public Dependency setEventId(long eventId) {
      this.eventId = eventId;
      return this;
    }

    public Relationship getRelationship() {
      return relationship;
    }

    public Dependency setRelationship(Relationship relationship) {
      this.relationship = relationship;
      return this;
    }
  }

  public static class Duration {
    private Integer years;
    private Integer months;
    private Integer days;

    public Integer getYears() {
      return years;
    }

    public Duration setYears(Integer years) {
      this.years = years;
      return this;
    }

    public Integer getMonths() {
      return months;
    }

    public Duration setMonths(Integer months) {
      this.months = months;
      return this;
    }

    public Integer getDays() {
      return days;
    }

    public Duration setDays(Integer days) {
      this.days = days;
      return this;
    }

    @ApiResourceProperty
    public String value() {
      return years == null ? "" : years + "y" + (months == null ? "" : months + "m") + (days == null ? "" : days + "d");
    }

    public String toString() {
      return ((years == null ? "" : years + " years")
          + (months == null ? "" : " " + months + " months")
          + (days == null ? "" : " " + days + " days")).trim();
    }

    public Duration setValue(String value) {
      Matcher matcher = DURATION_PATTERN.matcher(value);
      if (matcher.find()) {
        do {
          int val = Integer.parseInt(matcher.group(1));
          switch (matcher.group(2).charAt(0)) {
            case 'm': setMonths(val); break;
            case 'd': setDays(val); break;
            case 'y': setYears(val); break;
            default:
              throw new DD4StorageException("Unknown duration: " + value, DD4StorageException.ErrorCode.BAD_REQUEST);
          }
        } while (matcher.find());
      } else if (!value.isEmpty()) {
        setYears(Integer.parseInt(value));
      }

      return this;
    }

    public int years() {
      return (int) Math.round(
          (years == null ? 0 : years) + (months == null ? 0 : months / 12.0) + (days == null ? 0 : days / 364.0));
    }
  }

  public static int add(int baseYear, Duration duration) {
    if (duration == null) {
      return baseYear;
    }

    return baseYear + duration.years();
  }
}

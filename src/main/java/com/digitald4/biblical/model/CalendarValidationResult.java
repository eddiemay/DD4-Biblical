package com.digitald4.biblical.model;

public class CalendarValidationResult {
  public enum Type {
    MATCHING,
    WARNING,
    ERROR
  }

  private final Type type;
  private final String message;
  public CalendarValidationResult(Type type, String message) {
    this.type = type;
    this.message = message;
  }

  public static CalendarValidationResult createMatching(String message) {
    return new CalendarValidationResult(Type.MATCHING, message);
  }

  public static CalendarValidationResult createWarning(String message) {
    return new CalendarValidationResult(Type.WARNING, message);
  }

  public static CalendarValidationResult createError(String message) {
    return new CalendarValidationResult(Type.ERROR, message);
  }

  public Type getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return String.format("%s - %s", getType(), getMessage());
  }
}

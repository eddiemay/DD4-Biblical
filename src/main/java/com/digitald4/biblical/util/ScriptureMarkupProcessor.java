package com.digitald4.biblical.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptureMarkupProcessor {
  private static final Pattern SCRIPTURE_PATTERN = Pattern.compile("\\(([\\w ]+\\.? \\d+[:\\w,; \\-–]*)\\)");
  private static final String SCRIPTURE_TEMPLATE = "(<scripture ref=\"%s\"/>)";
  private static final Pattern INLINE_SCRIPTURE_PATTERN = Pattern.compile("\\[([\\w ]+\\.? \\d+[:\\w,; \\-–]*)]");
  private static final String INLINE_SCRIPTURE_TEMPLATE = "(<inline-scripture ref=\"%s\"/>)";

  public String replaceScriptures(String content) {
    if (content == null) {
      return null;
    }

    Matcher matcher = SCRIPTURE_PATTERN.matcher(content);
    while (matcher.find()) {
      content = content.replace(matcher.group(), String.format(SCRIPTURE_TEMPLATE, matcher.group(1)));
    }

    matcher = INLINE_SCRIPTURE_PATTERN.matcher(content);
    while (matcher.find()) {
      content = content.replace(matcher.group(), String.format(INLINE_SCRIPTURE_TEMPLATE, matcher.group(1)));
    }

    return content;
  }

  public StringBuilder replaceScriptures(StringBuilder content) {
    if (content == null) {
      return null;
    }

    return new StringBuilder(replaceScriptures(content.toString()));
  }
}

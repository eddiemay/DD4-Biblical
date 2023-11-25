package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;

public class HebrewConverter {
  enum AlefBet {
    Alef("Alef", 'א', "𐤀", "𓃾", 'ࠀ', 'ا', 1, "A", "ox head, strength"),
    Bet("Bet", 'ב', "𐤁", "𓉔", 'ࠀ', 'ا', 2, "B", "house"),
    Gimel("Gimel", 'ג', "𐤂", "𓃀", 'ࠀ', 'ا', 3, "G", "foot, camel"),
    Dalet("Dalet", 'ד', "𐤃", "𓇯", 'ࠀ', 'ا', 4, "D", "door"),
    Hey("Hey", 'ה',"𐤄", "𓀠", 'ࠀ', 'ا', 5, "H", "jubilation, window"),
    Wav("Wav", 'ו', "𐤅", "𓏲", 'ࠀ', 'ا', 6, "W", "hook"),
    Zayin("Zayin", 'ז', "𐤆", "𓌻", 'ࠀ', 'ا' , 7, "Z", "weapon"),
    Chet("Chet", 'ח', "𐤇", "𓈈", 'ࠀ', 'ا' , 8, "Ch", "courtyard, gate"),
    Tet("Tet", 'ט', "𐤈", "𐤈", 'ࠀ', 'ا', 9, "T", "wheel"),
    Yod("Yod", 'י', "𐤉", "𓂝", 'ࠀ', 'ا', 10, "Y", "arm, hand"),
    Kaf("Kaf", 'כ', "𐤊", "𓂩", 'ࠀ', 'ا', 20, "K", "palm of hand", 'ך'),
    Lamed("Lamed", 'ל', "𐤋", "𓏱", 'ࠀ', 'ا' , 30, "L", "goad, staff"),
    Mem("Mem", 'מ', "𐤌", "𓈖", 'ࠀ', 'ا', 40, "M", "water, life", 'ם'),
    Nun("Nun", 'נ', "𐤍", "𓆓", 'ࠀ', 'ا', 50, "N", "fish", 'ן'),
    Samekh("Samekh", 'ס', "𐤎", "𓊽", 'ࠀ', 'ا', 60, "S", "pillar, support"),
    Ayin("Ayin", 'ע',"𐤏", "𓁹", 'ࠀ', 'ا', 70, "I", "eye"),
    Pay("Pay", 'פ', "𐤐", "𓂋", 'ࠀ', 'ا' , 80, "P", "mouth", 'ף'),
    Tzadi("Tzadi", 'צ', "𐤑", "𓄘", 'ࠀ', 'ا', 90, "Tz", "Man on side, desire, need", 'ץ'),
    Qof("Qof", 'ק', "𐤒", "𐤒", 'ࠀ', 'ا', 100, "Q", "eye of needle"),
    Resh("Resh", 'ר', "𐤓", "𓁶", 'ࠀ', 'ا' , 200, "R", "man head"),
    Shin("Shin", 'ש', "ש", "𓌓", 'ࠀ', 'ا', 300, "Sh", "tooth"),
    Tav("Tav", 'ת', "𐤕", "𓏴", 'ࠀ', 'ا' , 400, "T", "mark, sign");

    public final String name;
    public final char modern;
    public final String paleo;
    public final String ancient;
    public final char samaritan;
    public final char arabic;
    public final int value;
    public final String english;
    public final String meaning;
    public final Character finalModern;

    AlefBet(String name, char modern, String paleo, String ancient, char samaritan, char arabic,
        int value, String english, String meaning) {
      this(name, modern, paleo, ancient, samaritan, arabic, value, english, meaning, null);
    }

    AlefBet(String name, char modern, String paleo, String ancient, char samaritan, char arabic,
        int value, String english, String meaning, Character finalModern) {
      this.name = name;
      this.modern = modern;
      this.paleo = paleo;
      this.ancient = ancient;
      this.samaritan = samaritan;
      this.arabic = arabic;
      this.value = value;
      this.english = english;
      this.meaning = meaning;
      this.finalModern = finalModern;
    }

    public String getName() {
      return name;
    }

    public char modern() {
      return modern;
    }

    public String paleo() {
      return paleo;
    }

    public String ancient() {
      return ancient;
    }

    public char samaritan() {
      return samaritan;
    }

    public char arabic() {
      return arabic;
    }

    public int value() {
      return value;
    }

    public String english() {
      return english;
    }

    public String meaning() {
      return meaning;
    }

    public char finalModern() {
      return finalModern;
    }
  }

  public static final ImmutableMap<Character, String> PICTOGRAPH_MAP = ImmutableMap.<Character, String>builder()
      .putAll(stream(AlefBet.values()).collect(toImmutableMap(AlefBet::modern, AlefBet::ancient)))
      .putAll(
          stream(AlefBet.values())
              .filter(alefBet -> alefBet.finalModern != null)
              .collect(toImmutableMap(AlefBet::finalModern, AlefBet::ancient)))
      .build();

  public static String removePunctuation(String hebrew) {
    hebrew = hebrew
        .replaceAll("־", " ").replaceAll("׀ ", "").replaceAll("‸", "").replaceAll("\\.", "")
        .replaceAll("\\[", "").replaceAll("]", "").replaceAll("\\(", "").replaceAll("\\)", "");
    if (hebrew.indexOf("׃") > 0) {
      hebrew = hebrew.substring(0, hebrew.indexOf("׃")).trim();
    }
    return hebrew;
  }

  public static String removePunctuation(StringBuilder hebrew) {
    return removePunctuation(hebrew.toString());
  }

  public static String toAncient(char c) {
    if (c == ' ' || c == '־') {
      return "\u00a0 \u00a0";
    }
    // if (!PICTOGRAPH_MAP.containsKey(c) && c != ' ' && c != '־' && c != '׃') throw new IllegalArgumentException("Unknown char: " + c);

    return PICTOGRAPH_MAP.getOrDefault(c, String.valueOf(c));
  }

  public static String toAncient(String text) {
    return removePunctuation(text).chars().mapToObj(c -> toAncient((char) c)).collect(joining());
  }

  public static String toAncient(StringBuilder text) {
    return toAncient(text.toString());
  }

  public static String toAncientRtl(String text) {
    List<String> ancient = removePunctuation(text).chars().mapToObj(c -> toAncient((char) c)).collect(toList());
    Collections.reverse(ancient);
    return String.join("", ancient);
  }

  public static String toAncientRtl(StringBuilder text) {
    return toAncientRtl(text.toString());
  }

  public static String toConstantsOnly(String text) {
    return removePunctuation(text).chars().filter(c -> c < 1425 || c > 1479)
        .mapToObj(c -> String.valueOf((char) c)).collect(joining()).trim();
  }

  public static String toConstantsOnly(StringBuilder text) {
    return toConstantsOnly(text.toString());
  }

  public static String toStrongsId(String strongsRef) {
    switch (strongsRef.length()) {
      case 2: return strongsRef.charAt(0) + "000" + strongsRef.substring(1);
      case 3: return strongsRef.charAt(0) + "00" + strongsRef.substring(1);
      case 4: return strongsRef.charAt(0) + "0" + strongsRef.substring(1);
    }
    return strongsRef;
  }
}

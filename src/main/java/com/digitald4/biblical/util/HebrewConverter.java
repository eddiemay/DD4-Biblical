package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class HebrewConverter {
  public enum AlefBet {
    Alef("Alef", 'א', "𐤀", "𓃾", 'ࠀ', 'ا', 1, "A", "ox head, strength", true),
    Bet("Bet", 'ב', "𐤁", "𓉔", 'ࠀ', 'ا', 2, "B", "house"),
    Gimel("Gimel", 'ג', "𐤂", "𓃀", 'ࠀ', 'ا', 3, "G", "foot, camel"),
    Dalet("Dalet", 'ד', "𐤃", "𓇯", 'ࠀ', 'ا', 4, "D", "door"),
    Hey("Hey", 'ה',"𐤄", "𓀠", 'ࠀ', 'ا', 5, "H", "jubilation, window"),
    Wav("Wav", 'ו', "𐤅", "𓏲", 'ࠀ', 'ا', 6, "U", "hook", true),
    Zayin("Zayin", 'ז', "𐤆", "𓌻", 'ࠀ', 'ا' , 7, "Z", "weapon"),
    Chet("Chet", 'ח', "𐤇", "𓈈", 'ࠀ', 'ا' , 8, "Ch", "courtyard, gate"),
    Tet("Tet", 'ט', "𐤈", "𐤈", 'ࠀ', 'ا', 9, "T", "wheel"),
    Yod("Yod", 'י', "𐤉", "𓂝", 'ࠀ', 'ا', 10, "Y", "arm, hand", true),
    Kaf("Kaf", 'כ', "𐤊", "𓂩", 'ࠀ', 'ا', 20, "C", "palm of hand", 'ך'),
    Lamed("Lamed", 'ל', "𐤋", "𓏱", 'ࠀ', 'ا' , 30, "L", "goad, staff"),
    Mem("Mem", 'מ', "𐤌", "𓈖", 'ࠀ', 'ا', 40, "M", "water, life", 'ם'),
    Nun("Nun", 'נ', "𐤍", "𓆓", 'ࠀ', 'ا', 50, "N", "fish", 'ן'),
    Samekh("Samekh", 'ס', "𐤎", "𓊽", 'ࠀ', 'ا', 60, "S", "pillar, support"),
    Ayin("Ayin", 'ע',"𐤏", "𓁹", 'ࠀ', 'ا', 70, "I", "eye", true),
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
    public final boolean isVowel;

    AlefBet(String name, char modern, String paleo, String ancient, char samaritan, char arabic,
        int value, String english, String meaning) {
      this(name, modern, paleo, ancient, samaritan, arabic, value, english, meaning, false, null);
    }

    AlefBet(String name, char modern, String paleo, String ancient, char samaritan, char arabic,
        int value, String english, String meaning, boolean isVowel) {
      this(name, modern, paleo, ancient, samaritan, arabic, value, english, meaning, isVowel, null);
    }

    AlefBet(String name, char modern, String paleo, String ancient, char samaritan, char arabic,
        int value, String english, String meaning, Character finalModern) {
      this(name, modern, paleo, ancient, samaritan, arabic, value, english, meaning, false, finalModern);
    }

    AlefBet(String name, char modern, String paleo, String ancient, char samaritan, char arabic,
        int value, String english, String meaning, boolean isVowel, Character finalModern) {
      this.name = name;
      this.modern = modern;
      this.paleo = paleo;
      this.ancient = ancient;
      this.samaritan = samaritan;
      this.arabic = arabic;
      this.value = value;
      this.english = english;
      this.meaning = meaning;
      this.isVowel = isVowel;
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

    public boolean isVowel() {
      return isVowel;
    }
  }

  public static final ImmutableMap<Character, String> PICTOGRAPH_MAP = ImmutableMap.<Character, String>builder()
      .putAll(stream(AlefBet.values()).collect(toImmutableMap(AlefBet::modern, AlefBet::ancient)))
      .putAll(
          stream(AlefBet.values())
              .filter(alefBet -> alefBet.finalModern != null)
              .collect(toImmutableMap(AlefBet::finalModern, AlefBet::ancient)))
      .build();

  public static final ImmutableMap<Character, AlefBet> TRANSLITERATE_MAP = ImmutableMap.<Character, AlefBet>builder()
      .putAll(stream(AlefBet.values()).collect(toImmutableMap(AlefBet::modern, Function.identity())))
      .putAll(
          stream(AlefBet.values())
              .filter(alefBet -> alefBet.finalModern != null)
              .collect(toImmutableMap(AlefBet::finalModern, Function.identity())))
      .build();

  public static String removeGarbage(String text) {
    return text.chars()
        .filter(c -> c != '\u202A' && c != '\u202C' && c != '\u202D' && c != '\u200D')
        .mapToObj(c -> String.valueOf((char) c)).collect(joining());
  }

  public static String removePunctuation(String text) {
    AtomicBoolean foundEnd = new AtomicBoolean();
    return removeGarbage(text).replaceAll("׀ ", "").chars()
        .map(c -> c == '־' ? ' ' : c)
        .peek(c -> {
          if (c == '׃' || c == ':') {
            foundEnd.set(true);
          }
        })
        .filter(c -> c != '׀' && c != '[' && c != ']' && c != '(' && c != ')' && c != '‸'
            && c != ',' && c != '.' && !foundEnd.get())
        .mapToObj(c -> String.valueOf((char) c)).collect(joining()).trim();
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
    if (text.charAt(0) < 'א' || text.charAt(0) > 'ת') {
      return null;
    }

    List<String> ancient = removePunctuation(text).chars().mapToObj(c -> toAncient((char) c)).collect(toList());
    Collections.reverse(ancient);
    return String.join("", ancient);
  }

  public static String transliterate(String word) {
    if (word.length() == 1) {
      AlefBet ab = TRANSLITERATE_MAP.get(word.charAt(0));
      if (ab == null) {
        return word;
      }

      return ab.english.toLowerCase() + (ab == AlefBet.Alef ? "" : "a");
    }

    AtomicReference<AlefBet> prev = new AtomicReference<>();
    return word.chars().mapToObj(c -> {
      AlefBet ab = TRANSLITERATE_MAP.get((char) c);
      if (ab == null) {
        return String.valueOf(c);
      }

      if (prev.get() != null && !ab.isVowel() && !prev.get().isVowel()) {
        prev.set(ab);
        return "a" + ab.english.toLowerCase();
      }

      prev.set(ab);
      return ab.english.toLowerCase();
    }).collect(joining());
  }

  public static String toAncientRtl(StringBuilder text) {
    return toAncientRtl(text.toString());
  }

  public static String toConstantsOnly(String text) {
    return removePunctuation(text).chars()
        .filter(c -> c < 1425 || c > 1479)
        .mapToObj(c -> String.valueOf((char) c)).collect(joining()).trim();
  }

  public static String toFullHebrew(String text) {
    StringBuilder output = new StringBuilder();
    int[] letters = removePunctuation(text).chars().toArray();
    for (int l = 0; l < letters.length; l++) {
      char c = (char) letters[l];
      if ((c == '\u05BB' || c == '\u05B9') && letters[l - 1] != 'ו' && letters[l + 1] != 'ו') {
        output.append('ו');
      } else if (c == '\u05B4' && letters[l - 1] != 'י' && letters[l + 1] != 'י') {
        // output.append('י');
      }

      if ((c < 1425 || c > 1479) && c != '\u202A' && c != '\u202C' && c != '\u200D') {
        output.append(c);
      }
    }
    return output.toString();
  }

  public static String toConstantsOnly(StringBuilder text) {
    return toConstantsOnly(text.toString());
  }

  public static String unfinalize(String text) {
    return stream(text.split(" ")).map(HebrewConverter::unfinalizeWord).collect(joining(" "));
  }

  public static String unfinalizeWord(String word) {
    if (word.length() == 0) {
      return word;
    }
    return
        word.replaceAll("ך", "כ").replaceAll("ם", "מ").replaceAll("ן", "נ").replaceAll("ף", "פ").replaceAll("ץ", "צ");

    /* int lastIndex = word.length() - 1;
    return switch (word.charAt(lastIndex)) {
      case 'ך' -> word.substring(0, lastIndex) + 'כ';
      case 'ם' -> word.substring(0, lastIndex) + 'מ';
      case 'ן' -> word.substring(0, lastIndex) + 'נ';
      case 'ף' -> word.substring(0, lastIndex) + 'פ';
      case 'ץ' -> word.substring(0, lastIndex) + 'צ';
      default -> word;
    }; */
  }

  public static String finalize(String text) {
    return stream(text.split(" ")).map(HebrewConverter::finalizeWord).collect(joining(" "));
  }

  public static String finalizeWord(String word) {
    int lastIndex = word.length() - 1;
    return switch (word.charAt(lastIndex)) {
      case 'כ' -> word.substring(0, lastIndex) + 'ך';
      case 'מ' -> word.substring(0, lastIndex) + 'ם';
      case 'נ' -> word.substring(0, lastIndex) + 'ן';
      case 'פ' -> word.substring(0, lastIndex) + 'ף';
      case 'צ' -> word.substring(0, lastIndex) + 'ץ';
      default -> word;
    };
  }

  public static String toStrongsId(String strongsRef) {
    if (strongsRef == null) return null;

    return switch (strongsRef.length()) {
      case 2 -> strongsRef.charAt(0) + "000" + strongsRef.substring(1);
      case 3 -> strongsRef.charAt(0) + "00" + strongsRef.substring(1);
      case 4 -> strongsRef.charAt(0) + "0" + strongsRef.substring(1);
      default -> strongsRef;
    };
  }
}

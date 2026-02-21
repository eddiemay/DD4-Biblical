package com.digitald4.biblical.util;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import com.digitald4.common.util.FormatText;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class HebrewConverter {
  public enum CharacterType {Consonant, Vowel, Sometimes_Vowel}
  public enum AlefBet {
    Alef("Alef", 'א', "𐤀", "𓃾", 'ࠀ', 'ا', 1, "a", "ox head, strength", CharacterType.Vowel, null),
    Bet("Bet", 'ב', "𐤁", "𓉔", 'ࠀ', 'ا', 2, "b", "house"),
    Gimel("Gimel", 'ג', "𐤂", "𓃀", 'ࠀ', 'ا', 3, "g", "foot, camel"),
    Dalet("Dalet", 'ד', "𐤃", "𓇯", 'ࠀ', 'ا', 4, "d", "door"),
    Hey("Hey", 'ה',"𐤄", "𓀠", 'ࠀ', 'ا', 5, "h", "jubilation, window"),
    Wav("Wav", 'ו', "𐤅", "𓏲", 'ࠀ', 'ا', 6, "w", "hook", CharacterType.Sometimes_Vowel, "u"),
    Zayin("Zayin", 'ז', "𐤆", "𓌻", 'ࠀ', 'ا' , 7, "z", "weapon"),
    Chet("Chet", 'ח', "𐤇", "𓈈", 'ࠀ', 'ا' , 8, "ch", "courtyard, gate"),
    Tet("Tet", 'ט', "𐤈", "𐤈", 'ࠀ', 'ا', 9, "t", "wheel"),
    Yod("Yod", 'י', "𐤉", "𓂝", 'ࠀ', 'ا', 10, "y", "arm, hand", CharacterType.Sometimes_Vowel, "y"),
    Kaf("Kaf", 'כ', "𐤊", "𓂩", 'ࠀ', 'ا', 20, "c", "palm of hand", 'ך'),
    Lamed("Lamed", 'ל', "𐤋", "𓏱", 'ࠀ', 'ا' , 30, "l", "goad, staff"),
    Mem("Mem", 'מ', "𐤌", "𓈖", 'ࠀ', 'ا', 40, "m", "water, life", 'ם'),
    Nun("Nun", 'נ', "𐤍", "𓆓", 'ࠀ', 'ا', 50, "n", "fish", 'ן'),
    Samekh("Samekh", 'ס', "𐤎", "𓊽", 'ࠀ', 'ا', 60, "s", "pillar, support"),
    Ayin("Ayin", 'ע',"𐤏", "𓁹", 'ࠀ', 'ا', 70, "i", "eye", CharacterType.Vowel, null),
    Pay("Pay", 'פ', "𐤐", "𓂋", 'ࠀ', 'ا' , 80, "p", "mouth", 'ף'),
    Tzadi("Tzadi", 'צ', "𐤑", "𓄘", 'ࠀ', 'ا', 90, "tz", "Man on side, desire, need", 'ץ'),
    Qof("Qof", 'ק', "𐤒", "𐤒", 'ࠀ', 'ا', 100, "q", "eye of needle"),
    Resh("Resh", 'ר', "𐤓", "𓁶", 'ࠀ', 'ا' , 200, "r", "man head"),
    Shin("Shin", 'ש', "ש", "𓌓", 'ࠀ', 'ا', 300, "sh", "tooth"),
    Tav("Tav", 'ת', "𐤕", "𓏴", 'ࠀ', 'ا' , 400, "t", "mark, sign");

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
    public final CharacterType characterType;
    private final String vowelSound;

    AlefBet(String name, char modern, String paleo, String ancient, char samaritan, char arabic,
        int value, String english, String meaning) {
      this(name, modern, paleo, ancient, samaritan, arabic, value, english, meaning, null,
          CharacterType.Consonant, null);
    }

    AlefBet(String name, char modern, String paleo, String ancient, char samaritan, char arabic,
        int value, String english, String meaning, CharacterType characterType, String engVowel) {
      this(name, modern, paleo, ancient, samaritan, arabic, value, english, meaning, null, characterType, engVowel);
    }

    AlefBet(String name, char modern, String paleo, String ancient, char samaritan, char arabic,
        int value, String english, String meaning, Character finalModern) {
      this(name, modern, paleo, ancient, samaritan, arabic, value, english, meaning, finalModern,
          CharacterType.Consonant, null);
    }

    AlefBet(String name, char modern, String paleo, String ancient, char samaritan, char arabic, int value,
        String english, String meaning, Character finalModern, CharacterType characterType, String vowelSound) {
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
      this.characterType = characterType;
      this.vowelSound = vowelSound;
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

    public String getVowelSound() {
      return vowelSound != null ? vowelSound : english;
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
        .filter(c -> c != '׀' && c != '[' && c != ']' && c != '(' && c != ')' && c != '‸' && c != ';' && c != '·'
            && c != '፤' && c != ',' && c != '.' && c != '፡' && c != '።' && c != '፥' && c != '፣' && c != '፧' && c != '*'
            && !foundEnd.get())
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
    return toAncient(toFullHebrew(text.toString()));
  }

  public static String toAncientRtl(String text) {
    if (text.charAt(0) < 'א' || text.charAt(0) > 'ת') {
      return null;
    }

    List<String> ancient = removePunctuation(text).chars().mapToObj(c -> toAncient((char) c)).collect(toList());
    Collections.reverse(ancient);
    return String.join("", ancient);
  }

  public static String transliterate(String word, boolean isSuffix) {
    if (word.length() == 1) {
      AlefBet ab = TRANSLITERATE_MAP.get(word.charAt(0));
      if (ab == null) {
        return "";
      }

      return ab.english + (ab.characterType != CharacterType.Vowel ? "a" : "");
    }

    AtomicReference<Boolean> prevIsVowel = new AtomicReference<>();
    return word.chars().mapToObj(c -> {
      AlefBet ab = TRANSLITERATE_MAP.get((char) c);
      if (ab == null) {
        return "";
        // return String.valueOf((char) c);
      }

      // If this is the first letter or the previous letter is producing a vowel sound, return the default constant sound.
      if (prevIsVowel.get() == null || prevIsVowel.get() == Boolean.TRUE || ab.characterType == CharacterType.Vowel) {
        prevIsVowel.set(ab.characterType == CharacterType.Vowel
            || ab.characterType == CharacterType.Sometimes_Vowel && isSuffix);
        return ab.english;
      }

      // If we are here then this is not the first letter and the previous letter was not a vowel.
      if (ab.characterType == CharacterType.Sometimes_Vowel) {
        prevIsVowel.set(true);
        return ab.vowelSound;
      }

      prevIsVowel.set(false);
      return "a" + ab.english;
    }).collect(joining());
  }

  public static String toAncientRtl(StringBuilder text) {
    return toAncientRtl(text.toString());
  }

  public static String toConstantsOnly(String text) {
    return FormatText.removeAccents(removePunctuation(text));
  }

  public static String toConstantsOnly(StringBuilder text) {
    return toConstantsOnly(text.toString());
  }

  public static String toGeezConstants(String text) {
    StringBuilder output = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      // Check if the character is Ethiopia Geez.
      if (c > 0x1200 && c < 0x137F && !(c == 'ማ' && i != 0 && output.charAt(i - 1) == 'ሰ')) {
        // Calculate the base consonant by subtracting the vowel offset
        c -= ((c - 0x1200) % 8);
      }
      output.append(c);
    }
    return output.toString();
  }

  public static String toFullHebrew(String text) {
    StringBuilder output = new StringBuilder();
    int[] letters = removePunctuation(text).chars().toArray();
    for (int l = 0; l < letters.length; l++) {
      int c = letters[l];
      if ((c == 0x05BB || c == 0x05B9) && letters[l - 1] != 'ו' && letters[l + 1] != 'ו') {
        output.append('ו');
      } else if (c == 0x05B4 && letters[l - 1] != 'י' && letters[l + 1] != 'י') {
        // output.append('י');
      }

      if ((c < 1425 || c > 1479) && c != 0x202A && c != 0x202C && c != 0x200D
          // Guard against inserting 2 waws in a row.
          && (c != 'ו' || output.isEmpty() || output.charAt(output.length() - 1) != 'ו')) {
        output.append((char) c);
      }
    }
    return toConstantsOnly(output.toString());
  }

  public static String toFullHebrew(StringBuilder text) {
    return toFullHebrew(text.toString());
  }

  public static String toRestored(String text) {
    return unfinalize(toFullHebrew(text));
  }

  public static String toRestored(StringBuilder text) {
    return toRestored(text.toString());
  }

  public static String unfinalize(String text) {
    return text
        .replaceAll("ך", "כ").replaceAll("ם", "מ").replaceAll("ן", "נ").replaceAll("ף", "פ").replaceAll("ץ", "צ");
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

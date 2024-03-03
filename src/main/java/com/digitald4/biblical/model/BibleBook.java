package com.digitald4.biblical.model;

import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.collect.ImmutableSet;

public class BibleBook {
  public static final String ESTHER = "Esther";
  public static final String Psalms = "Psalms";
  public static final String SONG_OF_SOLOMON = "Song of Solomon";
  public static final String ISAIAH = "Isaiah";
  public static final String ADDITIONS_TO_ESTHER = "Additions to Esther";
  public static final String APOCRYPHAL_PSALMS = "Apocryphal Psalms";
  public static final String SIRACH = "Sirach";
  public static final String WISDOM_OF_SOLOMON = "Wisdom of Solomon";
  public static final String MACCABEES_1 = "1 Maccabees";
  public static final String MACCABEES_2 = "2 Maccabees";
  public static final String TESTAMENTS_OF_THE_TWELVE_PATRIARCHS = "Testaments of the Twelve Patriarchs";
  public static final String SUSANNA = "Susanna";
  public static final String JUBILEES = "Jubilees";
  public static final String ENOCH = "Enoch";
  public static final String ENOCH_2 = "2 Enoch";
  public static final String ENOCH_3 = "3 Enoch";
  public static final String BOOK_OF_ADAM_AND_EVE = "Book of Adam and Eve";
  public static final String JASHER = "Jasher";
  public static final String COMMUNITY_RULE = "Community Rule";
  public static final String WAR_SCROLL = "War Scroll";
  public static final String GIANTS = "Book of Giants";
  public static final String JOSEPHUS = "Josephus";
  public static final String TESTAMENT_OF_JOB = "Testament of Job";
  public static final String GAD_THE_SEER = "Gad the Seer";
  public static final String LIVES_OF_THE_PROPHETS = "Lives of the Prophets";
  public static final String BARUCH_2 = "2 Baruch";
  public static final String CLEMENT_1 = "1 Clement";
  public static final String ODES_OF_PEACE = "Odes of Peace";
  @Deprecated
  public static final String EPISTLE_OF_ARISTEAS = "Epistle of Aristeas";
  public static final String LETTER_OF_ARISTEAS = "Letter of Aristeas";
  public static final String MEGILLAT_ANTIOCHUS = "Megillat Antiochus";
  public static final String PRAYER_OF_MANESSEH = "Prayer of Manasseh";

  // Abbreviations help provided by: https://www.logos.com/bible-book-abbreviations
  // Hebrew names provided by: https://headcoverings-by-devorah.com/HebrewBibleNames.html,
  // and https://www.shalach.org/BibleSearch/booksofthebible.htm

  private String name;
  private int number;
  private String tags;
  private int chapterCount;
  private ImmutableSet<String> altNames;
  private Boolean unreleased;
  private StringBuilder description;

  public String getId() {
    return name;
  }

  public BibleBook setId(String name) {
    this.name = name;
    return this;
  }

  @ApiResourceProperty
  public String name() {
    return name;
  }

  public BibleBook setName(String name) {
    this.name = name;
    return this;
  }

  public int getNumber() {
    return number;
  }

  public BibleBook setNumber(int number) {
    this.number = number;
    return this;
  }

  public String getTags() {
    return tags;
  }

  public BibleBook setTags(String tags) {
    this.tags = tags;
    return this;
  }

  public ImmutableSet<String> tags() {
    return ImmutableSet.copyOf(tags.split(","));
  }

  public int getChapterCount() {
    return chapterCount;
  }

  public BibleBook setChapterCount(int chapterCount) {
    this.chapterCount = chapterCount;
    return this;
  }

  public ImmutableSet<String> getAltNames() {
    return altNames;
  }

  public BibleBook setAltNames(Iterable<String> altNames) {
    this.altNames = ImmutableSet.copyOf(altNames);
    return this;
  }

  public Boolean getUnreleased() {
    return unreleased;
  }

  public BibleBook setUnreleased(Boolean unreleased) {
    this.unreleased = unreleased;
    return this;
  }

  public BibleBook setUnreleased() {
    return setUnreleased(true);
  }

  public StringBuilder getDescription() {
    return description;
  }

  public BibleBook setDescription(StringBuilder description) {
    this.description = description;
    return this;
  }

  public BibleBook setDescription(String description) {
    return setDescription(new StringBuilder(description));
  }

  @Deprecated
  public ImmutableSet<String> getNames() {
    return null;
  }

  @Deprecated
  public BibleBook setNames(Iterable<String> unused) {
    return this;
  }
  
  public static BibleBook of(int number, String name, String tags, int chapters, String... altNames) {
    return new BibleBook()
        .setName(name)
        .setNumber(number)
        .setTags(tags)
        .setChapterCount(chapters)
        .setAltNames(ImmutableSet.copyOf(altNames));
  }

  @Override
  public String toString() {
    return name();
  }
}
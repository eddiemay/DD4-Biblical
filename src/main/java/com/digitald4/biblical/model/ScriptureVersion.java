package com.digitald4.biblical.model;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ScriptureVersion {
  private final String name;
  private final String version;
  private final int versionNum;
  private final ImmutableSet<BibleBook> bibleBooks;
  private final ImmutableSet<String> supportedLanguages;

  public static final ImmutableList<ScriptureVersion> ALL_VERSIONS = ImmutableList.of(
      new ScriptureVersion("The Scriptures (1998)", "ISR", 10, BibleBook.CANON),
      new ScriptureVersion("Restored Names King James", "RSKJ", 20, BibleBook.CANON),
      new ScriptureVersion("New Revised Standard Version", "NRSV", 30,
          ImmutableSet.<BibleBook>builder()
              .addAll(BibleBook.CANON)
              .addAll(BibleBook.APOCRYPHA)
              .add(BibleBook.PSALMS_151)
              .addAll(BibleBook.EASTERN_ORTHODOX_DEUTEROCANON)
              .build()),
      new ScriptureVersion("Oxford", "OXFORD", 40,
          ImmutableSet.of(BibleBook.ENOCH, BibleBook.ENOCH_2, BibleBook.JUBILEES, BibleBook.JASHER,
              BibleBook.BOOK_OF_ADAM_AND_EVE)),
      new ScriptureVersion("New World Translation", "NWT", 50, BibleBook.CANON),
      new ScriptureVersion("Qumran", "qumran", 56,
          ImmutableSet.of(BibleBook.COMMUNITY_RULE, BibleBook.WAR_SCROLL, BibleBook.GIANTS)),
      new ScriptureVersion("Sefaria", "Sefaria", 60,
          ImmutableList.<BibleBook>builder()
              .add(BibleBook.JUBILEES, BibleBook.MACCABEES_1, BibleBook.MACCABEES_2,
                  BibleBook.SUSANNA, BibleBook.TOBIT, BibleBook.TESTAMENTS_OF_THE_TWELVE_PATRIARCHS)
              // .addAll(BibleBook.TESTAMENTS_OF_THE_TWELVE)
              .build(),
          ImmutableSet.of(BibleBook.EN, BibleBook.HEBREW)),
      new ScriptureVersion("Covenant Christian Coalition", "CCC", 61,
          ImmutableSet.of(BibleBook.JOSEPHUS, BibleBook.ENOCH_3, BibleBook.TESTAMENT_OF_JOB,
              BibleBook.GAD_THE_SEER, BibleBook.LIVES_OF_THE_PROPHETS)),
      new ScriptureVersion("King James 1611", "KJV1611", 70,
          ImmutableSet.<BibleBook>builder().addAll(BibleBook.CANON)
              .addAll(BibleBook.APOCRYPHA).add(BibleBook.ADDITIONS_TO_ESTHER).build()),
      new ScriptureVersion("Westminster Leningrad Codex - Consonants Only", "WLCO", 80,
          BibleBook.CANON.subList(0, 39), ImmutableSet.of(BibleBook.HEBREW)));

  private static final ImmutableMap<String, ScriptureVersion> BY_VERSION =
      ALL_VERSIONS.stream().collect(toImmutableMap(ScriptureVersion::getVersion, identity()));

  public ScriptureVersion(String name, String version, int number, Iterable<BibleBook> bibleBooks) {
    this(name, version, number, bibleBooks, ImmutableSet.of(BibleBook.EN));
  }

  public ScriptureVersion(String name, String version, int versionNum,
      Iterable<BibleBook> bibleBooks, Iterable<String> supportedLanguages) {
    this.name = name;
    this.version = version;
    this.versionNum = versionNum;
    this.bibleBooks = ImmutableSet.copyOf(bibleBooks);
    this.supportedLanguages = ImmutableSet.copyOf(supportedLanguages);
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public int getVersionNum() {
    return versionNum;
  }

  public ImmutableSet<BibleBook> getBibleBooks() {
    return bibleBooks;
  }

  public ImmutableSet<String> getSupportedLanguages() {
    return supportedLanguages;
  }

  public boolean meetsCriteria(BibleBook book, String lang) {
    return getBibleBooks().contains(book) && (lang == null || supportedLanguages.contains(lang));
  }

  @Override
  public String toString() {
    return getVersion();
  }

  public static ScriptureVersion get(String version) {
    // We will support all the different versions of BibleHub without having to create instances for them.
    return BY_VERSION.get(version); // OrDefault(version, new ScriptureVersion(version, version, BibleBook.CANON));
  }

  public static ScriptureVersion getOrFallback(String version, String lang, BibleBook book) {
    ScriptureVersion scriptureVersion = get(version);
    if (scriptureVersion == null) {
      throw new DD4StorageException("Unknown scripture version: " + version, ErrorCode.BAD_REQUEST);
    }

    if (scriptureVersion.meetsCriteria(book, lang)) {
      return scriptureVersion;
    }

    return BY_VERSION.values().stream().filter(sv -> sv.meetsCriteria(book, lang)).findFirst()
        .orElseThrow(() ->
             new DD4StorageException("No source found for book: " + book + " in language: " + lang));
  }
}

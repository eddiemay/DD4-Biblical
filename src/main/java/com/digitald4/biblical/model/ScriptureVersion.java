package com.digitald4.biblical.model;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.digitald4.common.exception.DD4StorageException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ScriptureVersion {
  private final String name;
  private final String version;
  private final int versionNum;
  private final ImmutableSet<BibleBook> bibleBooks;

  private static final ImmutableList<ScriptureVersion> ALL_VERSIONS = ImmutableList.of(
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
          ImmutableSet.of(BibleBook.ENOCH, BibleBook.JUBILEES, BibleBook.JASHER, BibleBook.BOOK_OF_ADAM_AND_EVE)),
      new ScriptureVersion("Enoch Reference", "EnochRef", 35, ImmutableSet.of(BibleBook.ENOCH)),
      new ScriptureVersion("New World Translation", "NWT", 50, BibleBook.CANON),
      new ScriptureVersion("Essene", "essene", 55, ImmutableSet.of(BibleBook.COMMUNITY_RULE)),
      new ScriptureVersion("Qumran", "qumran", 56, ImmutableSet.of(BibleBook.WAR_SCROLL)),
      new ScriptureVersion("University of Chicago", "uchicago", 57, ImmutableSet.of(BibleBook.JOSEPHUS)),
      new ScriptureVersion("King James 1611", "KJV1611", 70,
          ImmutableSet.<BibleBook>builder()
              .addAll(BibleBook.CANON).addAll(BibleBook.APOCRYPHA).add(BibleBook.ADDITIONS_TO_ESTHER).build()));

  private static final ImmutableMap<String, ScriptureVersion> BY_VERSION =
      ALL_VERSIONS.stream().collect(toImmutableMap(ScriptureVersion::getVersion, identity()));

  public ScriptureVersion(String name, String version, int versionNum, Iterable<BibleBook> bibleBooks) {
    this.name = name;
    this.version = version;
    this.versionNum = versionNum;
    this.bibleBooks = ImmutableSet.copyOf(bibleBooks);
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

  public static ScriptureVersion get(String version) {
    // We will support all the different versions of BibleHub without having to create instances for them.
    return BY_VERSION.get(version); // OrDefault(version, new ScriptureVersion(version, version, BibleBook.CANON));
  }

  public static ScriptureVersion getOrFallback(String version, BibleBook book) {
    ScriptureVersion scriptureVersion = get(version);
    if (scriptureVersion.getBibleBooks().contains(book)) {
      return scriptureVersion;
    }

    return BY_VERSION.values().stream().filter(sv -> sv.getBibleBooks().contains(book)).findFirst()
        .orElseThrow(() -> new DD4StorageException("No source found for book: " + book));
  }
}

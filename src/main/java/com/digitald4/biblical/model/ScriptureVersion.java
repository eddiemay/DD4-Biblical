package com.digitald4.biblical.model;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;

import com.digitald4.biblical.util.Language;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ScriptureVersion {
  @Deprecated public static final String INTERLINEAR = "Interlinear";
  private final String name;
  private final String version;
  private final int versionNum;
  private final ImmutableSet<String> tags;
  private final ImmutableSet<String> supportedLanguages;

  public static final ImmutableList<ScriptureVersion> ALL_VERSIONS = ImmutableList.of(
      new ScriptureVersion("The Scriptures (1998)", "ISR", 10, "Canon"),
      new ScriptureVersion("Restored Names King James", "RSKJ", 20, "Canon"),
      new ScriptureVersion("New Revised Standard Version", "NRSV", 30,
          ImmutableSet.of("Canon", "OT Additions", "Deuterocanon", "Eastern")),
      new ScriptureVersion("Oxford", "OXFORD", 40,
          ImmutableSet.of("Ethiopian", "2 Enoch", "Jubilees", "Jasher", "Book of Adam and Eve")),
      new ScriptureVersion("New World Translation", "NWT", 50, "Canon"),
      new ScriptureVersion("Sefaria", "Sefaria", 60, ImmutableSet.of("Aristeas", "Jubilees", "Judith", "1 Macc",
          "2 Macc", "Manasseh", "Megillat Antiochus", "Sirach", "Susanna", "Tobit", "TofT", "Wisdom of Solomon"),
          Language.EN, Language.HEBREW),
      new ScriptureVersion("Covenant Christian Coalition", "CCC", 61,
          ImmutableSet.of("Additional Apocrypha", "New Testament Apocrypha")),
      new ScriptureVersion("King James 1611", "KJV1611", 70,
          ImmutableSet.of("Canon", "OT Additions", "Deuterocanon")),
      new ScriptureVersion("Brenton's Septuagint", "SEP", 75, "OT"), // Support for Septuagint.
      new ScriptureVersion("Westminster Leningrad Codex", "WLC", 80, ImmutableSet.of("OT"),
          Language.HEBREW),
      new ScriptureVersion("Nestle 104 GNT", "Nestle", 81, ImmutableSet.of("NT"), Language.GK),
      new ScriptureVersion("Dead Sea Scrolls", "DSS", 82,
          ImmutableSet.of("Community Rule", "War Scroll", "Book of Giants", BibleBook.ISAIAH),
          Language.EN, Language.HEBREW),
      new ScriptureVersion("Open Siddur", "SID", 85, ImmutableSet.of("Jubilees"), Language.GEEZ),
      new ScriptureVersion("Geez Experience", "GzExp", 86, ImmutableSet.of("Canon"), Language.GEEZ),
      new ScriptureVersion(INTERLINEAR, INTERLINEAR, 90, "Canon"));

  public static final ImmutableMap<String, ScriptureVersion> BY_VERSION =
      ALL_VERSIONS.stream().collect(toImmutableMap(ScriptureVersion::getVersion, identity()));

  public ScriptureVersion(String name, String version, int number, String... tag) {
    this(name, version, number, ImmutableSet.copyOf(tag));
  }

  public ScriptureVersion(String name, String version, int versionNum, Iterable<String> tags,
      String... supportedLanguages) {
    this.name = name;
    this.version = version;
    this.versionNum = versionNum;
    this.tags = ImmutableSet.copyOf(tags);
    this.supportedLanguages = supportedLanguages.length > 0
        ? ImmutableSet.copyOf(supportedLanguages) : ImmutableSet.of(Language.EN);
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

  public ImmutableSet<String> getTags() {
    return tags;
  }

  public ImmutableSet<String> getSupportedLanguages() {
    return supportedLanguages;
  }


  @Override
  public String toString() {
    return getVersion();
  }

  public static ScriptureVersion get(String version) {
    // We will support all the different versions of BibleHub without having to create instances for them.
    return BY_VERSION.get(version); // OrDefault(version, new ScriptureVersion(version, version, "Canon"));
  }
}

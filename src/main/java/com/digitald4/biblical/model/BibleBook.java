package com.digitald4.biblical.model;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.util.Pair;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

public class BibleBook {
  public static final String EN = "en";
  public static final String HEBREW = "he";
  public static final String HEBREW_ANCIENT = "he-A";
  public static final String INTERLACED = "interlaced";

  public static final BibleBook Genesis;
  public static final BibleBook Kings2;
  public static final BibleBook ESTHER;
  public static final BibleBook Psalms;
  public static final BibleBook SONG_OF_SOLOMON;
  public static final BibleBook Jude;

  // Abbreviations help provided by: https://www.logos.com/bible-book-abbreviations
  // Hebrew names provided by: https://headcoverings-by-devorah.com/HebrewBibleNames.html,
  // and https://www.shalach.org/BibleSearch/booksofthebible.htm
  // $221 3.29, 2.84, $221
  public static final ImmutableList<BibleBook> CANON = ImmutableList.of(
      Genesis = BibleBook.of(1, "Genesis", "Canon,OT,Pentateuch", 50, "Gen", "Ge", "Gn", "Bereishit", "Beresheet"),
      BibleBook.of(2, "Exodus", "Canon,OT,Pentateuch", 40, "Ex", "Exo", "Exod", "Shemot"),
      BibleBook.of(3, "Leviticus", "Canon,OT,Pentateuch", 27, "Lev", "Le", "Lv", "Vayikra", "Vyakra"),
      BibleBook.of(4, "Numbers", "Canon,OT,Pentateuch", 36, "Num", "Nu", "Nm", "Nb", "Bamidbar", "Bamidvar"),
      BibleBook.of(5, "Deuteronomy", "Canon,OT,Pentateuch", 34,"Deut", "De", "Dt", "Devarim"),
      
      BibleBook.of(6, "Joshua", "Canon,OT,Historical", 24,
          "Josh", "Jos", "Jsh", "Yehoshua", "Yahosha", "Yahoshai", "Yahoshi", "Yahusha", "Yahushai", "Yahushi"),
      BibleBook.of(7, "Judges", "Canon,OT,Historical",21, "Judg", "Jdg", "Jg", "Jdgs", "Shoftim"),
      BibleBook.of(8, "Ruth", "Canon,OT,Historical", 4, "Ru", "Rth", "Rut"),
      BibleBook.of(9, "1 Samuel", "Canon,OT,Historical,Samuel,Sam", 31, "1Sam", "1Sa", "1Sm", "1 Shamuel", "1 Shmuel"),
      BibleBook.of(10, "2 Samuel", "Canon,OT,Historical,Samuel,Sam", 24,"2Sam", "2Sa", "2Sm", "2 Shemuel", "2 Shamuel"),
      BibleBook.of(11, "1 Kings", "Canon,OT,Historical,Kings", 22,"1Ki", "1 Kgs", "1 Malachim"),
      Kings2 = BibleBook.of(12, "2 Kings", "Canon,OT,Historical,Kings", 25,"2Ki", "2 Kgs", "2 Malachim"),
      BibleBook.of(13, "1 Chronicles", "Canon,OT,Historical,Chronicles,Chr", 29, "1Ch", "1Chr", "1 Divrei HaYamim"),
      BibleBook.of(14, "2 Chronicles", "Canon,OT,Historical,Chronicles,Chr", 36, "2Ch", "2Chr", "2 Divrei HaYamim"),
      BibleBook.of(15, "Ezra", "Canon,OT,Historical", 10, "Ezr", "Ez"),
      BibleBook.of(16, "Nehemiah", "Canon,OT,Historical", 13, "Neh", "Ne", "Nechemya"),
      ESTHER = BibleBook.of(17, "Esther", "Canon,OT,Historical", 16, "Est", "Esth", "Es", "Hadassah"),
      
      BibleBook.of(18, "Job", "Canon,OT,Wisdom", 42, "Jb", "Iyov"),
      Psalms = BibleBook.of(19, "Psalms", "Canon,OT,Wisdom", 155, "Ps", "Psalm", "Psa", "Tehillim", "Tikkan"), // Includes the 151 Psalm
      BibleBook.of(20, "Proverbs", "Canon,OT,Wisdom", 31, "Prov", "Pro", "Prv", "Pr", "Mishlei"),
      BibleBook.of(21, "Ecclesiastes", "Canon,OT,Wisdom", 12, "Ecc", "Ec", "kohelet"),
      SONG_OF_SOLOMON = BibleBook.of(22, "Song of Solomon", "Canon,OT,Wisdom", 8, "songs", "song", "sos", "ca", "Shir HaShirim"),
      
      BibleBook.of(23, "Isaiah", "Canon,OT,Prophets", 66, "Isa", "Is", "Yeshayahu", "Yashayahu", "Yashaiahu"),
      BibleBook.of(24, "Jeremiah", "Canon,OT,Prophets", 52, "Jer", "Je", "Yirmeyahu", "Yirmiyah"),
      BibleBook.of(25, "Lamentations", "Canon,OT,Prophets", 5, "Lam", "La", "Eicha"),
      BibleBook.of(26, "Ezekiel", "Canon,OT,Prophets", 48, "Ezek", "Eze", "Ezk", "Yechezkel"),

      BibleBook.of(27, "Daniel", "Canon,OT,Prophets", 12, "Dan", "Da", "Dn"), // Includes "Susanna and the Elders" (Ch. 13) and "Bel and the Dragon" (Ch. 14)

      BibleBook.of(28, "Hosea", "Canon,OT,Prophets", 14, "Hos", "Ho", "Hoshea"),
      BibleBook.of(29, "Joel", "Canon,OT,Prophets", 3, "Joe", "Jl", "Yoel"),
      BibleBook.of(30, "Amos", "Canon,OT,Prophets", 9, "Am"),
      BibleBook.of(31, "Obadiah", "Canon,OT,Prophets", 1, "Obad", "Ob", "Oba", "Ovadya"),
      BibleBook.of(32, "Jonah", "Canon,OT,Prophets", 4, "Jon", "Jo", "Yonah"),
      BibleBook.of(33, "Micah", "Canon,OT,Prophets", 7, "Mic", "Mc", "Micha"),
      BibleBook.of(34, "Nahum", "Canon,OT,Prophets", 3, "Nah", "Nachum"),
      BibleBook.of(35, "Habakkuk", "Canon,OT,Prophets", 3, "Hab", "Hb", "Chavakkuk"),
      BibleBook.of(36, "Zephaniah", "Canon,OT,Prophets", 3, "Zeph", "Zep", "Zp", "Tzefanya"),
      BibleBook.of(37, "Haggai", "Canon,OT,Prophets", 2, "Hag", "Hg", "Chaggai"),
      BibleBook.of(38, "Zechariah", "Canon,OT,Prophets", 14, "Zech", "Zec", "Zc", "Zecharya"),
      BibleBook.of(39, "Malachi", "Canon,OT,Prophets", 4, "Mal", "ML"),

      BibleBook.of(40, "Matthew", "Canon,NT,Gospel", 28, "Matt", "Mt", "Matityah"),
      BibleBook.of(41, "Mark", "Canon,NT,Gospel", 16, "Mar", "Mr", "Makabi"),
      BibleBook.of(42, "Luke", "Canon,NT,Gospel", 24, "Luk", "Lu", "Ur"),
      BibleBook.of(43, "John", "Canon,NT,Gospel", 21, "Joh", "Jn", "Yochanan", "Yohanan"),
      
      BibleBook.of(44, "Acts", "Canon,NT,Apostles", 28, "Act", "Ac", "Pyilut Hashaliachim"),
      
      BibleBook.of(45, "Romans", "Canon,NT,Letters", 16, "Rom", "Ro", "Rm"),
      BibleBook.of(46, "1 Corinthians", "Canon,NT,Letters,Corinthians,Cor", 16, "1Co", "1Cor"),
      BibleBook.of(47, "2 Corinthians", "Canon,NT,Letters,Corinthians,Cor", 13, "2Co", "2Cor"),
      BibleBook.of(48, "Galatians", "Canon,NT,Letters", 6, "Ga", "Gal"),
      BibleBook.of(49, "Ephesians", "Canon,NT,Letters", 6, "Eph", "Ep", "Ephes"),
      BibleBook.of(50, "Philippians", "Canon,NT,Letters", 4, "Phil", "Php", "Pp"),
      BibleBook.of(51, "Colossians", "Canon,NT,Letters", 4, "Col", "Co"),
      BibleBook.of(52, "1 Thessalonians", "Canon,NT,Letters,Thessalonians,Thes", 5, "1Th", "1Thes"),
      BibleBook.of(53, "2 Thessalonians", "Canon,NT,Letters,Thessalonians,Thes", 3, "2Th", "2Thes"),
      BibleBook.of(54, "1 Timothy", "Canon,NT,Letters,Timothy,Tim", 6, "1Ti", "1Tim"),
      BibleBook.of(55, "2 Timothy", "Canon,NT,Letters,Timothy,Tim", 4, "2Ti", "2Tim"),
      BibleBook.of(56, "Titus", "Canon,NT,Letters", 3, "Tit", "Ti"),
      BibleBook.of(57, "Philemon", "Canon,NT,Letters", 1, "Phm", "Ph"),

      BibleBook.of(58, "Hebrews", "Canon,NT,Letters", 13, "Heb", "He", "Yehudim", "Mashiakim Yehudim\t"),
      BibleBook.of(59, "James", "Canon,NT,Letters", 5, "Jas", "Jm", "Ya'akov"),
      BibleBook.of(60, "1 Peter", "Canon,NT,Letters,Peter", 5, "1Pe", "1Pet", "1 Kefa"),
      BibleBook.of(61, "2 Peter", "Canon,NT,Letters,Peter", 3, "2Pe", "2Pet", "2 Kefa"),
      BibleBook.of(62, "1 John", "Canon,NT,Letters.John", 5, "1Jo", "1Jn", "1 Yochanan"),
      BibleBook.of(63, "2 John", "Canon,NT,Letters,John", 1, "2Jo", "2Jn", "2 Yochanan"),
      BibleBook.of(64, "3 John", "Canon,NT,Letters,John", 1, "3Jo", "3Jn", "3 Yochanan"),
      Jude = BibleBook.of(65, "Jude", "Canon,NT,Letters", 1, "Jud", "Jd", "Yahudah"),
      BibleBook.of(66, "Revelation", "Canon,NT,Prophecy", 22, "Rev", "Re", "Revelations", "Hagilu Natan Elohim\t"));

  public static final BibleBook ESDRAS_1 = BibleBook.of(67, "1 Esdras", "Apocrypha,Deuterocanon,Eastern,Esdras,Ezra", 9);
  public static final BibleBook ESDRAS_2 = BibleBook.of(68, "2 Esdras", "Apocrypha,Deuterocanon,Ethiopian,Esdras,Ezra", 16);
  public static final BibleBook WISDOM_OF_SOLOMON;
  public static final BibleBook SIRACH;
  public static final BibleBook MACCABEES_1;
  public static final BibleBook MACCABEES_2;
  public static final BibleBook TOBIT;
  public static final ImmutableList<BibleBook> UNIVERSAL_DEUTEROCANON = ImmutableList.of(
      TOBIT = BibleBook.of(69, "Tobit", "Apocrypha,Deuterocanon,Universal", 14, "Tob", "Tb"),
      BibleBook.of(70, "Judith", "Apocrypha,Deuterocanon,Universal", 16, "Jth", "Jdth", "Jdt"),
      WISDOM_OF_SOLOMON = BibleBook.of(71, "Wisdom of Solomon", "Apocrypha,Deutzerocanon,Universal", 19, "Wisdom", "Wis", "Ws"),
      SIRACH = BibleBook.of(72, "Sirach", "Apocrypha,Deuterocanon,Universal", 51, "Ecclesiasticus", "Sir", "Ecclus"),
      BibleBook.of(73, "Baruch", "Apocrypha,Deuterocanon,Universal", 5, "Bar"),
      MACCABEES_1 = BibleBook.of(74, "1 Maccabees", "Apocrypha,Deuterocanon,Universal,Maccabees,Mac,Macc", 16,
          "1Mac", "1Macc", "1 Machabees"),
      MACCABEES_2 = BibleBook.of(75, "2 Maccabees", "Apocrypha,Deuterocanon,Universal,Maccabees,Mac,Macc", 15,
          "2Mac", "2Macc", "2 Machabees"));

  public static final BibleBook PRAYER_OF_MANASSEH;
  public static final BibleBook SUSANNA;
  public static final ImmutableList<BibleBook> APOCRYPHA = ImmutableList.<BibleBook>builder()
      .add(ESDRAS_1, ESDRAS_2)
      .addAll(UNIVERSAL_DEUTEROCANON)
      .add(
          BibleBook.of(76, "Letter of Jeremiah", "Apocrypha", 1, "LJe"),
          BibleBook.of(77, "Prayer of Azariah", "Apocrypha", 1, "Song of the Three",
              "Song of the Three Holy Children", "Song of the Three Children", "PrA", "azriah"),
          SUSANNA = BibleBook.of(78, "Susanna", "Apocrypha", 1, "Sus"),
          BibleBook.of(79, "Bel and the Dragon", "Apocrypha", 1, "Bel"),
          PRAYER_OF_MANASSEH = BibleBook.of(80, "Prayer of Manasseh",
              "Apocrypha,Deuterocanon,Eastern", 1, "PrM", "PMa"))
      .build();

  public static final BibleBook ADDITIONS_TO_ESTHER = BibleBook.of(81, "Additions to Esther",
      "Apocrypha,Esther", 16, "Add Esther", "Add Est", "Add Esth", "Add Es");
  public static final BibleBook APOCRYPHAL_PSALMS = BibleBook.of(82, "Apocryphal Psalms",
      "Apocrypha,Psalms,Psa", 5, "Add Ps", "Add Psa");

  public static final ImmutableList<BibleBook> EASTERN_ORTHODOX_DEUTEROCANON = ImmutableList.of(
      ESDRAS_1,
      BibleBook.of(83, "3 Maccabees",
          "Apocrypha,Deuterocanon,Eastern,Machabees,Mac,Macc", 7, "3Mac", "3Macc", "3 Machabees"),
      BibleBook.of(84, "4 Maccabees",
          "Apocrypha,Deuterocanon,Eastern,Machabees,Mac,Macc", 18, "4Mac", "4Macc", "4 Machabees"),
      PRAYER_OF_MANASSEH);

  public static final BibleBook ENOCH =
      BibleBook.of(85, "Enoch", "Apocrypha,Deuterocanon,Ethiopian", 108, "Hanok", "Eno");
  public static final BibleBook ENOCH_2 =
      BibleBook.of(86, "2 Enoch", "Apocrypha,Enoch", 68, "2 Hanok", "2 Eno");
  public static final BibleBook ENOCH_3 =
      BibleBook.of(87, "3 Enoch", "Apocrypha,Enoch", 48, "3 Hanok", "3 Eno");
  public static final BibleBook GIANTS =
      BibleBook.of(88, "Book of Giants", "Apocrypha,Enoch", 14, "Giants");
  public static final BibleBook JUBILEES =
      BibleBook.of(89, "Jubilees", "Apocrypha,Deuterocanon,Ethiopian", 50, "Jub");
  public static final ImmutableList<BibleBook> ETHIOPIAN_ORTHODOX_DEUTEROCANON =
      ImmutableList.of(ESDRAS_2, ENOCH, JUBILEES);

  public static final BibleBook JASHER =
      BibleBook.of(90, "Jasher", "Apocrypha,Jewish History", 91, "Yasher", "Yahusher");
  public static final BibleBook COMMUNITY_RULE = BibleBook.of(91, "Community Rule",
      "Apocrypha,Jewish History", 1, "cr", "Manual of Discipline", "MD");
  public static final BibleBook WAR_SCROLL =
      BibleBook.of(92, "War Scroll", "Apocrypha,Jewish History", 19, "War");
  public static final BibleBook JOSEPHUS =
      BibleBook.of(93, "Josephus", "Apocrypha,Jewish History", 20);
  public static final BibleBook BOOK_OF_ADAM_AND_EVE =
      BibleBook.of(94, "Book of Adam and Eve", "Apocrypha,Jewish History", 51);
  public static final BibleBook TESTAMENT_OF_JOB;
  public static final BibleBook GAD_THE_SEER;
  public static final BibleBook LIVES_OF_THE_PROPHETS;

  public static final BibleBook TESTAMENTS_OF_THE_TWELVE_PATRIARCHS =
      BibleBook.of(103, "Testaments of the Twelve Patriarchs", "Apocrypha,Jewish History", 12,
          "Testaments of the Twelve", "Testaments", "TofT", "ttp");
  public static final BibleBook BARUCH_2;
  public static final ImmutableList<BibleBook> ADDITIONAL_APOCRYPHA = ImmutableList.of(
      BibleBook.of(95, "Book of Creation", "Apocrypha,Jewish History", 6),
      BibleBook.of(96, "Testament of Abraham", "Apocrypha,Jewish History", 20),
      BibleBook.of(97, "Revelation of Abraham", "Apocrypha,Jewish History", 32),
      BibleBook.of(98, "Testament of Isaac", "Apocrypha,Jewish History", 13),
      BibleBook.of(99, "Testament of Jacob", "Apocrypha,Jewish History", 8),
      BibleBook.of(100, "Ladder of Jacob", "Apocrypha,Jewish History", 8, "Jacob's Ladder"),
      BibleBook.of(101, "Joseph and Asenath", "Apocrypha,Jewish History", 29),
      TESTAMENT_OF_JOB = BibleBook.of(102, "Testament of Job", "Apocrypha,Jewish History", 12),
      BibleBook.of(104, "Testament of Moses", "Apocrypha,Jewish History", 12),
      BibleBook.of(105, "Testament of Solomon", "Apocrypha,Jewish History", 28),
      BibleBook.of(106, "Psalms of Solomon", "Apocrypha,Jewish History", 18),
      LIVES_OF_THE_PROPHETS =
          BibleBook.of(107, "Lives of the Prophets", "Apocrypha,Jewish History", 14, "Prophets"),
      GAD_THE_SEER = BibleBook.of(108, "Gad the Seer", "Apocrypha,Jewish History", 14),
      BibleBook.of(109, "Ascension of Isaiah", "Apocrypha,Jewish History", 11),
      BARUCH_2 = BibleBook.of(110, "2 Baruch", "Apocrypha,Jewish History", 87),
      BibleBook.of(111, "3 Baruch", "Apocrypha,Jewish History", 17),
      BibleBook.of(112, "4 Baruch", "Apocrypha,Jewish History", 9),
      BibleBook.of(113, "Revelation of EliJah", "Apocrypha,Jewish History", 5, "Revelation of Eliyah"),
      BibleBook.of(114, "Revelation of Zephaniah", "Apocrypha,Jewish History", 3),
      BibleBook.of(115, "Apocryphon of Ezekiel", "Apocrypha,Jewish History", 6),
      BibleBook.of(116, "Epistle of Aristeas", "Apocrypha,Jewish History", 32));

  public static final BibleBook CLEMENT_1;
  public static final BibleBook ODES_OF_PEACE;
  public static final ImmutableList<BibleBook> NEW_TESTAMENT_APOCRYPHA = ImmutableList.of(
      BibleBook.of(117, "Didache", "New Testament Apocrypha", 16),
      BibleBook.of(118, "Revelation of Peter", "New Testament Apocrypha", 4),
      BibleBook.of(119, "Epistle of Barnabas", "New Testament Apocrypha", 21),
      BibleBook.of(
          120, "3 Corinthians", "New Testament Apocrypha,Letters,Corinthians,Cor", 3, "3Co", "3Cor"),
      CLEMENT_1 = BibleBook.of(121, "1 Clement", "New Testament Apocrypha", 65, "1 Clem"),
      BibleBook.of(122, "2 Clement", "New Testament Apocrypha", 20, "2 Clem"),
      BibleBook.of(123, "Seven Epistles of Ignatius", "New Testament Apocrypha,Letters", 7).setUnreleased(),
      BibleBook.of(124, "Epistle of Polycarp to the Phillippians", "New Testament Apocrypha,Letters", 14),
      BibleBook.of(125, "Martyrdom of Polycarp", "New Testament Apocrypha", 22).setUnreleased(),
      BibleBook.of(126, "Epistle of Mahetes to Diognetus", "New Testament Apocrypha,Letters", 12),
      BibleBook.of(127, "The Shepherd of Hermas", "New Testament Apocrypha", 27).setUnreleased(),
      ODES_OF_PEACE = BibleBook.of(128, "Odes of Peace", "New Testament Apocrypha", 42, "Odes"),
      BibleBook.of(129, "Apology of Aristides", "New Testament Apocrypha", 17).setUnreleased());

  public static final ImmutableSet<BibleBook> ALL_BOOKS = ImmutableSet.<BibleBook>builder()
      .addAll(CANON)
      .addAll(APOCRYPHA)
      .add(ADDITIONS_TO_ESTHER, APOCRYPHAL_PSALMS)
      .addAll(EASTERN_ORTHODOX_DEUTEROCANON)
      .addAll(ETHIOPIAN_ORTHODOX_DEUTEROCANON)
      .add(ENOCH_2, ENOCH_3, GIANTS, JASHER, COMMUNITY_RULE, WAR_SCROLL, JOSEPHUS,
          BOOK_OF_ADAM_AND_EVE, TESTAMENTS_OF_THE_TWELVE_PATRIARCHS)
      .addAll(ADDITIONAL_APOCRYPHA)
      .addAll(NEW_TESTAMENT_APOCRYPHA)
      .build();

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

  private static final ImmutableMap<String, BibleBook> BY_NAME = ALL_BOOKS.stream()
      .flatMap(book ->
        ImmutableSet.<String>builder().add(book.name()).addAll(book.getAltNames()).build().stream()
            .map(name -> Pair.of(collapseName(name), book)))
      .collect(toImmutableMap(Pair::getLeft, Pair::getRight));

  public static BibleBook get(String name) {
    BibleBook book = BY_NAME.get(collapseName(name));
    if (book == null) {
      throw new DD4StorageException("Unknown bible book: " + name, DD4StorageException.ErrorCode.BAD_REQUEST);
    }

    return book;
  }

  private static String collapseName(String name) {
    return name.toLowerCase().replace(" ", "");
  }

  public static BibleBook get(String name, int chapter) {
    BibleBook book = get(name);

    if (book == BibleBook.ESTHER && chapter > 10) {
      return BibleBook.ADDITIONS_TO_ESTHER;
    } else if (book == BibleBook.ADDITIONS_TO_ESTHER && chapter < 10) {
      return BibleBook.ESTHER;
    }

    if (book == BibleBook.Psalms && chapter > 150) {
      return BibleBook.APOCRYPHAL_PSALMS;
    }

    return book;
  }

  @Override
  public String toString() {
    return name();
  }
}

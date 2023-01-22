package com.digitald4.biblical.model;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.util.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class BibleBook {
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
      Genesis = new BibleBook(1, "Genesis", "Canon,OT,Pentateuch", 50, "Gen", "Ge", "Gn", "Bereishit", "Beresheet"),
      new BibleBook(2, "Exodus", "Canon,OT,Pentateuch", 40, "Ex", "Exo", "Exod", "Shemot"),
      new BibleBook(3, "Leviticus", "Canon,OT,Pentateuch", 27, "Lev", "Le", "Lv", "Vayikra", "Vyakra"),
      new BibleBook(4, "Numbers", "Canon,OT,Pentateuch", 36, "Num", "Nu", "Nm", "Nb", "Bamidbar", "Bamidvar"),
      new BibleBook(5, "Deuteronomy", "Canon,OT,Pentateuch", 34,"Deut", "De", "Dt", "Devarim"),
      
      new BibleBook(6, "Joshua", "Canon,OT,Historical", 24,
          "Josh", "Jos", "Jsh", "Yehoshua", "Yahosha", "Yahoshai", "Yahoshi", "Yahusha", "Yahushai", "Yahushi"),
      new BibleBook(7, "Judges", "Canon,OT,Historical",21, "Judg", "Jdg", "Jg", "Jdgs", "Shoftim"),
      new BibleBook(8, "Ruth", "Canon,OT,Historical", 4, "Ru", "Rth", "Rut"),
      new BibleBook(9, "1 Samuel", "Canon,OT,Historical,Samuel,Sam", 31, "1Sam", "1Sa", "1Sm", "1 Shamuel", "1 Shmuel"),
      new BibleBook(10, "2 Samuel", "Canon,OT,Historical,Samuel,Sam", 24,"2Sam", "2Sa", "2Sm", "2 Shemuel", "2 Shamuel"),
      new BibleBook(11, "1 Kings", "Canon,OT,Historical,Kings", 22,"1Ki", "1 Kgs", "1 Malachim"),
      Kings2 = new BibleBook(12, "2 Kings", "Canon,OT,Historical,Kings", 25,"2Ki", "2 Kgs", "2 Malachim"),
      new BibleBook(13, "1 Chronicles", "Canon,OT,Historical,Chronicles,Chr", 29, "1Ch", "1Chr", "1 Divrei HaYamim"),
      new BibleBook(14, "2 Chronicles", "Canon,OT,Historical,Chronicles,Chr", 36, "2Ch", "2Chr", "2 Divrei HaYamim"),
      new BibleBook(15, "Ezra", "Canon,OT,Historical", 10, "Ezr", "Ez"),
      new BibleBook(16, "Nehemiah", "Canon,OT,Historical", 13, "Neh", "Ne", "Nechemya"),
      ESTHER = new BibleBook(17, "Esther", "Canon,OT,Historical", 16, "Est", "Esth", "Es", "Hadassah"),
      
      new BibleBook(18, "Job", "Canon,OT,Wisdom", 42, "Jb", "Iyov"),
      Psalms = new BibleBook(19, "Psalms", "Canon,OT,Wisdom", 151, "Ps", "Psalm", "Psa", "Tehillim", "Tikkan"), // Includes the 151 Psalm
      new BibleBook(20, "Proverbs", "Canon,OT,Wisdom", 31, "Prov", "Pro", "Prv", "Pr", "Mishlei"),
      new BibleBook(21, "Ecclesiastes", "Canon,OT,Wisdom", 12, "Ecc", "Ec", "kohelet"),
      SONG_OF_SOLOMON = new BibleBook(22, "Song of Solomon", "Canon,OT,Wisdom", 8, "songs", "song", "sos", "ca", "Shir HaShirim"),
      
      new BibleBook(23, "Isaiah", "Canon,OT,Prophets", 66, "Isa", "Is", "Yeshayahu", "Yashayahu", "Yashaiahu"),
      new BibleBook(24, "Jeremiah", "Canon,OT,Prophets", 52, "Jer", "Je", "Yirmeyahu", "Yirmiyah"),
      new BibleBook(25, "Lamentations", "Canon,OT,Prophets", 5, "Lam", "La", "Eicha"),
      new BibleBook(26, "Ezekiel", "Canon,OT,Prophets", 48, "Ezek", "Eze", "Ezk", "Yechezkel"),

      new BibleBook(27, "Daniel", "Canon,OT,Prophets", 12, "Dan", "Da", "Dn"), // Includes "Susanna and the Elders" (Ch. 13) and "Bel and the Dragon" (Ch. 14)

      new BibleBook(28, "Hosea", "Canon,OT,Prophets", 14, "Hos", "Ho", "Hoshea"),
      new BibleBook(29, "Joel", "Canon,OT,Prophets", 3, "Joe", "Jl", "Yoel"),
      new BibleBook(30, "Amos", "Canon,OT,Prophets", 9, "Am"),
      new BibleBook(31, "Obadiah", "Canon,OT,Prophets", 1, "Obad", "Ob", "Ovadya"),
      new BibleBook(32, "Jonah", "Canon,OT,Prophets", 4, "Jon", "Jo", "Yonah"),
      new BibleBook(33, "Micah", "Canon,OT,Prophets", 7, "Mic", "Mc", "Micha"),
      new BibleBook(34, "Nahum", "Canon,OT,Prophets", 3, "Nah", "Nachum"),
      new BibleBook(35, "Habakkuk", "Canon,OT,Prophets", 3, "Hab", "Hb", "Chavakkuk"),
      new BibleBook(36, "Zephaniah", "Canon,OT,Prophets", 3, "Zeph", "Zep", "Zp", "Tzefanya"),
      new BibleBook(37, "Haggai", "Canon,OT,Prophets", 2, "Hag", "Hg", "Chaggai"),
      new BibleBook(38, "Zechariah", "Canon,OT,Prophets", 14, "Zech", "Zec", "Zc", "Zecharya"),
      new BibleBook(39, "Malachi", "Canon,OT,Prophets", 4, "Mal", "ML"),

      new BibleBook(40, "Matthew", "Canon,NT,Gospel", 28, "Matt", "Mt", "Matityah"),
      new BibleBook(41, "Mark", "Canon,NT,Gospel", 16, "Mar", "Mr", "Makabi"),
      new BibleBook(42, "Luke", "Canon,NT,Gospel", 24, "Luk", "Lu", "Ur"),
      new BibleBook(43, "John", "Canon,NT,Gospel", 21, "Joh", "Jn", "Yochanan", "Yohanan"),
      
      new BibleBook(44, "Acts", "Canon,NT,Apostles", 28, "Act", "Ac", "Pyilut Hashaliachim"),
      
      new BibleBook(45, "Romans", "Canon,NT,Letters", 16, "Rom", "Ro", "Rm"),
      new BibleBook(46, "1 Corinthians", "Canon,NT,Letters,Corinthians,Cor", 16, "1Co", "1Cor"),
      new BibleBook(47, "2 Corinthians", "Canon,NT,Letters,Corinthians,Cor", 13, "2Co", "2Cor"),
      new BibleBook(48, "Galatians", "Canon,NT,Letters", 6, "Ga", "Gal"),
      new BibleBook(49, "Ephesians", "Canon,NT,Letters", 6, "Eph", "Ep", "Ephes"),
      new BibleBook(50, "Philippians", "Canon,NT,Letters", 4, "Phil", "Php", "Pp"),
      new BibleBook(51, "Colossians", "Canon,NT,Letters", 4, "Col", "Co"),
      new BibleBook(52, "1 Thessalonians", "Canon,NT,Letters,Thessalonians,Thes", 5, "1Th", "1Thes"),
      new BibleBook(53, "2 Thessalonians", "Canon,NT,Letters,Thessalonians,Thes", 3, "2Th", "2Thes"),
      new BibleBook(54, "1 Timothy", "Canon,NT,Letters,Timothy,Tim", 6, "1Ti", "1Tim"),
      new BibleBook(55, "2 Timothy", "Canon,NT,Letters,Timothy,Tim", 4, "2Ti", "2Tim"),
      new BibleBook(56, "Titus", "Canon,NT,Letters", 3, "Tit", "Ti"),
      new BibleBook(57, "Philemon", "Canon,NT,Letters", 1, "Phm", "Ph"),

      new BibleBook(58, "Hebrews", "Canon,NT,Letters", 13, "Heb", "He", "Yehudim", "Mashiakim Yehudim\t"),
      new BibleBook(59, "James", "Canon,NT,Letters", 5, "Jas", "Jm", "Ya'akov"),
      new BibleBook(60, "1 Peter", "Canon,NT,Letters,Peter", 5, "1Pe", "1Pet", "1 Kefa"),
      new BibleBook(61, "2 Peter", "Canon,NT,Letters,Peter", 3, "2Pe", "2Pet", "2 Kefa"),
      new BibleBook(62, "1 John", "Canon,NT,Letters.John", 5, "1Jo", "1Jn", "1 Yochanan"),
      new BibleBook(63, "2 John", "Canon,NT,Letters,John", 1, "2Jo", "2Jn", "2 Yochanan"),
      new BibleBook(64, "3 John", "Canon,NT,Letters,John", 1, "3Jo", "3Jn", "3 Yochanan"),
      Jude = new BibleBook(65, "Jude", "Canon,NT,Letters", 1, "Jud", "Jd", "Yahudah"),

      new BibleBook(66, "Revelation", "Canon,NT,Prophecy", 22, "Rev", "Re", "Revelations", "Hagilu Natan Elohim\t"));

  public static final BibleBook ESDRAS_1 = new BibleBook(67, "1 Esdras", "Apocrypha,Deuterocanon,Eastern,Esdras,Ezra", 9);
  public static final BibleBook ESDRAS_2 = new BibleBook(68, "2 Esdras", "Apocrypha,Deuterocanon,Ethiopian,Esdras,Ezra", 16);
  public static final BibleBook WISDOM_OF_SOLOMON;
  public static final BibleBook MACCABES_1;
  public static final BibleBook SIRACH;
  public static final ImmutableList<BibleBook> UNIVERSAL_DEUTEROCANON = ImmutableList.of(
      new BibleBook(69, "Tobit", "Apocrypha,Deuterocanon,Universal", 14, "Tob", "Tb"),
      new BibleBook(70, "Judith", "Apocrypha,Deuterocanon,Universal", 16, "Jth", "Jdth", "Jdt"),
      WISDOM_OF_SOLOMON = new BibleBook(71, "Wisdom of Solomon", "Apocrypha,Deuterocanon,Universal", 19, "Wisdom", "Wis", "Ws"),
      SIRACH = new BibleBook(72, "Sirach", "Apocrypha,Deuterocanon,Universal", 51, "Ecclesiasticus", "Sir", "Ecclus"),
      new BibleBook(73, "Baruch", "Apocrypha,Deuterocanon,Universal", 5, "Bar"),
      MACCABES_1 = new BibleBook(74, "1 Maccabees", "Apocrypha,Deuterocanon,Universal,Maccabees,Mac,Macc", 16, "1Mac", "1Macc", "1 Machabees"),
      new BibleBook(75, "2 Maccabees", "Apocrypha,Deuterocanon,Universal,Maccabees,Mac,Macc", 15, "2Mac", "2Macc", "2 Machabees"));

  public static final BibleBook PRAYER_OF_MANASSEH;
  public static final ImmutableList<BibleBook> APOCRYPHA = ImmutableList.<BibleBook>builder()
      .add(ESDRAS_1, ESDRAS_2)
      .addAll(UNIVERSAL_DEUTEROCANON)
      .add(
          new BibleBook(76, "Letter of Jeremiah", "Apocrypha", 1, "LJe"),
          new BibleBook(77, "Prayer of Azariah", "Apocrypha", 1,
              "Song of the Three", "Song of the Three Holy Children", "Song of the Three Children", "PrA", "azriah"),
          new BibleBook(78, "Susanna", "Apocrypha", 1, "Sus"),
          new BibleBook(79, "Bel and the Dragon", "Apocrypha", 1, "Bel"),
          PRAYER_OF_MANASSEH = new BibleBook(80, "Prayer of Manasseh", "Apocrypha,Deuterocanon,Eastern", 1, "PrM", "PMa"))
      .build();

  public static final BibleBook ADDITIONS_TO_ESTHER =
      new BibleBook(81, "Additions to Esther", "Apocrypha,Esther", 16, "Add Esther", "Add Est", "Add Esth", "Add Es");
  public static final BibleBook PSALMS_151 = new BibleBook(82, "Psalms 151", "Apocrypha,Psalms,Psa", 1, "Add Ps", "Add Psa");

  public static final ImmutableList<BibleBook> EASTERN_ORTHODOX_DEUTEROCANON = ImmutableList.of(
      ESDRAS_1,
      new BibleBook(83, "3 Maccabees", "Apocrypha,Deuterocanon,Eastern,Machabees,Mac,Macc", 7, "3Mac", "3Macc", "3 Machabees"),
      new BibleBook(84, "4 Maccabees", "Apocrypha,Deuterocanon,Eastern,Machabees,Mac,Macc", 18, "4Mac", "4Macc", "4 Machabees"),
      PRAYER_OF_MANASSEH);

  public static final BibleBook ENOCH;
  public static final BibleBook JUBILEES;
  public static final ImmutableList<BibleBook> ETHIOPIAN_ORTHODOX_DEUTEROCANON =
      ImmutableList.of(
          ESDRAS_2,
          ENOCH = new BibleBook(85, "Enoch", "Apocrypha,Deuterocanon,Ethiopian", 108, "Hanok", "Eno"),
          JUBILEES = new BibleBook(86, "Jubilees", "Apocrypha,Deuterocanon,Ethiopian", 50, "Jub"));

  public static final BibleBook JASHER = new BibleBook(87, "Jasher",
      "Apocrypha,Jewish History", 91, "Yasher", "Yahusher");
  public static final BibleBook COMMUNITY_RULE = new BibleBook(88, "Community Rule",
      "Apocrypha,Jewish History", 1, "cr", "Manual of Discipline", "MD");
  public static final BibleBook WAR_SCROLL = new BibleBook(89, "War Scroll",
      "Apocrypha,Jewish History", 19, "War");
  public static final BibleBook JOSEPHUS = new BibleBook(90, "Josephus", "Apocrypha,Jewish History", 20);
  public static final BibleBook BOOK_OF_ADAM_AND_EVE = new BibleBook(91, "Book of Adam and Eve",
      "Apocrypha,Jewish History", 51);
  public static final BibleBook TESTAMENT_OF_JOB = new BibleBook(92, "Testament of Job", "Apocrypha,Jewish History", 12);

  public static final ImmutableSet<BibleBook> ALL_BOOKS = ImmutableSet.<BibleBook>builder()
      .addAll(CANON)
      .addAll(APOCRYPHA)
      .add(ADDITIONS_TO_ESTHER, PSALMS_151)
      .addAll(EASTERN_ORTHODOX_DEUTEROCANON)
      .addAll(ETHIOPIAN_ORTHODOX_DEUTEROCANON)
      .add(JASHER, COMMUNITY_RULE, WAR_SCROLL, JOSEPHUS, BOOK_OF_ADAM_AND_EVE, TESTAMENT_OF_JOB)
      .build();

  private final int bookNum;
  private final String name;
  private final String tags;
  private final int chapterCount;
  private final ImmutableList<String> altNames;

  BibleBook(int bookNum, String name, String tags, int chapterCount, String... altNames) {
    this.bookNum = bookNum;
    this.name = name;
    this.tags = tags;
    this.chapterCount = chapterCount;
    this.altNames = ImmutableList.copyOf(altNames);
  }

  public int getBookNum() {
    return bookNum;
  }

  public String getName() {
    return name;
  }

  public ImmutableList<String> getNames() {
    return ImmutableList.<String>builder().add(name).addAll(getAltNames()).build();
  }

  public String getTags() {
    return tags;
  }

  public int getChapterCount() {
    return chapterCount;
  }

  public ImmutableList<String> getAltNames() {
    return altNames;
  }

  private static final ImmutableMap<String, BibleBook> BY_NAME = ALL_BOOKS.stream()
      .flatMap(book -> book.getNames().stream().map(name -> Pair.of(collapseName(name), book)))
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

    if (book == BibleBook.Psalms && chapter == 151) {
      return BibleBook.PSALMS_151;
    } else if (book == BibleBook.PSALMS_151 && chapter != 1) {
      return BibleBook.Psalms;
    }

    return book;
  }

  @Override
  public String toString() {
    return getName();
  }
}

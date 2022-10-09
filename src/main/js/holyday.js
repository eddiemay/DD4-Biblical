HolyDay = function(title, cssClass, matcher, references, summary) {
  this.title = title;
  this.cssClass = cssClass;
  this.matches = matcher;
  this.references = references;
  this.summary = summary;
}

HolyDay.prototype.title;
HolyDay.prototype.cssClass;
HolyDay.prototype.matches;
HolyDay.prototype.references;
HolyDay.prototype.summary;

var SEPARATE_DAYS = 'Enoch 75:1-2';

HolyDay.HOLY_DAYS = {
  PASSOVER: new HolyDay('Passover', 'passover', date => date.getMonth() == 1 && date.getDay() == 14,
      'Exodus 12:6, Leviticus 23:5, Numbers 28:16, Deuteronomy 16:1, Jubilees 49:10'),

  PASSOVER_MAKEUP: new HolyDay('Passover Makeup', 'passover-makeup',
      date => date.getMonth() == 2 && date.getDay() == 14, 'Numbers 9:9-13'),

  FESTIVAL_UNLEAVENED_BREAD: new HolyDay('Festival of Unleavened Bread', 'unleavened-bread',
      date => date.getMonth() == 1 && date.getDay() >= 15 && date.getDay() <= 21, 'Leviticus 23:6-8'),

  WEAVE_OFFERING: new HolyDay('Weave Offering', 'weave-offering', date => date.getDayOfYear() == 22,
      'Leviticus 23:10-14'),
  FEAST_FIRST_FRUITS: new HolyDay('Feast of First Fruits', 'first-fruits', date => date.getDayOfYear() == 21 + 50,
      'Leviticus 23:15-21,Deuteronomy 16:9-12,Jubilees 6:1,17'),

  FEAST_TRUMPETS: new HolyDay('Feast of Trumpets', 'feast-trumpets', date => date.getMonth() == 7 && date.getDay() == 1,
      'Leviticus 23:24-25'),

  ATONEMENT_EVE: new HolyDay('Atonement Eve', 'atonement-eve', date => date.getMonth() == 7 && date.getDay() == 9,
      'Leviticus 23:32'),
  ATONEMENT_DAY: new HolyDay('Atonement Day', 'atonement-day', date => date.getMonth() == 7 && date.getDay() == 10,
      'Leviticus 23:27-32'),

  FESTIVAL_BOOTHS: new HolyDay('Festival of Booths', 'festival-booths',
      date => date.getMonth() == 7 && date.getDay() >= 15 && date.getDay() <= 21, 'Leviticus 23:34-43'),
  SOLEMN_ASSEMBLY: new HolyDay('Solemn Day of Assembly', 'solemn-assembly',
      date => date.getMonth() == 7 && date.getDay() == (15 + 7), 'Leviticus 23:36'),

  NEW_MONTH_TEN: new HolyDay('New Month Feast / Dedication Day 8', 'new-month',
      date => date.getDay() == 1 && date.getMonth() == 10, 'Numbers 10:10,28:11-15, 1 Maccabees 4:59'),
  NEW_MONTH: new HolyDay('New Month Feast', 'new-month', date => date.getDay() == 1 && date.getMonth() < 13,
      'Numbers 10:10,28:11-15'),

  SUMMER_BEGINS: new HolyDay('Summer Begins', 'summer-begins', hebrewDate => hebrewDate.getDayOfYear() == 91,
      'Enoch 72:13-14,' + SEPARATE_DAYS),
  AUTUMN_BEGINS: new HolyDay('Autumn Begins', 'autumn-begins', hebrewDate => hebrewDate.getDayOfYear() == 182,
      'Enoch 72:19-20,' + SEPARATE_DAYS),
  WINTER_BEGINS: new HolyDay('Winter Begins / Dedication Day 7', 'winter-begins',
      hebrewDate => hebrewDate.getDayOfYear() == 273, 'Enoch 72:25-26, 1 Maccabees 4:59, John 10:22,' + SEPARATE_DAYS),
  SPRING_BEGINS: new HolyDay('Spring Begins / Last day of year', 'spring-begins',
      hebrewDate => hebrewDate.getDayOfYear() == 364, 'Enoch 72:31-32, Jubilees 6:31-32,' + SEPARATE_DAYS),
  LEAP_WEEK: new HolyDay('Leap Week', 'leap-week', hebrewDate => hebrewDate.getMonth() == 13, 'Enoch 74:11',
      `<p>After 5 years the calendar is 6 days (6.2125 days to be exact 365.2425 - 364 = 1.2425 x 5) behind the actual
      exact solar year. We need to do a leap week of 7 days in order to catch up. This leap is done at the end of every
      5 years except if that year is a Jubilee (50th year). In that case no leap is needed because the Jubilee is made
      perfect from the excess of the .7875 days we have added over the last 9 leap years (9 x .7875 = 7.0875)</p>`),

  FESTIVAL_DEDICATION: new HolyDay('Festival of Dedication', 'festival-dedication', date => date.getMonth() == 9
      && date.getDay() >= 25 || date.getMonth() == 10 && date.getDay() == 1, '1 Maccabees 4:50-55,56-59, John 10:22'),
}

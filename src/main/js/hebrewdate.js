var ONE_DAY = 24 * 60 * 60 * 1000;
var DAYS_IN_MONTH = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
var HEBREW_MONTHS = [
  {number: 1, displayText: '1st Month', days: 30, name: 'Abib/Aviv/Nisan'},
  {number: 2, displayText: '2nd Month', days: 30, name: 'Ziw/Ziv'},
  {number: 3, displayText: '3rd Month', days: 31, name: 'Siwan/Sivan'},
  {number: 4, displayText: '4th Month', days: 30}, {number: 5, displayText: '5th Month', days: 30},
  {number: 6, displayText: '6th Month', days: 31, name: 'Elul'},
  {number: 7, displayText: '7th Month', days: 30, name: 'Eythanim/Ethanim'},
  {number: 8, displayText: '8th Month', days: 30, name: 'Bul'},
  {number: 9, displayText: '9th Month', days: 31, name: 'Kislew/Chislev'},
  {number: 10, displayText: '10th Month', days: 30, name: 'Tebeth'},
  {number: 11, displayText: '11th Month', days: 30, name: 'Shebat'},
  {number: 12, displayText: '12th Month', days: 31, name: 'Adar'}, {number: 13, displayText: 'Leap Week', days: 7}];

HebrewDate = function(year, month, day, date) {
  this.year = year;
  this.month = month;
  this.day = day || 1;
  this.date = date || calculateDate(year, month, day);
}

HebrewDate.prototype.year;
HebrewDate.prototype.month;
HebrewDate.prototype.day;
HebrewDate.prototype.date;

HebrewDate.prototype.getYear = function() {
  return this.year;
}

HebrewDate.prototype.getMonth = function() {
  return this.month;
}

HebrewDate.prototype.getDay = function() {
  return this.day;
}

HebrewDate.prototype.getDate = function() {
  return this.date;
}

HebrewDate.prototype.getDayOfWeek = function() {
  return this.date.getDay() + 1;
}

HebrewDate.prototype.getDayOfYear = function() {
  return Math.floor((this.month - 1) * 30.334) + this.day;
}

HebrewDate.prototype.getWeekOfYear = function() {
  return Math.floor(this.getDayOfYear() / 7) + 1;
}

HebrewDate.prototype.addDays = function(days) {
  if (days == 0) {
    return this;
  }
  var date = new Date(this.date.getTime() + days * ONE_DAY);

  var dayOfYear = this.getDayOfYear() + days;
  if (dayOfYear >= 1 && dayOfYear <= 364 || this.getDayOfYear() >= 365 && dayOfYear <= 371) {
   // Every year is 364 days long except if we are in a leap week situation in which it is 371 days.
   var month = Math.floor(dayOfYear / 30.334) + 1;
   var day = dayOfYear - Math.floor((month - 1) * 30.334);
   return new HebrewDate(this.year, month, day, date);
  }

  return HebrewDate.fromDate(date);
}

HebrewDate.prototype.getShortName = function() {
  return this.year + ' ' + HEBREW_MONTHS[this.month - 1].displayText;
}

HebrewDate.prototype.isEqual = function(hebrewDate) {
  if (!hebrewDate) {
    return false;
  }

  return this.year == hebrewDate.getYear() && this.month == hebrewDate.getMonth() && this.day == hebrewDate.getDay();
}

HebrewDate.prototype.getHolyDay = function() {
  for (holyDayName in HolyDay.HOLY_DAYS) {
    var holyDay = HolyDay.HOLY_DAYS[holyDayName];
    if (holyDay.matches(this)) {
      return holyDay;
    }
  }
}

HebrewDate.fromDate = function(date) {
  console.log('fromDate: ' + date);
  if (date.getHours() > 15) {
    date.setHours(5);
  }
  var dayOfYear = getDayOfYear(date);
  var year = date.getFullYear();
  if (date.getMonth() < 2) {
    year--;
  }
  var abibOne = getAbibOne(year);
  if (date.getTime() < abibOne.getTime()) {
    year--;
    abibOne = getAbibOne(year);
  }
  var hebrewDayOfYear = Math.round((date.getTime() - abibOne.getTime()) / ONE_DAY) + 1;
  var month = Math.floor(hebrewDayOfYear / 30.334) + 1;
  var day = hebrewDayOfYear - Math.floor((month - 1) * 30.334);

  console.log('Day of year: ' + dayOfYear);
  console.log('Hebrew year day: ' + hebrewDayOfYear);
  console.log('Abib 1: ' + abibOne);
  console.log('Hebrew date: ' + year + '-' + month + '-' + day);

  return new HebrewDate(year, month, day, date);
}

getAbibOne = function(year) {
  var baseYear = year - (year % 400);
  var yearDiff = year - baseYear;
  var weeks = yearDiff * 52;

  var baseAbib;
  if (baseYear == 0) {
    weeks -= 52;
    baseAbib = new Date(1, 2, 18, 4, 0);
    baseAbib.setFullYear(1);
  } else {
    baseAbib = new Date(baseYear, 2, 19, 4, 0);
  }
  yearDiff--;

  weeks += Math.floor(yearDiff / 5);

  // Skip every 50th year for the first 200 years.
  weeks -= Math.floor(yearDiff > 200 ? 4 : yearDiff / 50);

  // Skip every 40th year for the second 200 years.
  weeks -= Math.floor(yearDiff < 200 ? 0 : (yearDiff - 200) / 40);

  var abibOne = new Date(baseAbib.getTime() + weeks * ONE_DAY * 7);
  console.log('First day of Abib: ' + abibOne + ', baseYear: ' + baseYear + ', baseAbib: ' + baseAbib);

  return abibOne;
}

calculateDate = function(year, month, day) {
  var abibOne = getAbibOne(year);
  var hebrewDayOfYear = Math.floor((month - 1) * 30.334) + day;

  return new Date(abibOne.getTime() + (hebrewDayOfYear - 1) * ONE_DAY);
}

/** Returns the day of the year in the Georgian calendar from 1 - 366 */
getDayOfYear = function(day) {
  var month = day.getMonth();
  var dayOfYear = day.getDate();
  for (var m = 0; m < month; m++) {
    dayOfYear += DAYS_IN_MONTH[m];
  }
  if (month > 1 && day.getFullYear() % 4 == 0 && (day.getFullYear() % 100 != 0 || day.getFullYear() % 400 == 0)) {
    dayOfYear++;
  }

  return dayOfYear;
}
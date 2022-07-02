var ONE_HOUR = 1000 * 60 * 60;
var ONE_DAY = 24 * ONE_HOUR;
var EVENT_TYPES = ['BIBLICAL', 'NOTIFICATION'];
var SCRIPTURE_VERSIONS = ['ISR', 'RSKJ', 'NRSV', 'NWT', /*'NKJV',*/ 'KJV1611'];

// T. James 951-233-6912

com.digitald4.biblical.CalendarCtrl = function($location, $window, globalData, biblicalEventService, scriptureService) {
  globalData.scriptureVersion = globalData.scriptureVersion || 'ISR';
  this.locationProvider = $location;
  this.window = $window;
  this.globalData = globalData;
  this.scriptureVersions = SCRIPTURE_VERSIONS;
  this.userLoggedIn = () => globalData.activeSession != undefined;
  this.months = HEBREW_MONTHS;
  this.biblicalEventService = biblicalEventService;
  this.scriptureService = scriptureService;

  var year = parseInt($location.search()['year']);
  var month = parseInt($location.search()['month']) || 1;
  this.disableNet = $location.search()['disableNet'];
  this.hebrewDate = year ? new HebrewDate(year, month, 1) : HebrewDate.fromDate(new Date());
  this.hebrewDate = this.hebrewDate.addDays(1 - this.hebrewDate.getDay());
  this.refresh();
}

com.digitald4.biblical.CalendarCtrl.prototype.months = HEBREW_MONTHS;
com.digitald4.biblical.CalendarCtrl.prototype.eventTypes = EVENT_TYPES;
com.digitald4.biblical.CalendarCtrl.prototype.biblicalEventService;
com.digitald4.biblical.CalendarCtrl.prototype.scriptureService;

com.digitald4.biblical.CalendarCtrl.prototype.setupCalendar = function() {
  var hebrewDate = this.hebrewDate;
  var currDay = hebrewDate.addDays(1 - hebrewDate.getDayOfWeek());
  var weeks = [];
  var woy = Math.floor((hebrewDate.getMonth() - 1) * 4.334);
  var days = [];
  var todayDate = new Date();
  var dateDiff = (todayDate.getTime() - hebrewDate.getDate().getTime()) / ONE_DAY;
  var today = dateDiff >= 0 && dateDiff < 31 ? HebrewDate.fromDate(todayDate) : undefined;
  do {
    var week = {weekOfYear: ++woy};
    var weekdays = [];
    for (var d = 1; d <= 7; d++) {
      var holyDay = currDay.getMonth() == hebrewDate.getMonth() ? currDay.getHolyDay() : undefined;
      var day = {
        date: currDay,
        day: (currDay.getDay() != 31 && currDay.getMonth() != 13) ? currDay.getDay() : undefined,
        holyDay: holyDay,
        classes: {
          'even-reference-month': currDay.getDate().getMonth() % 2 == 0,
          'other-month': currDay.getMonth() != hebrewDate.getMonth(),
          'shabbath': d == 7 && currDay.getMonth() == hebrewDate.getMonth(),
          'today': currDay.isEqual(today),
        },
        biblicalEvents: [],
        notifications: []
      };
      if (holyDay) {
        day.classes[holyDay.cssClass] = true;
      }
      weekdays.push(day);
      days.push(day);
      currDay = currDay.addDays(1);
    }
    week.days = weekdays;
    weeks.push(week);
  } while (currDay.getMonth() == hebrewDate.getMonth());
  this.days = days;
  this.weeks = weeks;
}

com.digitald4.biblical.CalendarCtrl.prototype.refresh = function() {
	this.biblicalEvents = [];
	this.setupCalendar();
	if (this.disableNet) {
	  return;
	}
	this.biblicalEventService.listByMonth(this.hebrewDate.month, response => {
	  var biblicalEvents = response.items || [];
	  var firstDayOffset = this.getFirstDayOffset();
	  for (var e = 0; e < biblicalEvents.length; e++) {
	    var biblicalEvent = biblicalEvents[e];
	    if (biblicalEvent.month == this.hebrewDate.getMonth()) {
	      this.days[firstDayOffset + biblicalEvent.day - 1].biblicalEvents.push(biblicalEvent);
	      this.biblicalEvents.push(biblicalEvent);
	    }
	  }
	}, notify);
}

com.digitald4.biblical.CalendarCtrl.prototype.getFirstDayOffset = function() {
  return this.hebrewDate.getDayOfWeek() - 1;
}

com.digitald4.biblical.CalendarCtrl.prototype.getShortName = function() {
  return this.hebrewDate.getShortName();
}

com.digitald4.biblical.CalendarCtrl.prototype.prevMonth = function() {
  var hebrewDate = this.hebrewDate.addDays(-1);
  this.locationProvider.search('year', hebrewDate.getYear());
  this.locationProvider.search('month', hebrewDate.getMonth());
  // this.hebrewDate = hebrewDate.addDays(1 - hebrewDate.getDay());
  // this.refresh();
}

com.digitald4.biblical.CalendarCtrl.prototype.setMonth = function(year, month) {
  var hebrewDate = new HebrewDate(year, month, 1);
  this.locationProvider.search('year', hebrewDate.getYear());
  this.locationProvider.search('month', hebrewDate.getMonth());
  // this.refresh();
}

com.digitald4.biblical.CalendarCtrl.prototype.nextMonth = function() {
  var hebrewDate = this.hebrewDate.addDays(31);
  this.locationProvider.search('year', hebrewDate.getYear());
  this.locationProvider.search('month', hebrewDate.getMonth());
  // this.hebrewDate = hebrewDate.addDays(1 - hebrewDate.getDay());
  // this.refresh();
}

com.digitald4.biblical.CalendarCtrl.prototype.showMonthSelectionDialog = function() {
  this.monthSelection = {year: this.hebrewDate.getYear(), month: this.hebrewDate.getMonth()};
  this.dialogShown = 'SELECT_MONTH';
}

com.digitald4.biblical.CalendarCtrl.prototype.selectMonth = function() {
  this.setMonth(this.monthSelection.year, this.monthSelection.month);
	this.closeDialog();
}

com.digitald4.biblical.CalendarCtrl.prototype.showViewEventDialog = function(event) {
  this.event = event;
  if (!event.displayText) {
    this.refreshDisplayText(event);
  }
  this.dialogShown = 'VIEW_EVENT';
  this.window.scrollTo(0, 0);
}

com.digitald4.biblical.CalendarCtrl.prototype.scriptureVersionChanged = function() {
  for (var e = 0; e < this.biblicalEvents.length; e++) {
    this.biblicalEvents[e].displayText = undefined;
  }
  this.refreshDisplayText(this.event);
}

com.digitald4.biblical.CalendarCtrl.prototype.refreshDisplayText = function(event) {
  if (event.references) {
    this.scriptureService.scriptures(event.references, response => {
      var scriptures = response.items || [];
      event.scriptureGroups = [];
      var group;
      for (const script of scriptures) {
        if (!group || group.book != script.book || group.chapter != script.chapter || group.curVer + 1 != script.verse) {
          group = {book: script.book, chapter: script.chapter, verse: script.verse, scriptures: []};
          event.scriptureGroups.push(group);
        }
        group.scriptures.push(script);
        group.curVer = script.verse;
      }
    }, notifyError);
  }
  if (event.summary) {
    this.scriptureService.expand(event.summary, true, displayText => event.displayText = displayText, notifyError);
  }
}

com.digitald4.biblical.CalendarCtrl.prototype.showCreateEventDialog = function(hebrewDate) {
  this.editEvent = {type: 'BIBLICAL', month: hebrewDate.getMonth(), day: hebrewDate.getDay()};
  this.dialogShown = 'EDIT_EVENT';
}

com.digitald4.biblical.CalendarCtrl.prototype.createEvent = function() {
	this.biblicalEventService.create(this.editEvent, event => {
    if (event.month == this.hebrewDate.month) {
      var firstDayOffset = this.getFirstDayOffset();
      this.days[firstDayOffset + event.day - 1].biblicalEvents.push(event);
      this.biblicalEvents.push(event);
    }
    if (this.onUpdate) {
      this.onUpdate();
    }
	  this.closeDialog();
	}, notifyError);
}

com.digitald4.biblical.CalendarCtrl.prototype.showEditEventDialog = function(event) {
  this.editEvent = {id: event.id, month: event.month, day: event.day, year: event.year,
      references: event.references, title: event.title, summary: event.summary};

  if (event.summary) {
    this.scriptureService.expand(event.summary, false, displayText => this.editEvent.summary = displayText, notifyError);
  }

  this.origEvent = event;
  this.dialogShown = 'EDIT_EVENT';
}

com.digitald4.biblical.CalendarCtrl.prototype.saveEvent = function() {
  var edits = [];
  var dayChanged = false;
  if (this.editEvent.month != this.origEvent.month) {
    edits.push('month');
    dayChanged = true;
  }
  if (this.editEvent.day != this.origEvent.day) {
    edits.push('day');
    dayChanged = true;
  }
  if (this.editEvent.year != this.origEvent.year) {edits.push('year');}
  if (this.editEvent.references != this.origEvent.references) {edits.push('references');}
  if (this.editEvent.title != this.origEvent.title) {edits.push('title');}
  if (this.editEvent.summary != this.origEvent.summary) {edits.push('summary');}

	this.updateError = undefined;
	this.biblicalEventService.update(this.editEvent, edits, event => {
	  if (dayChanged) { // If the event has moved days, then we need to remove the original event and insert the new event.
      var firstDayOffset = this.getFirstDayOffset();
      var origDay = this.days[firstDayOffset + this.origEvent.day - 1];
      if (origDay) {
        var index = origDay.biblicalEvents.indexOf(this.origEvent);
        origDay.biblicalEvents.splice(index, 1);
        index = this.biblicalEvents.indexOf(this.origEvent);
        this.biblicalEvents.splice(index, 1);
      }
      if (event.month == this.hebrewDate.month) {
        var newDay = this.days[firstDayOffset + event.day - 1];
        if (newDay) {
          newDay.biblicalEvents.push(event);
          this.biblicalEvents.push(event);
        }
      }
    } else { // The event is on the same day, then copy the updated info to the original item.
      this.origEvent.references = event.references;
      this.origEvent.title = event.title;
      this.origEvent.summary = event.summary;
      this.origEvent.year = event.year;
      this.origEvent.displayText = undefined;
    }
    if (this.onUpdate) {
      this.onUpdate();
    }
	  this.closeDialog();
	}, error => this.updateError = error);
}

com.digitald4.biblical.CalendarCtrl.prototype.closeDialog = function() {
  this.dialogShown = undefined;
}

com.digitald4.biblical.CalendarCtrl.prototype.showScripture = function(version, book, chapter, verse) {
  this.reference = {book: book, chapter: chapter, verse: verse, version: version || this.globalData.scriptureVersion};
  this.scriptureDialogShown = true;
}

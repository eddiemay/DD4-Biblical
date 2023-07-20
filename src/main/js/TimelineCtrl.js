var ONE_BCE = 3960;
var VIEW_OPTIONS = {
  AFTER_MAN_AND_COMMON_ERA: 'After Man + Common Era',
  AFTER_MAN_ONLY: 'After Man Only',
  COMMON_ERA_ONLY: 'Common Era Only'
}
var YEAR_BOUND = {
  JUBILEE: {name: 'Jubilee', years: 50, abb: 'j', countBy: 1},
  CENTURY: {name: 'Century', years: 100, abb: 'c', countBy: 7},
  MILLENNIUM: {name: 'Millennium', years: 1000, abb: 'm', countBy: 25}
}

com.digitald4.biblical.TimelineCtrl = function($location, $window, globalData, biblicalEventService) {
  this.locationProvider = $location;
  this.window = $window;
  this.globalData = globalData;
  this.globalData.scriptureVersion = globalData.scriptureVersion || 'NWT';
  this.months = HEBREW_MONTHS;
  this.DEPENDENCY_RELATIONS =
      ['START_TO_START', 'FINISH_TO_START', 'START_TO_FINISH', 'FINISH_TO_FINISH'];
  this.viewOptions = VIEW_OPTIONS;
  this.yearBound = YEAR_BOUND;
  this.biblicalEventService = biblicalEventService;

  this.offset = $location.search()['offset'] || '1j';

  var jubilee = parseInt($location.search()['jubilee']);
  if (jubilee > 1) {
    this.offset = jubilee + 'j';
  }
  var abb = this.offset.substring(this.offset.length - 1);
  for (var prop in YEAR_BOUND) {
    var yearBound = YEAR_BOUND[prop];
    if (yearBound.abb == abb) {
      this.offsetUnit = yearBound;
      break;
    }
  }
  this.offsetValue = parseInt(this.offset.substring(0, this.offset.length - 1));
  this.view = $location.search()['view'] || VIEW_OPTIONS.AFTER_MAN_AND_COMMON_ERA;
  this.startYear = (this.offsetValue - 1) * this.offsetUnit.years + 1;
  this.endYear = this.offsetValue * this.offsetUnit.years;
  this.refresh();
}

com.digitald4.biblical.TimelineCtrl.prototype.refresh = function() {
	this.loading = true;
	this.biblicalEvents = [];
	this.biblicalEventService.listTimelineEvents(this.startYear, this.endYear, response => {
	  var events = response.items || [];
	  for (var e = 0; e < events.length; e++) {
	    var event = events[e];
	    if (event.year <= this.endYear && event.endYear >= this.startYear) {
	      if (this.view == VIEW_OPTIONS.AFTER_MAN_ONLY) {
	        event.display = event.displayAmOnly;
	      } else if (this.view == VIEW_OPTIONS.COMMON_ERA_ONLY) {
	        event.display = event.displayEraOnly;
	      }
	      this.biblicalEvents.push(this.setMetadata(event));
	    }
	  }
	  this.loading = undefined;
	});

	var years = [];
	var displayYears = this.offsetUnit.years;
	var countBy = this.offsetUnit.countBy;
	var middle = Math.floor(displayYears / 2);
	for (var y = 0; y < displayYears; y++) {
	  var year = this.startYear + y;
	  var percentLeft = y * 100 / displayYears;
	  var style = {left: percentLeft + '%'};
	  var hoverText = this.getHoverText(year);
	  if (y == 0 || y + 1 == middle || y + 1 == displayYears) {
	    years.push({year: hoverText, display: hoverText, style: style});
	  } else if (year % 50 == 0) {
	    years.push({year: hoverText, display: '🎉', style: style});
	  } else if ((y + 1) % countBy == 0) {
	    years.push({year: hoverText, display: '|', style: style});
	  }
  }
	//
	this.years = years;
}

com.digitald4.biblical.TimelineCtrl.prototype.getHoverText = function(year) {
  var am = year + 'AM';
  if (this.view == VIEW_OPTIONS.AFTER_MAN_ONLY) {
    return am;
  }

  var era = (year <= ONE_BCE) ? (ONE_BCE + 1 - year) + 'BCE' : (year - ONE_BCE) + 'CE';
  if (this.view == VIEW_OPTIONS.COMMON_ERA_ONLY) {
    return era;
  }

  return am + ' (' + era + ')';
}

com.digitald4.biblical.TimelineCtrl.prototype.getDisplayYear = function(year) {
  var era = (year <= ONE_BCE) ? (ONE_BCE + 1 - year) + 'BCE' : (year - ONE_BCE) + 'CE';
  return this.view == VIEW_OPTIONS.COMMON_ERA_ONLY ? era : year + 'AM';
}

com.digitald4.biblical.TimelineCtrl.prototype.setMetadata = function(event) {
  var left = this.getLeftPercent(event);
  event.style = {left: left + '%'};

  if (event.year == event.endYear) {
    event.class = event.depEventId ? 'milestone' : 'milestone-red';
    var paddingLeft = left < 67 ? (left + 1) : (left - (event.display.length * .70));
    event.titleStyle = {'padding-left': paddingLeft + '%'};
  } else {
    event.class = 'event-orange';
    event.style.right = this.getRightPercent(event) + '%';
  }

  return event;
}

com.digitald4.biblical.TimelineCtrl.prototype.getLeftPercent = function(event) {
  if (event.year < this.startYear) {
    return 0;
  }

  var indexYear = ((event.year - 1) % this.offsetUnit.years) + 1;
  indexYear += event.month / 12.0 + event.day / 364.0;
  return (indexYear - 1) * 100 / this.offsetUnit.years;
}

com.digitald4.biblical.TimelineCtrl.prototype.getRightPercent = function(event) {
  if (event.endYear <= event.startYear) {
    return undefined;
  }

  if (event.endYear >= this.endYear) {
    return 0;
  }

  var right = event.endYear % this.offsetUnit.years;
  return 100 - (right - 1) * 100 / this.offsetUnit.years;
}

com.digitald4.biblical.TimelineCtrl.prototype.prevValue = function() {
  this.offsetValue = this.offsetValue - 1;
  this.setValue();
}

com.digitald4.biblical.TimelineCtrl.prototype.setValue = function() {
  if (this.offsetValue < 1) {
    this.offsetValue = 1;
  } else if (this.offsetValue * this.offsetUnit.years > 6000) {
    this.offsetValue = 6000 / this.offsetUnit.years;
  }
  this.locationProvider.search(
      'offset', this.offsetValue + this.offsetUnit.abb);
}

com.digitald4.biblical.TimelineCtrl.prototype.nextValue = function() {
  this.offsetValue = this.offsetValue + 1;
  this.setValue();
}

com.digitald4.biblical.TimelineCtrl.prototype.setUnit = function() {
  this.offsetValue = parseInt(this.startYear / this.offsetUnit.years) + 1;
  this.setValue();
}

com.digitald4.biblical.TimelineCtrl.prototype.setView = function() {
  this.locationProvider.search('view', this.view);
}

com.digitald4.biblical.TimelineCtrl.prototype.showEvent = function(event) {
  this.event = event;
  this.viewDialogShown = true;
  this.window.scrollTo(0, 0);
}

com.digitald4.biblical.TimelineCtrl.prototype.showAddDialog = function(dependencyId) {
	this.allEvents = [];
	Array.prototype.push.apply(this.allEvents, this.biblicalEvents);
	this.editEvent = {depEventId: dependencyId};
	this.viewDialogShown = undefined;
  this.editDialogShown = true;
}

com.digitald4.biblical.TimelineCtrl.prototype.showEditDialog = function(event) {
	this.allEvents = [];
	Array.prototype.push.apply(this.allEvents, this.biblicalEvents);
  if (event.offset != undefined && event.offset.years > 50) {
    this.readAll();
  }
	this.editEvent = event;
	this.viewDialogShown = undefined;
  this.editDialogShown = true;
}

com.digitald4.biblical.TimelineCtrl.prototype.readAll = function() {
  this.biblicalEventService.listAll(
      response => Array.prototype.push.apply(this.allEvents, response.items));
}

com.digitald4.biblical.TimelineCtrl.prototype.createEvent = function() {
	this.biblicalEventService.create(this.editEvent, created => {
	  this.refresh();
	  this.closeDialog();
	});
}

com.digitald4.biblical.TimelineCtrl.prototype.saveEvent = function() {
  var edits = ['title'];
  if (this.editEvent.summary) edits.push('summary')
  if (this.editEvent.depEventId) edits.push('depEventId');
  if (this.editEvent.depRelationship) edits.push('depRelationship');
  if (this.editEvent.offset.value) {
    this.editEvent.offset = {value: this.editEvent.offset.value};
    edits.push('offset');
  }
  if (this.editEvent.duration && this.editEvent.duration.value){
    this.editEvent.duration = {value: this.editEvent.duration.value};
    edits.push('duration');
  }

	this.biblicalEventService.update(this.editEvent, edits, updated => {
	  this.refresh();
	  this.closeDialog();
	});
}

com.digitald4.biblical.TimelineCtrl.prototype.updateAll = function() {
  var total = this.biblicalEvents.length;
  var index = 0;
  var updateNext = () => {
    if (index < this.biblicalEvents.length) {
      this.biblicalEventService.update(this.biblicalEvents[index], ['title'], updated => {
        index++;
        updateNext();
      });
    } else {
      this.refresh();
    }
  }

  updateNext();
	/* this.biblicalEventService.batchUpdate(this.biblicalEvents, ['title'], updated => {
	  this.refresh();
	}); */
}

com.digitald4.biblical.TimelineCtrl.prototype.scriptureVersionChanged = function() {
  this.event.summary = this.event.summary + ' ';
}

com.digitald4.biblical.TimelineCtrl.prototype.closeDialog = function() {
  this.viewDialogShown = undefined;
  this.editDialogShown = undefined;
}

com.digitald4.biblical.TimelineCtrl = function($location, $window, globalData, biblicalEventService) {
  this.locationProvider = $location;
  this.window = $window;
  this.globalData = globalData;
  globalData.scriptureVersion = globalData.scriptureVersion || 'NWT';
  this.months = HEBREW_MONTHS;
  this.DEPENDENCY_RELATIONS = ['START_TO_START', 'FINISH_TO_START', 'START_TO_FINISH', 'FINISH_TO_FINISH'];
  this.biblicalEventService = biblicalEventService;

  this.jubilee = parseInt($location.search()['jubilee']) || 1;
  if (this.jubilee < 1) {
    this.jubilee = 1;
  }
  this.startYear = (this.jubilee - 1) * 50 + 1;
  this.endYear = this.jubilee * 50;
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
	      this.biblicalEvents.push(this.setMetadata(event));
	    }
	  }
	  this.loading = undefined;
	}, notify);

	this.years = [];
	for (var y = 0; y < 49;) {
	  this.years.push({year: (this.startYear + y), display: '[', class: 'at-year-' + (++y)});
	  this.years.push({year: (this.startYear + y), display: '|', class: 'at-year-' + (++y)});
	  this.years.push({year: (this.startYear + y), display: '|', class: 'at-year-' + (++y)});
	  this.years.push({year: (this.startYear + y), display: (this.startYear + y), class: 'at-year-' + (++y)});
	  this.years.push({year: (this.startYear + y), display: '|', class: 'at-year-' + (++y)});
	  this.years.push({year: (this.startYear + y), display: '|', class: 'at-year-' + (++y)});
	  this.years.push({year: (this.startYear + y), display: ']', class: 'at-year-' + (++y)});
	}
	this.years.push({year: this.endYear, display: '🎉', class: 'at-year-50'});
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

  var indexYear = ((event.year - 1) % 50) + 1;
  indexYear += event.month / 12.0 + event.day / 364.0;
  return (indexYear - 1) * 2;
}

com.digitald4.biblical.TimelineCtrl.prototype.getRightPercent = function(event) {
  if (event.endYear <= event.startYear) {
    return undefined;
  }

  if (event.endYear > this.endYear) {
    return 0;
  }

  var right = event.endYear == this.endYear ? 50 : event.endYear % 50;
  return 100 - (right - 1) * 2;
}

com.digitald4.biblical.TimelineCtrl.prototype.prevJubilee = function() {
  this.jubilee = this.jubilee - 1;
  this.setJubilee();
}

com.digitald4.biblical.TimelineCtrl.prototype.setJubilee = function() {
  if (this.jubilee < 1) {
    this.jubilee = 1;
  } else if (this.jubilee > 150) {
    this.jubilee = 150;
  }
  this.locationProvider.search('jubilee', this.jubilee);
}

com.digitald4.biblical.TimelineCtrl.prototype.nextJubilee = function() {
  this.jubilee = this.jubilee + 1;
  this.setJubilee();
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
  this.biblicalEventService.listAll(response => Array.prototype.push.apply(this.allEvents, response.items), notifyError);
}

com.digitald4.biblical.TimelineCtrl.prototype.createEvent = function() {
	this.biblicalEventService.create(this.editEvent, created => {
	  this.refresh();
	  this.closeDialog();
	}, notifyError);
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
	}, notifyError);
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
	}, notify); */
}

com.digitald4.biblical.TimelineCtrl.prototype.scriptureVersionChanged = function() {
  this.event.summary = this.event.summary + ' ';
}


com.digitald4.biblical.TimelineCtrl.prototype.closeDialog = function() {
  this.viewDialogShown = undefined;
  this.editDialogShown = undefined;
}
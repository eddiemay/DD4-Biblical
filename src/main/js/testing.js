var BIBLE_BOOKS = [
    {name: 'Genesis', chapters: 50}, {name: 'Exodus', chapters: 40}, {name: 'Leviticus', chapters: 27},
    {name: 'Numbers', chapters: 36}, {name: 'Deuteronomy', chapter: 34}, {name: 'Joshua', chapter: 34},
    {name: 'Judges', chapters: 21}, {name: 'Ruth', chapters: 4}, {name: '1 Samuel', chapters: 31},
    {name: '2 Samuel', chapter: 24}, {name: '1 Kings', chapter: 22}, {name: '2 Kings', chapters: 25}];
ScriptureServiceInMemoryImpl = function() {}

ScriptureServiceInMemoryImpl.prototype.getBibleBooks = function(success, error) {
  success({items: BIBLE_BOOKS});
}

ScriptureServiceInMemoryImpl.prototype.getText = function(reference, success, error) {
  success({items: [reference + ' [Scripture place holder]']});
}

BiblicalEventServiceInMemoryImpl = function() {
  var maxId = 1000;
  for (var eventId in this.biblicalEvents) {
    var biblicalEvent = this.biblicalEvents[eventId];
    this.getByMonth(biblicalEvent.month).push(biblicalEvent);
    if (eventId > maxId) {
      maxId = eventId;
    }
  }
  this.idSequence = maxId + 1;
}

BiblicalEventServiceInMemoryImpl.prototype.listByMonth = function(month) {
  this.biblicalEventsByMonth[month] = this.biblicalEventsByMonth[month] || [];
  return this.biblicalEventsByMonth[month];
}

BiblicalEventServiceInMemoryImpl.prototype.create = function(biblicalEvent, success, error) {
  biblicalEvent.id = this.idSequence++;
  this.getByMonth(biblicalEvent.month).push(biblicalEvent);
  success(biblicalEvent);
}

BiblicalEventServiceInMemoryImpl.prototype.update = function(biblicalEvent, updateMask, success, error) {
  var original = this.biblicalEvents[biblicalEvent.id];
  var index = this.getByMonth(biblicalEvent.month).indexOf(original);
  this.getByMonth(biblicalEvent.month).splice(index, 1);
  this.biblicalEvents[biblicalEvent.id] = biblicalEvent;
  this.getByMonth(biblicalEvent.month).push(biblicalEvent);
  success(biblicalEvent);
}

BiblicalEventServiceInMemoryImpl.prototype.delete = function(id, success, error) {
  var original = this.biblicalEvents[id];
  var index = this.getByMonth(biblicalEvent.month).indexOf(original);
  this.getByMonth(biblicalEvent.month).splice(index, 1);
  this.biblicalEvents[id] = undefined;
  success(biblicalEvent);
}

BiblicalEventServiceInMemoryImpl.prototype.biblicalEvents = {
  1000: {id: 1000, month: 9, day: 24, scripture: 'Haggai 2:10', title: 'יהוה speaks to Haggai',
      summary: `<p>Haggai 2:10 On the 24th day of the ninth month, in the second year of Darius, the word of יהוה came to
          Haggai the prophet, saying: 11 "This is what יהוה of armies says, 'Ask, please the priests about the law.'"</p>`
    },
  1001: {id: 1001, month: 10, day: 10, scripture: '2 Kings 25:1', title: 'Babylon came against Jerusalem',
      summary: `<p>2 Kings 25:1 In the ninth year of Zed-e-kiah's reign, in the tenth month, on the tenth day of the
          month, King Neb-u-chad-nez-zar of Babylon came with all his army against Jerusalem. He camped against it and
          built a siege wall all around it.</p>`}
}

BiblicalEventServiceInMemoryImpl.prototype.biblicalEventsByMonth = {}

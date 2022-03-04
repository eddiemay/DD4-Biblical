com.digitald4.biblical.ReadTheWordCtrl = function(globalData, scriptureService) {
  this.globalData = globalData;
  this.scriptureService = scriptureService;
  this.globalData.scriptureVersion = 'ISR';
  this.scriptureVersions = SCRIPTURE_VERSIONS;
  this.bibleBooks = [
      {name: 'Genesis', chapters: 50}, {name: 'Exodus', chapters: 40}, {name: 'Leviticus', chapters: 27},
      {name: 'Numbers', chapters: 36}, {name: 'Deuteronomy', chapters: 34}, {name: 'Joshua', chapters: 24}];
  this.refreshBooks();
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.refreshBooks = function() {
  this.scriptureService.getBibleBooks(bibleBooks => this.bibleBooks = bibleBooks.items, notifyError);
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.viewScripture = function() {
  this.book = this.book || '';
  var reference = (this.book + ' ' + this.verses).trim();
  this.scriptures = [{text: reference}];
  this.scriptureService.scriptures(reference, response => {
    this.scriptures = response.items || [];
    var previous;
    for (const script of this.scriptures) {
      if (!previous || previous.chapter != script.chapter || previous.verse + 1 != script.verse || previous.book != script.book) {
        script.showChapter = true;
      }
      previous = script;
    }
  }, notifyError);
  this.searchShown = undefined;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showScripture = function(version, book, chapter, verse) {
  this.reference = {book: book, chapter: chapter, verse: verse, version: version || this.globalData.scriptureVersion};
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showSearch = function() {
  this.searchShown = true;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.hideSearch = function() {
  this.searchShown = undefined;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.search = function(pageToken) {
  this.searchResult = undefined;
  var request = {'searchText': this.searchText, 'version': this.searchVersion, 'pageSize': 50, 'pageToken': pageToken};
  this.scriptureService.search(request, searchResult => {
    searchResult.grouped = [];
    var current;
    for (const result of searchResult.items) {
      if (current && current.book == result.book && current.chapter == result.chapter && current.verse == result.verse) {
        current.items.push(result);
      } else {
        current = {book: result.book, chapter: result.chapter, verse: result.verse, items: [result]};
        searchResult.grouped.push(current);
      }
    }
    this.searchResult = searchResult;
  }, notifyError);
}
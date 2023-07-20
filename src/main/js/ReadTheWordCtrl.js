com.digitald4.biblical.ReadTheWordCtrl = function($location, globalData, bookService, scriptureService) {
  this.locationProvider = $location;
  this.globalData = globalData;
  this.globalData.scriptureVersion = globalData.scriptureVersion || 'ISR';
  this.bookService = bookService;
  this.scriptureService = scriptureService;
  this.reference = {value: $location.search()['reference']};
  this.pageToken = $location.search()['pageToken'] || 1;
  this.language = $location.search()['lang'];
  this.views = {READ: 1, SEARCH_AND_REPLACE: 2, UPLOAD: 3};
  this.view = this.views.READ;
  this.showReference();
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.getOrSearch = function(page) {
  this.locationProvider.search('reference', this.reference.value);
  this.locationProvider.search('version', this.globalData.scriptureVersion);
  this.locationProvider.search('lang', this.globalData.language);
  this.locationProvider.search('pageToken', page);
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.setReference = function(reference) {
  this.locationProvider.search('reference', reference);
  this.locationProvider.search('version', this.globalData.scriptureVersion);
  this.locationProvider.search('lang', this.globalData.language);
  this.locationProvider.search('pageToken', undefined);
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showScripture = function(version, book, chapter, verse) {
  this.globalData.reference = {book: book, chapter: chapter, verse: verse,
      version: version || this.globalData.scriptureVersion, lang: this.globalData.language};
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showReference = function() {
  if (!this.reference.value) {
    return;
  }
  var request = {
    'searchText': this.reference.value,
    'version': this.globalData.scriptureVersion,
    'lang': this.language,
    'pageSize': 50,
    'pageToken': this.pageToken};
  this.scriptureService.search(request, response => {
    if (response.resultType == 'GET') {
      this.processScriptureResult(response);
    } else {
      this.processSearchResult(response);
    }
  });

  this.scriptures = [{text: this.reference.value}];
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.processScriptureResult = function(scriptureResult) {
  this.prevChapter = scriptureResult.prevChapter;
  this.scriptures = scriptureResult.items || [];
  this.nextChapter = scriptureResult.nextChapter;
  var previous;
  for (const script of this.scriptures) {
    if (!previous || previous.chapter != script.chapter || previous.verse + 1 != script.verse || previous.book != script.book) {
      script.showChapter = true;
    }
    script.dir = script.locale == 'he' ? 'rtl' : 'ltr';
    previous = script;
  }
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.processSearchResult = function(searchResult) {
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
  this.searchShown = true;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.searchAndReplace = function(preview) {
  var request = {'phrase': this.phrase, 'replacement': this.replacement, 'filter': this.filter, 'preview': preview};
  this.scriptureService.searchAndReplace(request, searchResult => {
    this.processSearchResult(searchResult);
    this.previewShown = preview ? true : undefined;
  });

  this.scriptures = [{text: this.phrase + ' ' + this.filter}];
  this.searchShown = undefined;
  this.previewShown = undefined;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.showUpload = function() {
  if (!this.allBooks) {
    this.bookService.getBibleBooks(true, bibleBooks => {
      this.allBooks = [];
      for (var b = 0; b < bibleBooks.length; b++) {
        if (bibleBooks[b].unreleased) {
          this.allBooks.push(bibleBooks[b]);
        }
      }
      this.allBooks.push({name: '-------------------------------'});
      [].push.apply(this.allBooks, bibleBooks);
    });
  }
  this.view = this.views.UPLOAD;
}

com.digitald4.biblical.ReadTheWordCtrl.prototype.uploadScripture = function(preview) {
  var bookName = this.uploadBook.name
  var request = {'version': this.uploadVersion, 'book': bookName, 'chapter': this.uploadChapter,
      'text': this.uploadText, 'preview': preview};
  this.scriptureService.uploadScripture(request, searchResult => {
    this.processScriptureResult(searchResult);
    this.previewShown = preview ? true : undefined;
    if (!preview) {
      this.uploadText = '';
      this.uploadChapter++;
    }
  });

  this.scriptures =
      [{text: 'Uploading to: ' + this.uploadVersion + ' ' + bookName + ' ' + this.uploadChapter}];
  this.searchShown = undefined;
  this.previewShown = undefined;
}

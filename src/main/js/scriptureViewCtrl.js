com.digitald4.biblical.ScriptureViewCtrl = function(scriptureService) {
  this.scriptureService = scriptureService;
  this.scriptureVersions = SCRIPTURE_VERSIONS;
  this.refresh();
}

com.digitald4.biblical.ScriptureViewCtrl.prototype.refresh = function() {
  if (typeof(this.reference) == 'object') {
    this.showScripture(this.reference.version, this.reference.book, this.reference.chapter, this.reference.verse);
  } else {
    this.scriptureService.scriptures(this.reference, response => this.scriptures = response.items, notifyError);
  }
}

com.digitald4.biblical.ScriptureViewCtrl.prototype.showScripture = function(version, book, chapter, verse) {
  this.reference.version = version;
  this.reference.book = book;
  this.reference.chapter = chapter;
  this.reference.verse = verse;

  var searchText = 'book="' + book + '" chapter=' + chapter + (verse ? ' verse=' + verse : ' version=' + version);
  this.scriptureService.search({'searchText': searchText, 'pageSize': 200},
      searchResult => this.scriptures = searchResult.items || [],
      notifyError);
}

com.digitald4.biblical.ScriptureViewCtrl.prototype.closeDialog = function() {
  this.reference = undefined;
}
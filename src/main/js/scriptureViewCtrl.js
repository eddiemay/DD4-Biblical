com.digitald4.biblical.ScriptureViewCtrl = function($window, globalData, scriptureService) {
  this.style = {top: $window.visualViewport.pageTop - 20};
  this.globalData = globalData;
  this.scriptureService = scriptureService;
  this.refresh();
}

com.digitald4.biblical.ScriptureViewCtrl.prototype.refresh = function() {
  if (this.reference.book != undefined) {
    this.showScripture(this.reference.version, this.reference.book, this.reference.chapter, this.reference.verse);
  } else {
    this.reference.version = this.reference.version || this.globalData.scriptureVersion;
    this.scriptureService.scriptures(this.reference, response => this.scriptures = response.items, notifyError);
  }
}

com.digitald4.biblical.ScriptureViewCtrl.prototype.showScripture = function(version, book, chapter, verse) {
  this.reference.version = version;
  this.reference.book = book;
  this.reference.chapter = chapter;
  this.reference.verse = verse;

  var request = {reference: book + ' ' + chapter + (verse ? ':' + verse : ''), version: verse ? undefined : version};
  this.scriptureService.scriptures(request, response => this.scriptures = response.items, notifyError);
}

com.digitald4.biblical.ScriptureViewCtrl.prototype.closeDialog = function() {
  this.reference = undefined;
}
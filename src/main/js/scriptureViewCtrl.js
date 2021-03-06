com.digitald4.biblical.ScriptureViewCtrl = function(scriptureService, $window) {
  this.scriptureService = scriptureService;
  this.scriptureVersions = SCRIPTURE_VERSIONS;
  this.refresh();
  $window.scrollTo(0, 0);
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

  var request = {'reference': book + ' ' + chapter + (verse ? ':' + verse : ''), 'version': verse ? undefined : version};
  this.scriptureService.scriptures(request, response => this.scriptures = response.items, notifyError);
}

com.digitald4.biblical.ScriptureViewCtrl.prototype.closeDialog = function() {
  this.reference = undefined;
}
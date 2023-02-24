com.digitald4.biblical.InlineScriptureCtrl = function(scriptureService, globalData) {
  this.scriptureService = scriptureService;
  this.globalData = globalData;
  this.refresh();
}

com.digitald4.biblical.InlineScriptureCtrl.prototype.refresh = function() {
  this.loading = true;
  var request = {reference: this.ref, version: this.version || this.globalData.scriptureVersion};

  this.scriptureService.scriptures(request, response => {
    if (this.isGrouped === false) {
      this.scriptures = response.items;
    } else {
      var scriptures = response.items || [];
      this.scriptureGroups = [];
      var group;
      for (const script of scriptures) {
        if (!group || group.book != script.book || group.chapter != script.chapter || group.curVer + 1 != script.verse) {
          group = {book: script.book, chapter: script.chapter, verse: script.verse, scriptures: []};
          this.scriptureGroups.push(group);
        }
        group.scriptures.push(script);
        group.curVer = script.verse;
      }
    }
    this.loading = undefined;
  });
}

com.digitald4.biblical.InlineScriptureCtrl.prototype.showScripture = function(version, book, chapter, verse) {
  this.globalData.reference = {book: book, chapter: chapter, verse: verse, version: version || this.globalData.scriptureVersion};
}
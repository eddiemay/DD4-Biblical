com.digitald4.biblical.InlineScriptureCtrl = function(scriptureService, globalData) {
  this.scriptureService = scriptureService;
  this.globalData = globalData;
  this.refresh();
}

com.digitald4.biblical.InlineScriptureCtrl.prototype.refresh = function() {
  this.loading = true;
  var request = {reference: this.ref, lang: this.language,
      version: this.version || this.globalData.scriptureVersion};

  this.scriptureService.scriptures(request, response => {
    var scriptures = processScriptures(response.items)
    if (this.isGrouped === false) {
      this.scriptures = scriptures;
    } else {
      this.scriptureGroups = [];
      var group;
      for (const script of scriptures) {
        if (!group || group.version != script.version || group.book != script.book
            || group.language != script.language || group.chapter != script.chapter
            || group.curVer + 1 != script.verse || group.scriptures.length == 5) {
          group = {version: script.version, book: script.book, language: script.language,
              chapter: script.chapter, verse: script.verse, dir: script.dir, scriptures: []};
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
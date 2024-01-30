com.digitald4.biblical.BiblicalCtrl = function($location, apiConnector, globalData) {
  apiConnector.baseUrl = 'https://dd4-biblical.appspot.com/';
  globalData.runningLocal = $location.host() == 'localhost';
  this.globalData = globalData;
  this.globalData.scriptureVersions =
      ['ISR', 'RSKJ', 'NRSV', 'NWT', 'KJV1611', 'Sefaria', 'Interlinear'];
  this.globalData.languages = [{name: 'English', code: 'en'},
      {name: 'Interlaced', code: 'interlaced'}, {name: 'Hebrew', code: 'he'},
      {name: 'Pictograph', code: 'he-A'}, {name: 'Greek', code: 'gk'}];
  globalData.scriptureVersion = $location.search()['version'] || globalData.scriptureVersion;
  globalData.language = $location.search()['lang'] || this.globalData.languages[0].code;
  globalData.matchConstantsOnly = true;
}

com.digitald4.biblical.BiblicalCtrl.prototype.showReference = function(reference) {
  this.globalData.reference = {reference: reference, version: this.globalData.scriptureVersion};
}

com.digitald4.biblical.BiblicalCtrl.prototype.showLoginDialog = function() {
  this.loginDialogShown = true;
}

com.digitald4.biblical.BiblicalCtrl.prototype.hideLoginDialog = function() {
  this.loginDialogShown = undefined;
}

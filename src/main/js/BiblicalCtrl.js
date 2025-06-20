com.digitald4.biblical.BiblicalCtrl = function($location, apiConnector, globalData) {
  this.globalData = globalData;
  if ($location.host() == 'localhost') apiConnector.baseUrl = 'https://dd4-biblical.appspot.com/';
  globalData.runningLocal = $location.host() == 'localhost';
  globalData.scriptureVersions = ['ISR', 'RSKJ', 'NRSV', 'NWT', 'KJV1611', 'Sefaria', 'SEP', 'DSS'];
  globalData.languages = [{name: 'English', code: 'en'},
      {name: 'Hebrew', code: 'he'}, {name: 'Hebrew Haser', code: 'he-haser'},
      {name: 'Restored Hebrew', code: 'he-re'}, {name: 'Pictograph', code: 'anc-he'},
      {name: 'Greek', code: 'gk'}, {name: 'Geez', code: 'gez'}];
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

com.digitald4.biblical.BiblicalCtrl = function($location, apiConnector, globalData) {
  apiConnector.baseUrl = 'https://dd4-biblical.appspot.com/_api/';
  globalData.runningLocal = $location.host() == 'localhost';
  this.globalData = globalData;
  this.globalData.scriptureVersions = ['ISR', 'RSKJ', 'NRSV', 'NWT', /*'NKJV',*/ 'KJV1611'];
  globalData.scriptureVersion = $location.search()['version'] || globalData.scriptureVersion;
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

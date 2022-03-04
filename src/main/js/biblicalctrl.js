com.digitald4.biblical.BiblicalCtrl = function(apiConnector) {
  apiConnector.baseUrl = 'https://dd4-biblical.appspot.com/_api/';
}

com.digitald4.biblical.BiblicalCtrl.prototype.showLoginDialog = function() {
  this.loginDialogShown = true;
}

com.digitald4.biblical.BiblicalCtrl.prototype.hideLoginDialog = function() {
  this.loginDialogShown = undefined;
}

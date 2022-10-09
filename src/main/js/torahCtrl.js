com.digitald4.biblical.TorahCtrl = function($location, globalData, commandmentService) {
  this.locationProvider = $location;
  this.commandmentService = commandmentService;
  this.userLoggedIn = () => globalData.activeSession != undefined;
  globalData.scriptureVersion = $location.search()['version'] || globalData.scriptureVersion || 'ISR';
  this.searchText = $location.search()['searchText'] || '';
  this.pageToken = parseInt($location.search()['pageToken']) || 1;
  this.newCommand = {};
  this.pageSize = 50;
  var request = {searchText: this.searchText, pageSize: this.pageSize, pageToken: this.pageToken, orderBy: this.orderBy};
  this.commandmentService.search(request, searchResult => this.searchResult = searchResult, notify);
}

com.digitald4.biblical.TorahCtrl.prototype.search = function(pageToken) {
  this.locationProvider.search('searchText', this.searchText);
  this.locationProvider.search('pageToken', pageToken);
}

com.digitald4.biblical.TorahCtrl.prototype.create = function() {
  this.commandmentService.create(this.newCommand, newCommand => {
    this.searchResult.items.push(newCommand);
    this.searchResult.end++;
    this.searchResult.totalSize++;
    this.newCommand.summary = '';
    this.newCommand.scriptures = '';
  }, notify);
}

com.digitald4.biblical.TorahCtrl.prototype.edit = function(commandment) {
  commandment.isEditing = true;
}

com.digitald4.biblical.TorahCtrl.prototype.update = function(commandment) {
  this.commandmentService.update(commandment, ['summary', 'scriptures', 'tags'], updated => {
    commandment.isEditing = undefined;
    commandment.summary = updated.summary;
    commandment.scriptures = updated.scriptures;
    commandment.tags = updated.tags;
  }, notify);
}

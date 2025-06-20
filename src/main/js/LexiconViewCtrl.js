com.digitald4.biblical.LexiconViewCtrl =
    function($location, $window, globalData, interlinearService, lexiconService, tokenWordService) {
  this.locationProvider = $location;
  this.style = {top: $window.visualViewport.pageTop - 20};
  this.globalData = globalData;
  this.interlinearService = interlinearService;
  this.lexiconService = lexiconService;
  this.tokenWordService = tokenWordService;
  this.showStrongsDef(this.lexiconRequest.strongsId);
  if (this.lexiconRequest.view == 'REFERENCES') {
    this.showStrongsRefs();
  }
}

com.digitald4.biblical.LexiconViewCtrl.prototype.showStrongsDef = function(strongsId, view) {
  this.strongsId = strongsId || this.strongsId;
  if (this.strongsId == undefined) {return;}
  this.lexicon = {id: this.strongsId, word: 'Loading...', strongsDefinition: '', rootWord: ''};
  this.lexiconTranslations = [{translation: 'Loading...'}];
  this.lexiconService.get(this.strongsId, lexicon => this.lexicon = lexicon);
  this.tokenWordService.getTranslations(this.strongsId, response => {this.lexiconTranslations = response.items});
  this.addTokenWord = {strongsId: strongsId, word: '', translation: ''};
  this.view = view || 'INFO';
}

com.digitald4.biblical.LexiconViewCtrl.prototype.prevStrongsDef = function() {
  this.showStrongsDef(this.strongsId.substring(0, 1) + (parseInt(this.strongsId.substring(1)) - 1));
}

com.digitald4.biblical.LexiconViewCtrl.prototype.nextStrongsDef = function() {
  this.showStrongsDef(this.strongsId.substring(0, 1) + (parseInt(this.strongsId.substring(1)) + 1));
}

com.digitald4.biblical.LexiconViewCtrl.prototype.fillReferenceCount = function() {
  this.lexiconService.fillReferenceCount(this.strongsId, referenceCount => {
    this.lexicon.referenceCount = referenceCount;
  });
}

com.digitald4.biblical.LexiconViewCtrl.prototype.addTranslation = function() {
  this.tokenWordService.create(this.addTokenWord, tokenWord => {
    this.lexiconTranslations.push(tokenWord);
    this.addTokenWord.word = '';
    this.addTokenWord.translation = '';
  });
}

com.digitald4.biblical.LexiconViewCtrl.prototype.showStrongsRefs = function(page) {
  this.interlinearService.getReferences({strongsId: this.strongsId, pageToken: page}, response => {
    this.references = this.interlinearService.processPagination(response);
  });
  this.view = 'REFERENCES';
}

com.digitald4.biblical.LexiconViewCtrl.prototype.showScripture = function(version, book, chapter, verse) {
  this.globalData.reference = {book: book, chapter: chapter, verse: verse,
      version: version || this.globalData.scriptureVersion, lang: this.globalData.language};
}

com.digitald4.biblical.LexiconViewCtrl.prototype.setReference = function(reference) {
  this.locationProvider.search('reference', reference);
  this.locationProvider.search('version', this.globalData.scriptureVersion);
  this.locationProvider.search('lang', this.globalData.language);
  this.locationProvider.search('view', 'Interlinear');
  this.locationProvider.search('pageToken', undefined);
  this.locationProvider.search('strongsId', this.strongsId);
  // this.locationProvider.search('lexiconView', this.view);
}

com.digitald4.biblical.LexiconViewCtrl.prototype.search = function(page) {
  this.lexiconService.search({searchText: this.searchText, pageToken: page, orderBy: 'strongsId'},
      response => this.searchResults = response);
}

com.digitald4.biblical.LexiconViewCtrl.prototype.closeDialog = function() {
  this.lexiconRequest.strongsId = false;
}
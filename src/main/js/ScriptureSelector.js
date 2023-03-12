com.digitald4.biblical.ScriptureSelector = function(globalData, bookService) {
  this.globalData = globalData;
  this.bookService = bookService;
  this.bookService.getBibleBooks(bibleBooks => this.bibleBooks = bibleBooks);
  this.view = 'book';
  this.reference.toString = undefined;
}

com.digitald4.biblical.ScriptureSelector.prototype.updateToString = function() {
  var reference = this.reference;
  if (!reference.book) {
    return;
  }
  reference.toString = reference.book.name;

  if (!reference.chapter) {
    return;
  }
  reference.toString = reference.toString + ' ' + reference.chapter.number;

  if (!reference.startVerse) {
    return;
  }
  reference.toString = reference.toString + ':' + reference.startVerse;

  if (!reference.endVerse) {
    return;
  }
  reference.toString = reference.toString + '-' + reference.endVerse;
}

com.digitald4.biblical.ScriptureSelector.prototype.setBook = function(book) {
  this.reference.book = book;
  this.reference.chapter = undefined;
  this.reference.startVerse = undefined;
  this.reference.endVerse = undefined;
  this.view = 'chapter';
  this.updateToString();
}

com.digitald4.biblical.ScriptureSelector.prototype.setChapter = function(chapter) {
  this.reference.chapter = chapter;
  this.reference.startVerse = undefined;
  this.reference.endVerse = undefined;
  this.view = 'startVerse';
  if (!chapter.verses) {
    this.bookService.getVerseCount(
        this.reference.book.name, chapter.number, verseCount => chapter.verseCount = verseCount);
  }
  this.updateToString();
}

com.digitald4.biblical.ScriptureSelector.prototype.setVerse = function(verse) {
  if (this.view == 'startVerse') {
    this.reference.startVerse = verse;
    this.reference.endVerse = undefined;
    this.view = 'endVerse';
    this.updateToString();
  } else {
    this.reference.endVerse = verse;
    this.updateToString();
    this.closeDialog();
  }
}

com.digitald4.biblical.ScriptureSelector.prototype.closeDialog = function() {
  this.reference.value = this.reference.toString || this.reference.value;
  this.reference.shown = undefined;
}
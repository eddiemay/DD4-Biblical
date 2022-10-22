com.digitald4.biblical.LessonsCtrl = function($location, globalData, lessonService, scriptureService) {
  this.locationProvider = $location;
  this.lessonService = lessonService;
  this.scriptureService = scriptureService;
  globalData.scriptureVersion = $location.search()['version'] || globalData.scriptureVersion || 'RSKJ';
  lessonService.listLessons(response => {this.lessons = response.items}, notify);
  this.lessonId = $location.search()['lesson'];
  if (this.lessonId) {
    this.lessonService.latest(this.lessonId, lesson => { this.renderLesson(lesson) }, notify);
  }
}

com.digitald4.biblical.LessonsCtrl.prototype.showLesson = function(lesson) {
  this.locationProvider.search('lesson', lesson.id);
}

com.digitald4.biblical.LessonsCtrl.prototype.createNew = function() {
  this.lesson = {theme: {}};
  this.isEditing = true;
}

com.digitald4.biblical.LessonsCtrl.prototype.edit = function() {
  this.isEditing = true;
}

com.digitald4.biblical.LessonsCtrl.prototype.saveLesson = function() {
  if (this.lesson.lessonId) {
    this.lesson.id = this.lesson.lessonId;
    this.lessonService.update(
        this.lesson,
        ['title', 'theme', 'youtubeId', 'content'],
        lesson => { this.renderLesson(lesson) },
        notify);
  } else {
    this.lessonService.create(this.lesson, lesson => { this.renderLesson(lesson) }, notify);
  }
}

com.digitald4.biblical.LessonsCtrl.prototype.showScripture = function(scripture) {
  // $event.stopPropagation();
  // notify(scripture);
  this.reference = scripture;
}

com.digitald4.biblical.LessonsCtrl.prototype.renderLesson = function(lesson) {
  if (lesson.theme.scripture) {
    this.scriptureService.scriptures(
        lesson.theme.scripture,
        response => { lesson.theme.scriptureExpanded = this.expandScriptureInline(response.items) },
        notify);
  }
  this.lesson = lesson;
  this.isEditing = undefined;
}

com.digitald4.biblical.LessonsCtrl.prototype.expandScriptureInline = function(scriptures) {
  var scriptureExpanded = '';
  var book = '';
  var chapter = -1;
  for (var i = 0; i < scriptures.length; i++) {
    var scripture = scriptures[i];
    if (book != scripture.book) {
      book = scripture.book;
      chapter = -1;
      scriptureExpanded += ' ' + book;
    }
    if (chapter != scripture.chapter) {
      chapter = scripture.chapter;
      scriptureExpanded += ' ' + chapter + ':' + scripture.verse;
    } else {
      scriptureExpanded += ' ' + scripture.verse;
    }
    scriptureExpanded += ' ' + scripture.text;
  }

  return scriptureExpanded.trim();
}
com.digitald4.biblical.LessonsCtrl = function($location, globalData, lessonService, scriptureService) {
  this.locationProvider = $location;
  this.lessonService = lessonService;
  this.scriptureService = scriptureService;
  globalData.scriptureVersion = globalData.scriptureVersion || 'RSKJ';
  var allowDraft = globalData.activeSession != undefined;
  lessonService.listLessons(allowDraft, response => {this.lessons = response.items}, notify);
  this.lessonId = $location.search()['lesson'];
  if (this.lessonId) {
    this.lessonService.latest(this.lessonId, allowDraft, lesson => { this.renderLesson(lesson) }, notify);
  }
}

com.digitald4.biblical.LessonsCtrl.prototype.showLesson = function(lesson) {
  if (lesson.lessonId) {
    this.locationProvider.search('lesson', lesson.lessonId);
  } else {
    this.locationProvider.search('lesson', lesson.id);
  }
}

com.digitald4.biblical.LessonsCtrl.prototype.createNew = function() {
  this.lesson = {};
  this.isEditing = true;
}

com.digitald4.biblical.LessonsCtrl.prototype.edit = function() {
  this.isEditing = true;
}

com.digitald4.biblical.LessonsCtrl.prototype.saveLesson = function(published) {
  this.lesson.published = published;
  if (this.lesson.lessonId) {
    this.lesson.id = this.lesson.lessonId;
    this.lessonService.update(
        this.lesson,
        ['title', 'themeText', 'youtubeId', 'content', 'published'],
        lesson => {this.renderLesson(lesson)},
        notify);
  } else {
    this.lessonService.create(this.lesson, lesson => {this.showLesson(lesson)}, notify);
  }
}

com.digitald4.biblical.LessonsCtrl.prototype.renderLesson = function(lesson) {
  this.lesson = lesson;
  this.isEditing = undefined;
}
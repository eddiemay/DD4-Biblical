com.digitald4.biblical.LessonsCtrl =
    function($location, $routeParams, $window, globalData, lessonService, scriptureService) {
  this.window = $window;
  this.lessonService = lessonService;
  this.scriptureService = scriptureService;
  this.lessonId = $routeParams.id || $location.search()['lesson'];
  globalData.scriptureVersion = globalData.scriptureVersion || 'NRSV';
  var allowDraft = globalData.activeSession != undefined;
  this.refresh(allowDraft);
}

com.digitald4.biblical.LessonsCtrl.prototype.refresh = function(allowDraft) {
  this.lessonService.listLessons(allowDraft, response => {
    this.lessons = response.items;
    for (var l = 0; l < this.lessons.length; l++) {
      var lesson = this.lessons[l];
      lesson.url = '#lessons/' + lesson.id + '/' + lesson.title.replaceAll(' ', '_').replace('?', '');
      if (allowDraft) {
        lesson.published = lesson.latestPublishedVersionId != undefined;
      }
    }
  });

  if (this.lessonId) {
    this.lessonService.latest(
        this.lessonId, allowDraft, lesson => {this.renderLesson(lesson)});
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
    var props = ['title', 'content', 'published'];
    if (this.lesson.themeText) { props.push('themeText'); }
    this.lessonService.update(this.lesson, props, lesson => {this.renderLesson(lesson)});
  } else {
    this.lessonService.create(this.lesson, lesson => {
      this.window.location.href = '#lessons/' + lesson.lessonId;
    });
  }
}

com.digitald4.biblical.LessonsCtrl.prototype.renderLesson = function(lesson) {
  this.lesson = lesson;
  this.isEditing = undefined;
}
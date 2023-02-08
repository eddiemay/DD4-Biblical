com.digitald4.biblical.LessonsCtrl = function($location, globalData, lessonService, scriptureService) {
  this.locationProvider = $location;
  this.lessonService = lessonService;
  this.scriptureService = scriptureService;
  this.lessonId = $location.search()['lesson'];
  globalData.scriptureVersion = globalData.scriptureVersion || 'RSKJ';
  var allowDraft = globalData.activeSession != undefined;
  this.refresh(allowDraft);
}

com.digitald4.biblical.LessonsCtrl.prototype.refresh = function(allowDraft) {
  this.lessonService.listLessons(allowDraft, response => {
    this.lessons = response.items;
    if (allowDraft) {
      for (var l = 0; l < this.lessons.length; l++) {
        this.lessons[l].published = this.lessons[l].latestPublishedVersionId != undefined;
      }
    }
  }, notifyError);

  if (this.lessonId) {
    this.lessonService.latest(
        this.lessonId, allowDraft, lesson => {this.renderLesson(lesson)}, notifyError);
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
    var props = ['title', 'content', 'published'];
    if (this.lesson.themeText) { props.push('themeText'); }
    if (this.lesson.youtubeId) { props.push('youtubeId'); }
    this.lessonService.update(this.lesson, props, lesson => {this.renderLesson(lesson)}, notifyError);
  } else {
    this.lessonService.create(this.lesson, lesson => {this.showLesson(lesson)}, notifyError);
  }
}

com.digitald4.biblical.LessonsCtrl.prototype.renderLesson = function(lesson) {
  this.lesson = lesson;
  this.isEditing = undefined;
}
com.digitald4.biblical.module = angular.module('biblical', ['DD4Common', 'ngRoute', 'ngSanitize', 'angular-bind-html-compile'])
  .config(com.digitald4.biblical.router)
  .filter('trusted', ['$sce', $sce => { return url => { return $sce.trustAsResourceUrl(url); }}])
  .service('biblicalEventService', function(apiConnector) {
    var biblicalEventService = new com.digitald4.common.JSONService('biblicalEvent', apiConnector);
    biblicalEventService.listCalendarEvents = function(month, success, error) {
      biblicalEventService.sendRequest({action: 'calendar_events', params: {month: month}}, success, error);
    }
    biblicalEventService.listTimelineEvents = function(startYear, endYear, success, error) {
      biblicalEventService.sendRequest(
          {action: 'timeline_events', params: {startYear: startYear, endYear: endYear}}, success, error);
    }
    biblicalEventService.listAll = function(success, error) {
      biblicalEventService.sendRequest({action: 'all'}, success, error);
    }
    // biblicalEventService = new BiblicalEventServiceInMemoryImpl();
    return biblicalEventService;
  })
  .service('bookService', function(apiConnector, globalData) {
    var bookService = new com.digitald4.common.JSONService('book', apiConnector);
    bookService.getBibleBooks = function(includeUnreleased, success, error) {
      if (!includeUnreleased && bookService.bibleBooks) {
        success(bookService.bibleBooks);
        return;
      } else if (includeUnreleased && bookService.allBooks) {
        success(bookService.allBooks);
        return;
      }
      var expandChapters = function(bibleBooks) {
        for (var b = 0; b < bibleBooks.length; b++) {
          bibleBooks[b].chapters = [];
          for (var c = 0; c < bibleBooks[b].chapterCount;) {
            bibleBooks[b].chapters[c] = {number: ++c};
          }
        }
        if (!includeUnreleased) {
          bookService.bibleBooks = bibleBooks;
        } else {
          bookService.allBooks = bibleBooks;
        }
        success(bibleBooks);
      }
      var request = {action: 'books', params: {includeUnreleased: includeUnreleased}};
      bookService.sendRequest(request, response => expandChapters(response.items), error);
    };
    bookService.getVerseCount = function(book, chapter, success, error) {
      bookService.sendRequest({action: 'verseCount', params: {'book': book, 'chapter': chapter}}, success, error);
    };

    return bookService;
  })
  .service('commandmentService', function(apiConnector) {
    return new com.digitald4.common.JSONService('commandment', apiConnector)
  })
  .service('familyTreeNodeService', function(apiConnector) {
    return new com.digitald4.common.JSONService('familyTreeNode', apiConnector)
  })
  .service('highScoreService', function(apiConnector) {
    var highScoreService = new com.digitald4.common.JSONService('highscore', apiConnector);
    highScoreService.list = function(game, config, pageSize, pageToken, success, error) {
      highScoreService.sendRequest(
      		{action: 'list', params: {game: game, config: config, pageSize: 20}}, success, error);
    };
    return highScoreService;
  })
  .service('holyDayService', function(apiConnector) {
    return new com.digitald4.common.JSONService('holyDay', apiConnector);
  })
  .service('interlinearService', function(apiConnector) {
    var interlinearService = new com.digitald4.common.JSONService('interlinear', apiConnector);
    interlinearService.getReferences = function(request, success, error) {
      return interlinearService.sendRequest({action: 'getReferences', params: request}, success, error);
    }
    return interlinearService;
  })
  .service('lessonService', function(apiConnector) {
    var lessonService = new com.digitald4.common.JSONService('lesson', apiConnector);
    lessonService.listLessons = function(allowDraft, success, error) {
      lessonService.sendRequest({action: 'lessons', params: {allowDraft: allowDraft}}, success, error);
    }
    lessonService.latest = function(id, allowDraft, success, error) {
      lessonService.sendRequest({action: 'latest', params: {id: id, allowDraft: allowDraft}}, success, error);
    }
    return lessonService;
  })
  .service('letterBoxService', function(apiConnector) {
    return new com.digitald4.common.JSONService('letterBox', apiConnector)
  })
  .service('lexiconService', function(apiConnector) {
    var lexiconService = new com.digitald4.common.JSONService('lexicon', apiConnector);
    lexiconService.fillReferenceCount = function(strongsId, success, error) {
      return lexiconService.sendRequest(
          {action: 'fillReferenceCount', params: {strongsId: strongsId}}, success, error);
    }
    return lexiconService;
  })
  .service('notificationService', function(apiConnector) {
    return new com.digitald4.common.JSONService('notification', apiConnector);
  })
  .service('scriptureService', function($http, apiConnector, globalData) {
    var scriptureService = new com.digitald4.common.JSONService('scripture', apiConnector);
    var dssByVerse;
    scriptureService.scriptures = function(reference, success, error) {
      var request = typeof(reference) == 'object'
          ? reference : {reference: reference, version: globalData.scriptureVersion};
      scriptureService.sendRequest({action: 'scriptures', params: request}, success, error);
    }
    scriptureService.search = function(request, success, error) {
      scriptureService.sendRequest({action: 'fetch', params: request},
          response => success(scriptureService.processPagination(response)), error);
    }
    scriptureService.searchAndReplace = function(request, success, error) {
      scriptureService.sendRequest({action: 'searchAndReplace', method: 'POST', params: request},
          response => success(scriptureService.processPagination(response)), error);
    }
    scriptureService.uploadScripture = function(request, success, error) {
      scriptureService.sendRequest({action: 'uploadScripture', method: 'POST', data: request},
          response => success(scriptureService.processPagination(response)), error);
    }
    scriptureService.getScrollCoords = function(scripture, success, error) {
      const errorCallback = error || notifyError;
      const url = 'http://dss.collections.imj.org.il/api/get_translation?id=' + scripture.chapter + '%3A' + scripture.verse;
      if (!dssByVerse) {
        dssByVerse = {};
        DSS_ISA_BY_COLUMN.forEach(column =>
            column.forEach(verse => dssByVerse['Isaiah ' + verse.chapter + ":" + verse.verse] = verse));
      }

      return success(dssByVerse[scripture.reference]);
    }
    // scriptureService = new ScriptureServiceInMemoryImpl();
    return scriptureService;
  })
  .service('sunRiseSetService', function(apiConnector) {
    return new com.digitald4.common.JSONService('sunRiseSet', apiConnector)
  })
  .service('timelineService', function(apiConnector) {
    var timelineService = new com.digitald4.common.JSONService('timeline', apiConnector);
    var events = {items: [
      {title: 'Life of Adam', year: 1, duration: 930, endYear: 931, month: 1, day: 6, scripture: 'Gen 1:26,31,5:5'},
      {title: 'Eve is born', year: 1, endYear: 1, month: 1, day: 13, scripture: 'Jub 3:1,4-6,8'},
      {title: 'Adam & Eve kicked out the garden', year: 8, endYear: 8, month: 2, day: 17, scripture: 'Jub 3:17-21'},
      {title: 'Life of Seth', offset: 130, year: 131, duration: 912, endYear: 1043, scripture: 'Gen 5:3,8'},
      {title: 'Life of Enosh', offset: 105, year: 236, duration: 905, endYear: 1141, scripture: 'Gen 5:6,11'},
      {title: 'Life of Kenan', offset: 90, year: 326, duration: 910, endYear: 1236, scripture: 'Gen 5:9,14'},
      {title: 'Life of Mahalalel', offset: 70, year: 396, duration: 895, endYear: 1291, scripture: 'Gen 5:12,17'},
      {title: 'Life of Jared', offset: 65, year: 461, duration: 962, endYear: 1423, scripture: 'Gen 5:15,20'},
      {title: 'Days of Enoch', offset: 162, year: 633, duration: 350, endYear: 983, scripture: 'Gen 5:18,23-24'},
      {title: 'Life of Methuʹselah', offset: 65, year: 698, duration: 969, endYear: 1667, scripture: 'Gen 5:21,27'},
      {title: 'Life of Laʹmech', offset: 187, year: 885, duration: 777, endYear: 1662, scripture: 'Gen 5:25,31'},
      {title: 'Life of Noah', offset: 182, year: 1067, duration: 950, endYear: 2017, scripture: 'Gen 5:28,9:29'},
      {title: 'Life of Shem', offset: 500, year: 1567, duration: 600, endYear: 2167, scripture: 'Gen 5:32,11:10-11'},
      {title: 'Start of Flood', offset: 600, year: 1667, endYear: 1667, scripture: 'Gen 7:6'},
    ]};
    timelineService.listEvents = function(year, endYear, success, error) {
      success(events);
    }
    timelineService.listAll = function(success, error) {
      success(events);
    }
    return timelineService;
  })
  .service('tokenWordService', function(apiConnector) {
    var tokenWordService = new com.digitald4.common.JSONService('tokenWord', apiConnector);
    tokenWordService.getTranslations = function(strongsId, success, error) {
      return tokenWordService.sendRequest(
          {action: 'getTranslations', params: {strongsId: strongsId}}, success, error);
    }
    return tokenWordService;
  })
  .controller('biblicalCtrl', com.digitald4.biblical.BiblicalCtrl)
  .directive('biblicalCalendar', ['$compile', function($compile) {
    return {
      controller: com.digitald4.biblical.CalendarCtrl,
      restrict: 'AE',
      templateUrl: 'js/html/calendar.html'
    };
  }])
  .component('inlineScripture', {
    controller: com.digitald4.biblical.InlineScriptureCtrl,
    bindings: {
      ref: '@',
      version: '@',
      language: '@',
    },
    templateUrl: 'js/html/inline_scripture.html',
  })
  .component('lexiconView', {
    controller: com.digitald4.biblical.LexiconViewCtrl,
    bindings: {
      lexiconRequest: '=',
    },
    templateUrl: 'js/html/lexicon_view.html',
  })
  .component('scripture', {
    controller: function(globalData) {
      this.label = this.label || this.ref;
      this.show = () => {globalData.reference = {reference: this.ref, version: this.version}}
    },
    bindings: {
      ref: '@',
      label: '@',
      version: '@',
    },
    template: '<a href="" data-ng-click="$ctrl.show()">{{$ctrl.label}}</a>',
  })
  .component('scriptureSelector', {
    controller: com.digitald4.biblical.ScriptureSelector,
    bindings: {
      reference: '=',
    },
    templateUrl: 'js/html/scripture_selector.html',
  })
  .component('scriptureView', {
    controller: com.digitald4.biblical.ScriptureViewCtrl,
    bindings: {
      reference: '=',
    },
    templateUrl: 'js/html/scripture_view.html',
  });

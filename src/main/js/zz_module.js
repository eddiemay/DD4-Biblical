com.digitald4.biblical.module = angular.module('biblical', ['DD4Common', 'ngRoute', 'ngSanitize', 'angular-bind-html-compile'])
  .config(com.digitald4.biblical.router)
  .filter('trusted', ['$sce', $sce => { return url => { return $sce.trustAsResourceUrl(url); }}])
  .service('biblicalEventService', function(apiConnector) {
    var biblicalEventService = new com.digitald4.common.JSONService('biblicalEvent', apiConnector);
    biblicalEventService.listByMonth = function(month, success, error) {
      biblicalEventService.sendRequest({action: 'forMonth', params: {month: month}}, success, error);
    };
    // biblicalEventService = new BiblicalEventServiceInMemoryImpl();
    return biblicalEventService;
  })
  .service('bookService', function(apiConnector, globalData) {
    var bookService = new com.digitald4.common.JSONService('book', apiConnector);
    bookService.getBibleBooks = function(success, error) {
      if (bookService.bibleBooks) {
        success(bookService.bibleBooks);
        return;
      }
      var expandChapters = function(bibleBooks) {
        for (var b = 0; b < bibleBooks.length; b++) {
          bibleBooks[b].chapters = [];
          for (var c = 0; c < bibleBooks[b].chapterCount;) {
            bibleBooks[b].chapters[c] = {number: ++c};
          }
        }
        bookService.bibleBooks = bibleBooks;
        success(bookService.bibleBooks);
      }

      var bibleBooks = [
        {name: 'Genesis', chapterCount: 50}, {name: 'Exodus', chapterCount: 40}, {name: 'Leviticus', chapterCount: 27},
        {name: 'Numbers', chapterCount: 36}, {name: 'Deuteronomy', chapterCount: 34}, {name: 'Joshua', chapterCount: 24},
        {name: 'Judges', chapterCount: 36}, {name: 'Ruth', chapterCount: 34}, {name: '1 Samuel', chapterCount: 34},
        {name: '2 Samuel', chapterCount: 24}, {name: '1 Kings', chapterCount: 36}, {name: '2 Kings', chapterCount: 34},
        {name: '1 Chronicles', chapterCount: 24}, {name: '2 Chronicles', chapterCount: 36}, {name: 'Ezra', chapterCount: 34},
        {name: 'Nehemiah', chapterCount: 24}, {name: 'Esther', chapterCount: 42}];

      expandChapters(bibleBooks);
      bookService.performRequest('GET', 'books', undefined, undefined, response => expandChapters(response.items), error);
    };
    bookService.getVerseCount = function(book, chapter, success, error) {
      var request = {'book': book, 'chapter': chapter};
      bookService.performRequest('GET', 'verseCount', request, undefined, success, error);
    };

    return bookService;
  })
  .service('commandmentService', function(apiConnector) {
    return new com.digitald4.common.JSONService('commandment', apiConnector)
  })
  .service('holyDayService', function(apiConnector) {
    return new com.digitald4.common.JSONService('holyDay', apiConnector);
  })
  .service('lessonService', function(apiConnector) {
    var lessons = [
      {id: 123, title: 'The Sabbath still remains', scripture: 'Hebrews 4:9',
        content: 'The Seventh day was made holy from the beginning of time: (Gen 2:3)'},
      {id: 124, title: 'How to observe the Sabbath', scripture: 'Exodus 20:9-10',
        content: 'We are not to do any work nor seek our on pleasure (Isa 58:17)'},
      {id: 125, title: 'Poetic reading of Ezekiel chapter 13',
        themeText: 'Woe to those that have been whitewashing (Ezekiel 13:14)',
        youtubeId: 'JmVKwppqnZQ',
        content: '<p>There is a strong message in Ezekiel chapter 13 for those that have been whitewashing the history and killing the people of the Most High</p>'},
      {id: 126, title: 'The Law is NOT done away with',
        themeText: '"Do not think I came to destroy the Law..." (<a href="" data-ng-click="$ctrl.showScripture(\'Matt 5:17\')">Matt 5:17</a>)',
        content: 'The Messiah told us to not even think he came destroy the Law, we are not even supposed to get that in our head. (<a href="" data-ng-click="$ctrl.showScripture(\'Matt 5:17\')">Matt 5:17</a>)'}]
    var lessonService = new com.digitald4.common.JSONService('lesson', apiConnector);
    lessonService.list = function(request, success, error) {
      success(processPagination({items: lessons}));
    }
    lessonService.get = function(id, success, error) {
      success(lessons[id - 123]);
    }
    lessonService.listLessons = function(success, error) {
      lessonService.performRequest('GET', 'list', {allowDraft: true}, undefined, success, error);
    }
    lessonService.latest = function(id, success, error) {
      lessonService.performRequest('GET', 'latest', {id: id, allowDraft: true}, undefined, success, error);
    }
    lessonService.update = function(lesson, updateMask, success, error) {
      lessonService.sendRequest(
          {method: 'PUT', urlParams: lesson.lessonId, params: {updateMask: updateMask.join()}, data: lesson}, success, error);
    }
    return lessonService;
  })
  .service('notificationService', function(apiConnector) {
    return new com.digitald4.common.JSONService('notification', apiConnector);
  })
  .service('scriptureService', function(apiConnector, globalData) {
    var scriptureService = new com.digitald4.common.JSONService('scripture', apiConnector);
    scriptureService.scriptures = function(reference, success, error) {
      var request =
          typeof(reference) == 'object' ? reference : {reference: reference, version: globalData.scriptureVersion};
      scriptureService.performRequest(['scriptures', 'GET'], undefined, request, undefined, success, error);
    };
    scriptureService.expand = function(html, includeLinks, success, error) {
      scriptureService.performRequest(['expand', 'POST'], undefined, undefined,
          {'html': html, 'includeLinks': includeLinks, 'version': globalData.scriptureVersion}, success, error);
    };
    scriptureService.searchAndReplace = function(request, success, error) {
      scriptureService.performRequest(['searchAndReplace', 'POST'], undefined, undefined, request,
          response => success(processPagination(response)), error);
    };
    // scriptureService = new ScriptureServiceInMemoryImpl();
    return scriptureService;
  })
  .service('sunRiseSetService', function(apiConnector) {
    return new com.digitald4.common.JSONService('sunRiseSet', apiConnector)
  })
  .controller('biblicalCtrl', com.digitald4.biblical.BiblicalCtrl)
  .directive('biblicalCalendar', ['$compile', function($compile) {
    return {
      controller: com.digitald4.biblical.CalendarCtrl,
      restrict: 'AE',
      scope: {
        config: '=',
        onUpdate: '&'
      },
      templateUrl: 'js/html/calendar.html'
    };
  }])
  .component('scripture', {
    controller: function(globalData) {
      this.show = () => {globalData.reference = {reference: this.ref}}
    },
    bindings: {
      ref: '@',
    },
    template: '<a href="" data-ng-click="$ctrl.show()">{{$ctrl.ref}}</a>',
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

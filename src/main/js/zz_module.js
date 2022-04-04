com.digitald4.biblical.module = angular.module('biblical', ['DD4Common', 'ngRoute', 'ngSanitize'])
  .config(com.digitald4.biblical.router)
  .service('biblicalEventService', function(apiConnector) {
    var biblicalEventService = new com.digitald4.common.JSONService('biblicalEvent', apiConnector);
    biblicalEventService.listByMonth = function(month, success, error) {
      biblicalEventService.performRequest('GET', {'month': month}, undefined, undefined, success, error);
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
      // success(chapter);
      var request = {'book': book, 'chapter': chapter};
      bookService.performRequest('GET', 'verseCount', request, undefined, verseCount => success(verseCount), error);
    };

    return bookService;
  })
  .service('commandmentService', function(apiConnector) {
    return new com.digitald4.common.JSONService('commandment', apiConnector)
  })
  .service('holyDayService', function(apiConnector) {
    return new com.digitald4.common.JSONService('holyDay', apiConnector);
  })
  .service('notificationService', function(apiConnector) {
    return new com.digitald4.common.JSONService('notification', apiConnector)
  })
  .service('scriptureService', function(apiConnector, globalData) {
    var scriptureService = new com.digitald4.common.JSONService('scripture', apiConnector);
    scriptureService.scriptures = function(reference, success, error) {
      var request =
          typeof(reference) == 'object' ? reference : {'reference': reference, 'version': globalData.scriptureVersion};
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
  .controller('BiblicalCtrl', com.digitald4.biblical.BiblicalCtrl)
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

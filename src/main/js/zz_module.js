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
    scriptureService.getBibleBooks = function(success, error) {
      scriptureService.performRequest('GET', 'books', undefined, undefined, success, error);
    };
    scriptureService.scriptures = function(reference, success, error) {
      scriptureService.performRequest(['scriptures', 'GET'], undefined,
          {'reference': reference, 'version': globalData.scriptureVersion}, undefined, success, error);
    };
    scriptureService.expand = function(html, includeLinks, success, error) {
      scriptureService.performRequest(['expand', 'POST'], undefined, undefined,
          {'html': html, 'includeLinks': includeLinks, 'version': globalData.scriptureVersion}, success, error);
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
  .component('scriptureView', {
    controller: com.digitald4.biblical.ScriptureViewCtrl,
    bindings: {
      reference: '=',
    },
    templateUrl: 'js/html/scripture_view.html',
  });

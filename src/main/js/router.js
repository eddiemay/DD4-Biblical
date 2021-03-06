com.digitald4.biblical.router = function($routeProvider) {
	$routeProvider
		.when('/home', {
			controller: com.digitald4.biblical.HomeCtrl,
			controllerAs: 'homeCtrl',
			templateUrl: 'js/html/home.html'
		}).when('/calendar', {
			controller: com.digitald4.biblical.CalendarCtrl,
			controllerAs: '$ctrl',
			templateUrl: 'js/html/calendar.html'
		}).when('/read_the_word', {
			controller: com.digitald4.biblical.ReadTheWordCtrl,
			controllerAs: '$ctrl',
			templateUrl: 'js/html/read_the_word.html'
		}).when('/the_law', {
			controller: com.digitald4.biblical.TorahCtrl,
			controllerAs: '$ctrl',
			templateUrl: 'js/html/the_law.html'
		}).when('/utils', {
			controller: com.digitald4.biblical.UtilsCtrl,
			controllerAs: '$ctrl',
			templateUrl: 'js/html/utils.html'
		}).otherwise({ redirectTo: '/calendar'});
}

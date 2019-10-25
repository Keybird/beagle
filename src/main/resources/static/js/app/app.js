/*
 * This file is part of Beagle.
 * Copyright (c) 2017 Markus von Rüden.
 *
 * Beagle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Beagle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Beagle. If not, see http://www.gnu.org/licenses/.
 */
var app = angular.module('beagleApp', ['ui.router', 'ui.bootstrap', 'ngStomp', 'angular-loading-bar', 'ngAnimate']);

app.constant('urls', {
});

app.config(['$stateProvider', '$urlRouterProvider', '$httpProvider',
    function($stateProvider, $urlRouterProvider, $httpProvider) {

        // Required by spring security to not sent authentication headers back to client (prevents browser's credentials dialog)
        $httpProvider.defaults.headers.common["X-Requested-With"] = 'XMLHttpRequest';

        // Intercept various responses (e.g. for 401/403 responses)
        $httpProvider.interceptors.push('InterceptorService');

        $stateProvider
            .state('home', {
                templateUrl: 'views/home.html',
                url: '/home'
            })
            .state('login', {
                templateUrl: 'views/login.html',
                controller: 'LoginController',
                url: '/login',
                params: {
                    session_expired: false
                }
            })
            .state('imports', {
                templateUrl: 'views/imports.html',
                controller: 'ImportController',
                url: '/imports'
            })
            .state('pages', {
                templateUrl: 'views/pages.html',
                controller: 'PageController',
                url: '/documents'
            })
            .state('jobs', {
                templateUrl: 'views/jobs.html',
                controller: 'JobController',
                url: '/jobs'
            })
            .state('search', {
                templateUrl: '/views/search.html',
                params: {
                    query: null
                },
                controller: 'SearchController',
                url: '/search'
            })
            .state('profile', {
                templateUrl: '/views/profile.html',
                controller: 'ProfileController',
                url: '/profile'
            })
            .state('admin', {
                templateUrl: 'views/admin.html',
                controller: 'AdminController',
                url: '/admin'
            });
        $urlRouterProvider.otherwise('/login');
    }]);

app.factory('InterceptorService',['$q', '$rootScope', '$injector', function($q, $rootScope, $injector) {
    return {
        responseError: function (rejection) {
            if (rejection.status === 401) {
                // When a login request is ongoing and a 401 is returned, it is simply ignored
                var AuthService = $injector.get("AuthService"); // Otherwise we have loop
                if (AuthService.authenticating === true && (rejection.config.url.startsWith("user") || rejection.config.url.startsWith("/user"))) {
                    // Login in progress. Do not interfere.
                } else {
                    console.error('Login Required', rejection, rejection.headers);
                    $rootScope.$emit('loginRequired');
                }
            }
            if (rejection.status === 403) {
                $rootScope.$emit('permissionDenied');
            }
            return $q.reject(rejection);
        }
    }
}]);

app.run(['$rootScope', '$state', 'AuthService', function($rootScope, $state, AuthService) {
    $rootScope.$on('loginRequired', function() {
        $state.go("login", { session_expired: true });
    });

    $rootScope.$on('permissionDenied', function() {
        console.log("Permission denied. Not yet implemented");
    });

    $rootScope.$on('$stateChangeStart', function (event, toState, toParams, fromState, fromParams)  {
        // In case the user is not authenticated (probably triggered by a page reload) and tries to access not the login page
        // the user profile is attempted to load. If this fails, it means the cookie is no longer valid and the user needs to login
        // However the user will then be redirected by the 401 interceptor instead
        if (AuthService.authenticating !== true && AuthService.isAuthenticated() === false && toState.name !== 'login') {
            AuthService.loadProfile({}, function() {
                if (!AuthService.isAuthenticated()) {
                    event.preventDefault();
                }
            });
        }
    });
}]);
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
'use strict';

angular.module('beagleApp')
        // TODO MVR implement check of "logged in" to every page. At the moment it is only executed when on login page
    .controller('LoginController',
        ['$scope', '$state', '$interval', 'AuthService',  function($scope, $state, $interval, AuthService) {

            $scope.credentials = {};
            $scope.login = function() {
                AuthService.authenticate($scope.credentials, function() {
                    console.log("Callback called", AuthService);
                    if (AuthService.authenticated === true) {
                        $scope.error = false;
                        console.log("go home");
                        $state.go('home');
                    } else {
                        console.log("authentication error");
                        $scope.error = true;
                    }
                })
            };

            // Handle login-image
            var images = [
                'beagle1.jpg',
                'beagle3.jpg',
                'beagle4.jpg',
                'beagle5.jpg',
                'beagle6.jpg',
                'beagle7.jpg',
                'beagle8.jpg',
            ];
            var imgIndex = Math.floor(Math.random()*images.length);
            $scope.bgImage = images[imgIndex];

            // change it after 60 seconds
            $scope.changeBackground = function(){
                imgIndex++;
                if (imgIndex >= images.length) {
                    imgIndex = 0;
                }
                $scope.bgImage = images[imgIndex];
            };
            $interval(function() {
                $scope.changeBackground();
            }, 60000);
            $scope.changeBackground();
        }
        ]);

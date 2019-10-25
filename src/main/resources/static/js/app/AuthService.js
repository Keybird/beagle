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
    .factory('AuthService',
        ['$rootScope', '$http', '$state', function($rootScope, $http, $state) {
            return {
                authenticating: false,
                authenticated: false,
                profile: {},

                authenticate : function(credentials, callback) {
                    var config = {};
                    if (credentials) {
                        config.headers = { authorization : "Basic " + btoa(credentials.email + ":" + credentials.password) };
                    }
                    this.authenticating = true;
                    this.loadProfile(config, callback);
                },
                logout: function() {
                    var self = this;
                    var handleLogout = function() {
                        self.authenticated = false;
                        $state.go("login");
                    };
                    $http.post('logout', {}).then(handleLogout);
                },
                isAuthenticated: function() {
                    return this.authenticated === true;
                },
                loadProfile: function(config, callback) {
                    var self = this;
                    $http.get('user', config)
                        .then(
                            function(response) {
                                self.authenticating = false;
                                if (response.data && response.data.id) {
                                    self.profile = response.data;
                                    self.authenticated = true;

                                    // if no image is defined, we assign the user one automatically
                                    if (!self.profile.image) {
                                        var numberOfDummyAvatars = 4;
                                        var avatarIndex = btoa(self.profile.name).charCodeAt(0) % (numberOfDummyAvatars - 1);
                                        avatarIndex++;
                                        self.profile.image = "/img/avatars/avatar" + avatarIndex + ".jpg";
                                    }
                                } else {
                                    self.profile = {};
                                    self.authenticated = false;
                                }
                                if (callback) {
                                    callback();
                                }
                            },
                            function() {
                                self.authenticating = false;
                                self.authenticated = false;
                                if (callback) {
                                    callback();
                                }
                            }
                        );
                }
            };
        }]
    );
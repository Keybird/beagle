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

describe('Beagle Tests', function() {

    var doLogin = function() {
        browser.get(browser.baseUrl);
        element(by.id("inputEmail")).clear().sendKeys("test@test.de");
        element(by.id("inputPassword")).clear().sendKeys("test");
        element(by.name("loginBtn")).click();
    };

    describe('Login', function() {
        it('is required', function() {
            browser.get(browser.baseUrl + "#!/jobs");

            expect(element(by.xpath("//h2")).getText()).toContain("Project Beagle");
            expect(element(by.id("session_expired")).getText()).toContain("session expired");
        });
    });

    describe('login page', function() {

        beforeEach(function() {
            browser.get(browser.baseUrl);
        });

        it('should load', function() {
            // Verify login page
            expect(element(by.name('loginBtn')).getText()).toBe("Login");
            expect(element(by.xpath("//h2")).getText()).toContain("Project Beagle");
        });

        it ('login button is disabled if form is not filled out', function() {
            element(by.id("inputEmail")).clear();
            element(by.id("inputPassword")).clear();
            expect(element(by.name("loginBtn")).isEnabled()).toBe(false);
        });

        it ('shows error when credentials not valid', function() {
            element(by.id("inputEmail")).clear().sendKeys("dummy");
            element(by.id("inputPassword")).clear().sendKeys("dummy");
            element(by.name("loginBtn")).click();

            expect(element(by.id("error")).getText()).toContain("There was a problem logging in");
        });

        it('works', function() {
            // Login
            doLogin();

            // Verify logged in
            expect(element(by.xpath("//main/h2")).getText()).toBe("Home");
        });
    });

    describe('User details', function() {
        var verifyUserDetails = function() {
            var xpathExpression = "//a[@class='nav-link dropdown-toggle']";
            expect(element(by.xpath(xpathExpression)).getText()).toContain("Test User");
            expect(element(by.xpath(xpathExpression + "/img")).getAttribute("src")).toContain("/img/avatars/avatar3.jpg");
        };

        it('are properly loaded', function() {
            doLogin();
            verifyUserDetails();
        });

        it('are properly loaded on page relod', function() {
            doLogin();
            verifyUserDetails();
            browser.refresh();
            verifyUserDetails();
        })
    });

    describe('profile page', function() {
        it('should load', function() {
            doLogin();

            // go to profile page
            element(by.id("user-controls")).click(); // toggle actions
            element(by.xpath("//div/ul/li/div/a[contains(text(), 'Profile')]")).click();

            // Verify
            expect(element(by.xpath("//main//div/h3[text()='Test User']")));
        })
    });

    describe('Detect Job', function() {
       it('should show progress', function() {
            element(by.id("refreshBtn")).click();
            var jobs = element.all(by.repeater("job in jobs"));
            expect(jobs.count()).toEqual(1);
       }) ;
    });

});
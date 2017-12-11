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

exports.config = {
    capabilities: {
	directConnect: true,
        browserName: 'chrome',
        chromeOptions: {
            args: ["--headless", "--no-sandbox", "--disable-gpu", "--window-size=1920,1080"]
        },
    },
    seleniumAddress: 'http://localhost:4444/wd/hub',
    specs: ['**/specs/*.js']
};

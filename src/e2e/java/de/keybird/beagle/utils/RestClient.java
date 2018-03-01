/**
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

package de.keybird.beagle.utils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class RestClient implements TestRule {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private String sessionId;
    private RequestSpecification spec;

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                log.info("## Executing {}() ##", description.getMethodName());
                try {
                    initRestAssured();
                    base.evaluate();
                } finally {
                    resetRestAssured();
                }
            }

        };
    }

    // TODO MVR make baseUri, username and password configurable by System.property
    private void initRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
        RestAssured.basePath = "/";

        // get session
        if (sessionId == null) {
            final Response response = RestAssured.given()
                    .auth()
                        .preemptive().basic("test@keybird.de", "test")
                    .contentType(ContentType.JSON)
                    .get("/user");
            if (response.getSessionId() == null) {
                throw new IllegalStateException("Login did not work. No session returned");
            }
            this.sessionId = response.getSessionId();

        }
        // Set session id for all requests
        this.spec = RestAssured.given().sessionId(sessionId);
    }

    private void resetRestAssured() {
        RestAssured.reset();
    }

    public DocumentEndpoint documents() {
        return new DocumentEndpoint(RestAssured.given(spec));
    }

    public JobEndpoint jobs() {
        return new JobEndpoint(RestAssured.given(spec));
    }

    public PageEndpoint pages() {
        return new PageEndpoint(RestAssured.given(spec));
    }
}

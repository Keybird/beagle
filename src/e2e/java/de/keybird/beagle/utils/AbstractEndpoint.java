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

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

class AbstractEndpoint<T> {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private String xsrfToken;
    private Class<T> type;
    protected RequestSpecification spec;

    public AbstractEndpoint(RequestSpecification spec, Class<T> type) {
        this.spec = Objects.requireNonNull(spec);
        this.type = type;
    }

    protected void acquireXsrfToken() {
        acquireXsrfToken(ContentType.JSON);
    }

    protected void acquireXsrfToken(ContentType contentType) {
        if (xsrfToken == null) {
            LOG.info("Acquire XSRF Token for ContentType {} ...", contentType);
            // get cookie
            final Response response = spec
                    .contentType(contentType)
                    .get();
            final String xsrfToken = response.cookie("XSRF-TOKEN");
            if (xsrfToken != null) {
                this.xsrfToken = xsrfToken;
                // Set session Id and XSRF-Token for all requests
                this.spec = spec.headers("X-XSRF-TOKEN", xsrfToken).cookie("XSRF-TOKEN", xsrfToken);
                LOG.info("XSRF Token received: {}", xsrfToken);
            }
        } else {
            LOG.warn("XSRF Token already acquired. Skipping");
        }
    }

    public List<T> list() {
        LOG.info("List entities {}", type.getSimpleName());
        final Response response = spec.get();
        response.then().assertThat()
                .contentType(ContentType.JSON)
                .statusCode(anyOf(is(200), is(204)));
        if (response.statusCode() == 204) {
            return new ArrayList<>();
        }
        final List<T> entities = response.jsonPath().getList("", type);
        return entities;
    }

    public T get(Long id) {
        LOG.info("Get entity {}", type.getSimpleName());
        final Response response = spec.get(Long.toString(id));
        if (response.statusCode() == 404) return null;
        response.then().assertThat().statusCode(200);
        final T entity = response.as(type);
        return entity;
    }

    public void delete() {
        LOG.info("Delete entities {}", type.getSimpleName());
        acquireXsrfToken();
        spec.delete().then().assertThat().statusCode(204);

        // Wait until it is actually deleted
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> assertThat(list(), hasSize(0)));
    }
}

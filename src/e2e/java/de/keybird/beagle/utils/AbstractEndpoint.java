/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018-2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package de.keybird.beagle.utils;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

class AbstractEndpoint<T> {

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

    protected void acquireXsrfToken(ContentType customContentType) {
        if (xsrfToken == null) {
            // get cookie
            final Response response = spec
                    .contentType(customContentType)
                    .get();
            final String xsrfToken = response.cookie("XSRF-TOKEN");
            if (xsrfToken != null) {
                this.xsrfToken = xsrfToken;
                // Set session Id and XSRF-Token for all requests
                this.spec = spec.headers("X-XSRF-TOKEN", xsrfToken).cookie("XSRF-TOKEN", xsrfToken);
            }
        }
    }

    public List<T> list() {
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
        final Response response = spec.get(Long.toString(id));
        if (response.statusCode() == 404) return null;
        response.then().assertThat().statusCode(200);
        final T entity = response.as(type);
        return entity;
    }
}

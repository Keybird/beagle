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

import java.io.InputStream;

import de.keybird.beagle.rest.model.PageDTO;
import io.restassured.specification.RequestSpecification;

public class PageEndpoint extends AbstractEndpoint<PageDTO> {

    public PageEndpoint(RequestSpecification spec) {
        super(spec, PageDTO.class);
        spec.basePath("pages");
    }

    public InputStream payload(Long id) {
        final InputStream inputStream = spec.get(Long.toString(id))
                .then().assertThat()
                .statusCode(200)
                .contentType("application/pdf")
                .extract().response().asInputStream();
        return inputStream;
    }

    public InputStream thumbnail(Long id) {
        final InputStream inputStream = spec.get(Long.toString(id) + "/thumbnail")
                .then().assertThat()
                .statusCode(200)
                .contentType("image/jpeg")
                .extract().response().asInputStream();
        return inputStream;

    }
}

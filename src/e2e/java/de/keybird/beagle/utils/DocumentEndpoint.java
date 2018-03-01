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

import de.keybird.beagle.rest.model.DocumentDTO;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public class DocumentEndpoint extends AbstractEndpoint<DocumentDTO> {

    public DocumentEndpoint(RequestSpecification spec) {
        super(spec, DocumentDTO.class);
        spec.basePath("imports");
    }

    public void Import(InputStream inputStream, String name) {
        acquireXsrfToken(ContentType.BINARY);
        spec.given()
                .log().headers()
                .contentType(ContentType.BINARY)
                .queryParam("name", name)
                .body(inputStream)
            .post()
                .then().assertThat()
                .statusCode(204);
    }

    public void delete() {
        acquireXsrfToken();
        spec.delete().then().assertThat().statusCode(204);
    }
}

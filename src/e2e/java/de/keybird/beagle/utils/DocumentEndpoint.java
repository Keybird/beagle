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
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.keybird.beagle.api.DocumentState;
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
        given(spec)
                .log().headers()
                .contentType(ContentType.BINARY)
                .queryParam("name", name)
                .body(inputStream)
            .post()
                .then().assertThat()
                .statusCode(204);
    }

    public void doImport(InputStream inputStream, String name, int expectedDocumentCount) {
        Import(inputStream, name);
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> {
                    final List<DocumentDTO> documents = list();
                    assertThat(documents, hasSize(expectedDocumentCount));
                    for (int i=0;i<expectedDocumentCount; i++) {
                        assertThat(DocumentState.Imported, is(documents.get(i).getState()));
                    }
                });
    }
}

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

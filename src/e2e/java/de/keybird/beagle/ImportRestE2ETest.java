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

package de.keybird.beagle;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertThat;

import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import de.keybird.beagle.utils.RestClient;

@Category(E2ETest.class)
public class ImportRestE2ETest {

    @Rule
    public Timeout timeout = new Timeout(5, TimeUnit.MINUTES);

    @Rule
    public RestClient client = new RestClient();

    @Before
    @After
    public void deleteAll() {
        client.jobs().delete();
        client.documents().delete();
        assertThat(client.documents().list(), Matchers.hasSize(0));
    }

    @Test
    public void verifyImport() {
        assertThat(client.documents().list(), Matchers.hasSize(0));
        client.documents().Import(getClass().getResourceAsStream("/Beagle.pdf"), "beagle.pdf");
        await().atMost(10, TimeUnit.SECONDS).until(() -> assertThat(client.documents().list(), Matchers.hasSize(1)));
    }

}

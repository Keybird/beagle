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
        doImport(client);
    }

    public static void doImport(RestClient client) {
        client.documents().Import(ImportRestE2ETest.class.getResourceAsStream("/Beagle.pdf"), "beagle.pdf");
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .until(() -> assertThat(client.documents().list(), Matchers.hasSize(1)));
    }

}

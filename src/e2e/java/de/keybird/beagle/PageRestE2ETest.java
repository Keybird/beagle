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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.Timeout;

import com.google.common.io.ByteStreams;

import de.keybird.beagle.rest.model.PageDTO;
import de.keybird.beagle.utils.RestClient;

@Category(E2ETest.class)
public class PageRestE2ETest {

    @Rule
    public Timeout timeout = new Timeout(5, TimeUnit.MINUTES);

    @Rule
    public RestClient client = new RestClient();

    @Before
    @After
    public void deleteAll() {
        client.jobs().delete();
        client.documents().delete();
        assertThat(client.documents().list(), hasSize(0));
        assertThat(client.pages().list(), hasSize(0));
    }

    @Test
    public void verifyList() {
        ImportRestE2ETest.doImport(client); // import something
        assertThat(client.pages().list(), hasSize(15));
    }

    @Test
    public void verifyPayload() throws IOException {
        verifyList();
        PageDTO page = client.pages().list().get(0);
        InputStream payloadStream = client.pages().payload(page.getId());
        verifyInputStream(payloadStream);
    }

    @Test
    public void verifyThumbnail() throws IOException {
        verifyList();
        PageDTO page = client.pages().list().get(0);
        InputStream imageStream = client.pages().thumbnail(page.getId());
        verifyInputStream(imageStream);
    }

    private static void verifyInputStream(InputStream inputStream) throws IOException {
        // Try to read fully
        byte[] bytes = new byte[4096];
        ByteStreams.readFully(inputStream, bytes);
    }
}

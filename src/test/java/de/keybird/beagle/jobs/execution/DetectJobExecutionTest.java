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

package de.keybird.beagle.jobs.execution;

import static de.keybird.beagle.jobs.execution.DetectJobExecutionTest.WORKING_DIRECTORY;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import de.keybird.beagle.BeagleTest;
import de.keybird.beagle.TestConfig;
import de.keybird.beagle.WorkingDirectory;
import de.keybird.beagle.api.Document;
import de.keybird.beagle.api.DocumentState;
import de.keybird.beagle.jobs.DetectJob;
import de.keybird.beagle.repository.DocumentRepository;
import de.keybird.beagle.repository.JobRepository;

@RunWith(SpringRunner.class)
@BeagleTest
@SpringBootTest(
    properties = {
        "working.directory=" + WORKING_DIRECTORY
    }
)
@Transactional
public class DetectJobExecutionTest {

    public static final String WORKING_DIRECTORY = "target/beagle-home";

    @Rule
    public WorkingDirectory inboxDirectory = new WorkingDirectory(WORKING_DIRECTORY+ "/1_inbox");

    @Autowired
    private DetectJobExecution detectJobExecution;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobRunner jobRunner;

    @Before
    public void setUp() {
        tearDown();

        assertEquals(0, documentRepository.count());
        assertEquals(0, jobRepository.count());
    }

    @After
    public void tearDown() {
        jobRepository.findAll().forEach(j -> jobRepository.delete(j));
        documentRepository.findAll().forEach(d -> documentRepository.delete(d));
    }

    @Test
    public void verifyJobExecution() throws Exception {
        jobRunner.execute(new DetectJob(), detectJobExecution);

        assertEquals(0, documentRepository.count());
        assertEquals(1, jobRepository.count());
    }

    @Test
    public void verifyImportsPdf() throws Exception {
        // Add a document to the inbox directory
        inboxDirectory.addFile(TestConfig.BEAGLE_DE_PDF_URL);

        // Detect new files
        jobRunner.execute(new DetectJob(), detectJobExecution);

        // Ensure the document was imported
        assertEquals(1, jobRepository.count());
        assertEquals(1, documentRepository.count());
        final Document document = documentRepository.findAll().iterator().next();
        assertThat(document.getFilename(), is(TestConfig.BEAGLE_DE_PDF_NAME));
        assertThat(document.getPageCount(), is(4));
        assertThat(document.getState(), is(DocumentState.New));
        assertThat(document.getErrorMessage(), nullValue());
    }

    @Test
    public void verifyDoesNotImportAlreadyImportedFile() throws Exception {
        // import the same file multiple times
        int N = 2;
        for (int i=0; i<N; i++) {
            // Add a document to the inbox directory
            inboxDirectory.addFile(TestConfig.BEAGLE_DE_PDF_URL);

            // Detect new files
            jobRunner.execute(new DetectJob(), detectJobExecution);
        }

        // Ensure it is only available once
        assertEquals(1, documentRepository.count());

        // Jobs should be 2
        assertEquals(2, jobRepository.count());
    }

    @Test
    public void verifyIgnoresNonPdfFiles() throws Exception {
        inboxDirectory.addFile(getClass().getResource("/static/img/beagles/beagle1.jpg"));
        jobRunner.execute(new DetectJob(), detectJobExecution);
        assertEquals(0, documentRepository.count());
        assertEquals(1, jobRepository.count());
    }
}
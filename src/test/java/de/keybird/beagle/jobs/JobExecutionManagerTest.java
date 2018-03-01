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

package de.keybird.beagle.jobs;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Provider;
import javax.persistence.EntityManager;

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
import de.keybird.beagle.jobs.execution.JobExecutionContext;
import de.keybird.beagle.jobs.execution.JobRunner;
import de.keybird.beagle.jobs.persistence.DetectJobEntity;
import de.keybird.beagle.jobs.persistence.ImportJobEntity;
import de.keybird.beagle.jobs.persistence.JobEntity;
import de.keybird.beagle.jobs.persistence.JobState;
import de.keybird.beagle.jobs.persistence.JobType;
import de.keybird.beagle.repository.DocumentRepository;
import de.keybird.beagle.repository.JobRepository;
import de.keybird.beagle.repository.PageRepository;

@RunWith(SpringRunner.class)
@BeagleTest
@SpringBootTest(properties = { "working.directory=" + TestConfig.WORKING_DIRECTORY })
public class JobExecutionManagerTest {

    @Rule
    public WorkingDirectory inboxDirectory = new WorkingDirectory(TestConfig.INBOX_DIRECTORY);

    @Autowired
    private JobExecutionManager jobExecutionManager;

    @Autowired
    private JobExecutionFactory jobExecutionFactory;

    @Autowired
    private Provider<JobExecutionContext> jobExecutionContextProvider;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityManager entityManager;

    @Before
    public void setUp() {
        entityManager.clear();
        jobExecutionManager.init();
        assertThat(jobExecutionManager.hasRunningJobs(), is(false));
        assertThat(jobRepository.count(), is(0L));
        assertThat(documentRepository.count(), is(0L));
        assertThat(pageRepository.count(), is(0L));
    }

    @After
    public void tearDown() {
        jobRepository.deleteAll();
        pageRepository.deleteAll();
        documentRepository.deleteAll();
    }

    // See https://github.com/Keybird/beagle/issues/1
    @Test(timeout=15000)
    public void verifyHasExecutions() {
        // Verify no execution currently in progress
        assertThat(jobExecutionManager.hasRunningJobs(), is(false));

        // Submit dummy job
        final JobExecutionContext<DetectJobEntity> jobExecutionContext = jobExecutionContextProvider.get();
        jobExecutionContext.setJobEntity(new DetectJobEntity());
        final JobRunner dummyJobExecution = new JobRunner<>(jobExecutionContext, context -> Thread.sleep(5000));
        jobExecutionManager.submit(dummyJobExecution);

        // Verify execution is in progress
        assertThat(jobExecutionManager.hasRunningJobs(), is(true));
        assertThat(jobExecutionManager.getExecutions(JobType.Detect), hasSize(1));

        // Wait until finished
        await().atMost(10, SECONDS).until(() -> !jobExecutionManager.hasRunningJobs());
        assertThat(jobExecutionManager.getExecutions(JobType.Detect), hasSize(0));
    }

    @Test(timeout=60000)
    public void verifyCanImport() throws Exception {
        // Add file to import
        inboxDirectory.addFile(TestConfig.BEAGLE_EN_PDF_URL);

        // Start detecting
        final CompletableFuture submittedJob = jobExecutionManager.submit(jobExecutionFactory.createDetectJobRunner());
        submittedJob.get();

        // Now Import and Index Jobs should run, we wait for them to finish
        await().atMost(30, SECONDS).pollInterval(5, SECONDS).until(() -> jobRepository.count() == 3);

        // Detect + Import + Index jobs should have run
        assertThat(jobRepository.count(), is(3L));

        // Verify Import Job
        final List<JobEntity> importJobs = jobRepository.findAll().stream().filter(job -> job.getType() == JobType.Import).collect(Collectors.toList());
        assertThat(importJobs, hasSize(1));
        assertThat(importJobs.get(0), instanceOf(ImportJobEntity.class));
        ImportJobEntity importJob = (ImportJobEntity) importJobs.get(0);
        assertThat(importJob.getState(), is(JobState.Completed));
        assertThat(importJob.getErrorMessage(), nullValue());
        assertThat(importJob.getDocument().getFilename(), is(TestConfig.BEAGLE_EN_PDF_NAME));
        assertThat(importJob.getDocument().getPageCount(), is(15));
        assertThat(importJob.getCompleteTime(), notNullValue());
        assertThat(importJob.getStartTime(), notNullValue());
        assertThat(importJob.getCreateTime(), notNullValue());
        assertThat(importJob.getStartTime(), greaterThanOrEqualTo(importJob.getCreateTime()));
        assertThat(importJob.getCompleteTime(), greaterThanOrEqualTo(importJob.getStartTime()));

        // Verify Document and Page verify briefly
        assertThat(documentRepository.count(), is(1L));
        assertThat(pageRepository.count(), is(15L));
    }

    @Test(timeout=20000)
    public void verifyJobRemovedProperlyOnSuccess() throws InterruptedException {
        final JobExecutionContext<DetectJobEntity> jobExecutionContext = jobExecutionContextProvider.get();
        jobExecutionContext.setJobEntity(new DetectJobEntity());
        final JobRunner<DetectJobEntity> runner = new JobRunner<>(jobExecutionContext, context -> {

        });
        jobExecutionManager.submit(runner);
        jobExecutionManager.shutdown();
        jobExecutionManager.awaitTermination(5, SECONDS);
        assertThat(jobExecutionManager.getExecutions(), hasSize(0));
    }

    @Test(timeout=20000)
    public void verifyJobRemovedProperlyOnError() throws InterruptedException {
        final JobExecutionContext<DetectJobEntity> jobExecutionContext = jobExecutionContextProvider.get();
        jobExecutionContext.setJobEntity(new DetectJobEntity());
        final JobRunner<DetectJobEntity> runner = new JobRunner<>(jobExecutionContext, context -> {
            throw new IllegalStateException("Some random exception");
        });
        jobExecutionManager.submit(runner);
        jobExecutionManager.shutdown();
        jobExecutionManager.awaitTermination(5, SECONDS);
        assertThat(jobExecutionManager.getExecutions(), hasSize(0));
    }
}





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

import javax.inject.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.keybird.beagle.api.Document;
import de.keybird.beagle.api.source.DocumentSource;
import de.keybird.beagle.api.source.FileSystemSource;
import de.keybird.beagle.jobs.execution.DetectJobExecution;
import de.keybird.beagle.jobs.execution.ImportJobExecution;
import de.keybird.beagle.jobs.execution.IndexJobExecution;
import de.keybird.beagle.jobs.execution.JobExecutionContext;
import de.keybird.beagle.jobs.execution.JobRunner;
import de.keybird.beagle.jobs.persistence.DetectJobEntity;
import de.keybird.beagle.jobs.persistence.ImportJobEntity;
import de.keybird.beagle.jobs.persistence.IndexJobEntity;

@Service
public class JobExecutionFactory {

    @Autowired
    private Provider<DetectJobExecution> detectJobExecutionProvider;

    @Autowired
    private Provider<ImportJobExecution> importJobExecutionProvider;

    @Autowired
    private Provider<IndexJobExecution> indexJobExecutionProvider;

    @Autowired
    private Provider<JobExecutionContext> jobExecutionContextProvider;

    public JobRunner<DetectJobEntity> createDetectJobRunner() {
      return createDetectJobRunner(new FileSystemSource());
    }

    public JobRunner<DetectJobEntity> createDetectJobRunner(DocumentSource documentSource) {
        final DetectJobExecution execution = detectJobExecutionProvider.get();
        final JobExecutionContext<DetectJobEntity> jobExecutionContext = jobExecutionContextProvider.get();
        jobExecutionContext.setJobEntity(new DetectJobEntity(documentSource));

        return new JobRunner<>(jobExecutionContext, execution);
    }

    public JobRunner<IndexJobEntity> createIndexJobRunner() {
        final IndexJobExecution execution = indexJobExecutionProvider.get();
        final JobExecutionContext<IndexJobEntity> jobExecutionContext = jobExecutionContextProvider.get();
        jobExecutionContext.setJobEntity(new IndexJobEntity());

        return new JobRunner<>(jobExecutionContext, execution);
    }

    public JobRunner<ImportJobEntity> createImportJobRunner(Document document) {
        final ImportJobExecution execution = importJobExecutionProvider.get();
        final JobExecutionContext<ImportJobEntity> jobExecutionContext = jobExecutionContextProvider.get();
        jobExecutionContext.setJobEntity(new ImportJobEntity(document));

        return new JobRunner<>(jobExecutionContext, execution);
    }

}

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

import de.keybird.beagle.jobs.execution.ArchiveJobExecution;
import de.keybird.beagle.jobs.execution.DetectJobExecution;
import de.keybird.beagle.jobs.execution.ImportJobExecution;
import de.keybird.beagle.jobs.execution.IndexJobExecution;
import de.keybird.beagle.jobs.execution.JobExecution;
import de.keybird.beagle.jobs.persistence.ArchiveJobEntity;
import de.keybird.beagle.jobs.persistence.DetectJobEntity;
import de.keybird.beagle.jobs.persistence.ImportJobEntity;
import de.keybird.beagle.jobs.persistence.IndexJobEntity;
import de.keybird.beagle.jobs.persistence.JobEntity;

@Service
public class JobExecutionFactory implements JobVisitor<JobExecution> {

    @Autowired
    private Provider<DetectJobExecution> detectJobExecutionProvider;

    @Autowired
    private Provider<ImportJobExecution> importJobExecutionProvider;

    @Autowired
    private Provider<IndexJobExecution> indexJobExecutionProvider;

    @Autowired
    private Provider<ArchiveJobExecution> archiveJobExecutionProvider;

    @Override
    public JobExecution<DetectJobEntity> visit(DetectJobEntity jobEntity) {
        return detectJobExecutionProvider.get();
    }

    @Override
    public JobExecution<IndexJobEntity> visit(IndexJobEntity jobEntity) {
        IndexJobExecution execution = indexJobExecutionProvider.get();
        return execution;
    }

    @Override
    public JobExecution<ImportJobEntity> visit(ImportJobEntity jobEntity) {
        return importJobExecutionProvider.get();
    }

    @Override
    public JobExecution visit(ArchiveJobEntity archiveJobEntity) {
        return archiveJobExecutionProvider.get();
    }

    public JobExecution getJobExecution(JobEntity jobEntity) {
        return jobEntity.accept(this);
    }
}
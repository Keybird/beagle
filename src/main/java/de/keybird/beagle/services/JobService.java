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

package de.keybird.beagle.services;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.keybird.beagle.api.DocumentState;
import de.keybird.beagle.jobs.JobExecutionFactory;
import de.keybird.beagle.jobs.JobExecutionManager;
import de.keybird.beagle.jobs.persistence.JobEntity;
import de.keybird.beagle.jobs.persistence.JobType;
import de.keybird.beagle.repository.DocumentRepository;
import de.keybird.beagle.repository.JobRepository;

@Service
public class JobService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private JobExecutionManager jobManager;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobExecutionFactory jobExecutionFactory;

    @Autowired
    private DocumentRepository documentRepository;

    @Transactional
    public void save(JobEntity jobEntity) {
        jobRepository.save(jobEntity);
    }

    @Transactional
    public void importDocuments() {
        // Kick of import of documents
        documentRepository
                .findByState(DocumentState.New)
                .forEach(document -> {
                    document.getPayload(); // lazy load property
                    jobManager.submit(jobExecutionFactory.createImportJobRunner(document));
                });
        indexPagesIfNecessary();
    }

    @Transactional
    public void indexPagesIfNecessary() {
        // Kick of index if we have imported documents
        if (!documentRepository.findByState(DocumentState.Imported).isEmpty()) {
            // After import kick of indexing, when no import job is running anymore
            boolean noImportJobsRunningAnymore = jobManager.getExecutions(JobType.Import).isEmpty();
            if (noImportJobsRunningAnymore && jobManager.getExecutions(JobType.Index).isEmpty()) {
                jobManager.submit(jobExecutionFactory.createIndexJobRunner());
            }
        }
    }
}
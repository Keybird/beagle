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

import static org.springframework.data.domain.Sort.Direction;

import javax.inject.Named;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import de.keybird.beagle.api.DocumentState;
import de.keybird.beagle.api.PageState;
import de.keybird.beagle.jobs.JobExecutionFactory;
import de.keybird.beagle.jobs.JobExecutionManager;
import de.keybird.beagle.jobs.persistence.JobEntity;
import de.keybird.beagle.jobs.ImportJob;
import de.keybird.beagle.jobs.IndexJob;
import de.keybird.beagle.jobs.JobType;
import de.keybird.beagle.repository.DocumentRepository;
import de.keybird.beagle.repository.JobRepository;
import de.keybird.beagle.repository.PageRepository;

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

    @Autowired
    private PageRepository pageRepository;

    @Value("${index.batchSize}")
    private int batchSize;

    @Autowired
    @Named("poolSize")
    private int poolSize;

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
                    jobManager.submit(new ImportJob(document));
                });
        indexPagesIfNecessary();
    }

    @Transactional
    public void indexPagesIfNecessary() {
        // Kick of index if we have imported documents
        if (documentRepository.countByState(DocumentState.Imported) > 0) {
            // After import kick of indexing, when no import job is running anymore
            boolean noImportJobsRunningAnymore = jobManager.getExecutions(JobType.Import).isEmpty();
            if (noImportJobsRunningAnymore && jobManager.getExecutions(JobType.Index).isEmpty()) {
                // If we have to index a lot of pages, we may run out of memory.
                // To prevent this, we kick of multiple jobs to index
                long pagesToIndex = pageRepository.countByState(PageState.Imported);
                int numberOfIndexJobs = (int) Math.ceil(pagesToIndex / (float) batchSize);

                // As only a certain number of jobs can run in parallel,
                // the indexes would not match anymore, when one job finished, and another is not yet started
                // therefore we only kick off as many index jobs as threads are available.
                int maxIndexJobs = Math.min(poolSize, numberOfIndexJobs);
                Pageable pageRequest = new PageRequest(0, batchSize, new Sort(Direction.ASC, "id"));
                for (int i=0; i<maxIndexJobs; i++) {
                    if (i > 0) {
                        pageRequest = pageRequest.next();
                    }
                    jobManager.submit(new IndexJob(pageRequest));
                }
            }
        }
    }
}
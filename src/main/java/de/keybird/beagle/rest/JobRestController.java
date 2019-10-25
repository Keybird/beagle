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

package de.keybird.beagle.rest;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.keybird.beagle.api.DocumentState;
import de.keybird.beagle.jobs.JobExecutionManager;
import de.keybird.beagle.jobs.Progress;
import de.keybird.beagle.jobs.execution.JobExecutionInfo;
import de.keybird.beagle.jobs.persistence.JobEntity;
import de.keybird.beagle.jobs.DetectJob;
import de.keybird.beagle.jobs.ImportJob;
import de.keybird.beagle.jobs.Job;
import de.keybird.beagle.repository.DocumentRepository;
import de.keybird.beagle.repository.JobRepository;
import de.keybird.beagle.rest.model.JobDTO;
import de.keybird.beagle.rest.model.JobExecutionDTO;
import de.keybird.beagle.services.JobService;

@RestController
@RequestMapping("/jobs")
public class JobRestController {

    @Autowired
    private JobExecutionManager jobExecutionManager;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listJobs() {
        final Iterable<JobEntity> all = jobRepository.findAll();
        if (!all.iterator().hasNext()) {
            return ResponseUtils.noContent();
        }
        // Convert entities to DTOs (let's us fine-tune what to expose)
        final List<JobDTO> entities = StreamSupport.stream(all.spliterator(), false)
                .map(e -> new JobDTO(e))
                .collect(Collectors.toList());
        return new ResponseEntity(entities, HttpStatus.OK);
    }

    @RequestMapping(path="/running", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<JobExecutionDTO>> showProgress() {
        final List<JobExecutionInfo> executions = jobExecutionManager.getExecutions();
        if (executions.isEmpty()) {
            return ResponseUtils.noContent();
        }
        final List<JobExecutionDTO> jobDTOList = executions.stream().map(jobExecution -> createFrom(jobExecution)).collect(Collectors.toList());
        jobDTOList.sort(Comparator.comparing(JobExecutionDTO::getId).reversed());
        return new ResponseEntity<>(jobDTOList, HttpStatus.OK);
    }

    @RequestMapping(path="/detect", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity startDetect() {
        jobExecutionManager.submit(new DetectJob());
        return ResponseEntity.accepted().build();
    }

    @RequestMapping(path="/import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity startImport() {
        // TODO MVR use service for this?
        documentRepository
            .findByState(DocumentState.New)
            .forEach(document -> {
                jobExecutionManager.submit(new ImportJob(document));
            });
        return ResponseEntity.accepted().build();
    }

    @RequestMapping(path="/index", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity startIndex() {
        jobService.indexPagesIfNecessary();
        return ResponseEntity.accepted().build();
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll() {
        // TODO MVR this is ugly but for now we keep it
        if (jobExecutionManager.hasRunningJobs()) {
            throw new IllegalStateException("Cannot delete jobs while jobs are running");
        }
        jobRepository.deleteAll();
    }

    public static JobExecutionDTO createFrom(JobExecutionInfo info) {
        final Job job = info.getJob();
        final JobExecutionDTO jobInfoDTO = new JobExecutionDTO();
        jobInfoDTO.setDescription(job.getDescription());
        jobInfoDTO.setCompleteTime(job.getCompleteTime());
        jobInfoDTO.setStartTime(job.getStartTime());
        jobInfoDTO.setErrorMessage(job.getErrorMessage());
        jobInfoDTO.setState(job.getState());
        jobInfoDTO.setProgress(new Progress(info.getProgress()));
        // TODO MVR with the latest changes the ID is alwways null :D
        if (job.getId() != null) { // TODO MVR the id should not be null here
            jobInfoDTO.setId(jobInfoDTO.getId());
        }
        return jobInfoDTO;
    }
}

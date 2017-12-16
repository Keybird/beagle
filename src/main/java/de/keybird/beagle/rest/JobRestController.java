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
import org.springframework.web.bind.annotation.RestController;

import de.keybird.beagle.api.DocumentState;
import de.keybird.beagle.jobs.JobExecutionFactory;
import de.keybird.beagle.jobs.JobExecutionManager;
import de.keybird.beagle.jobs.Progress;
import de.keybird.beagle.jobs.execution.AbstractJobExecution;
import de.keybird.beagle.jobs.persistence.JobEntity;
import de.keybird.beagle.repository.DocumentRepository;
import de.keybird.beagle.repository.JobRepository;
import de.keybird.beagle.rest.model.JobDTO;
import de.keybird.beagle.rest.model.JobInfoDTO;

@RestController
@RequestMapping("/jobs")
public class JobRestController {

    @Autowired
    private JobExecutionFactory jobFactory;

    @Autowired
    private JobExecutionManager jobExecutionManager;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JobRepository jobRepository;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listJobs() {
        final Iterable<JobEntity> all = jobRepository.findAll();
        if (!all.iterator().hasNext()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        // Convert entities to DTOs (let's us fine-tune what to expose)
        final List<JobDTO> entities = StreamSupport.stream(all.spliterator(), false)
                .map(e -> new JobDTO(e))
                .collect(Collectors.toList());
        return new ResponseEntity(entities, HttpStatus.OK);
    }

    @RequestMapping(path="/running", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<JobInfoDTO>> showProgress() {
        final List<AbstractJobExecution> executions = jobExecutionManager.getExecutions();
        if (executions.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        final List<JobInfoDTO> jobDTOList = executions.stream().map(jobExecution -> createFrom(jobExecution)).collect(Collectors.toList());
        jobDTOList.sort(Comparator.comparing(JobInfoDTO::getId).reversed());
        return new ResponseEntity<>(jobDTOList, HttpStatus.OK);
    }

    @RequestMapping(path="/detect", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity startDetect() {
        jobExecutionManager.submit(jobFactory.createDetectJob());
        return ResponseEntity.accepted().build();
    }

    @RequestMapping(path="/import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity startImport() {
        // TODO MVR use service for this?
        documentRepository
            .findByState(DocumentState.New)
            .forEach(theImport -> jobExecutionManager.submit(jobFactory.createImportJob(theImport)));
        return ResponseEntity.accepted().build();
    }

    @RequestMapping(path="/index", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity startIndex() {
        jobExecutionManager.submit(jobFactory.createIndexJob());
        return ResponseEntity.accepted().build();
    }

    public static JobInfoDTO createFrom(AbstractJobExecution jobExecution) {
        final JobEntity jobEntity = jobExecution.getJobEntity();
        final JobInfoDTO jobInfoDTO = new JobInfoDTO();
        jobInfoDTO.setDescription(jobExecution.getDescription());
        jobInfoDTO.setCompleteTime(jobEntity.getCompleteTime());
        jobInfoDTO.setStartTime(jobEntity.getStartTime());
        jobInfoDTO.setErrorMessage(jobEntity.getErrorMessage());
        jobInfoDTO.setState(jobEntity.getState());
        jobInfoDTO.setProgress(new Progress(jobExecution.getProgress()));
        if (jobEntity.getId() != null) { // TODO MVR the id should not be null here
            jobInfoDTO.setId(jobInfoDTO.getId());
        }
        return jobInfoDTO;
    }
}

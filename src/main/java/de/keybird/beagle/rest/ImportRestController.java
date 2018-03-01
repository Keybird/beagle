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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.keybird.beagle.api.Document;
import de.keybird.beagle.api.source.ByteDocumentSource;
import de.keybird.beagle.api.source.DocumentSource;
import de.keybird.beagle.jobs.JobExecutionFactory;
import de.keybird.beagle.jobs.JobExecutionManager;
import de.keybird.beagle.jobs.execution.JobRunner;
import de.keybird.beagle.jobs.persistence.DetectJobEntity;
import de.keybird.beagle.repository.DocumentRepository;
import de.keybird.beagle.rest.model.DocumentDTO;

@RestController
@RequestMapping("/imports")
// TODO MVR should be renamed to DocumentRestController as it lists documents
public class ImportRestController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JobExecutionFactory jobExecutionFactory;

    @Autowired
    private JobExecutionManager jobExecutionManager;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Document>> listImports() {
        Iterable<Document> documents = documentRepository.findAll();
        if (!documents.iterator().hasNext()) {
            return ResponseUtils.noContent();
        }
        // Convert to DTOs
        final List<DocumentDTO> documentDTOS = StreamSupport.stream(documents.spliterator(), false)
                .map(document -> new DocumentDTO(document))
                .collect(Collectors.toList());
        return new ResponseEntity(documentDTOS, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createDocument(@RequestParam(value = "name") String filename, HttpEntity<byte[]> requestEntity) {
        final DocumentSource source = new ByteDocumentSource(() -> requestEntity.getBody(), () -> filename);
        final JobRunner<DetectJobEntity> detectJobRunner = jobExecutionFactory.createDetectJobRunner(source);
        jobExecutionManager.submit(detectJobRunner);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll() {
        documentRepository.deleteAll();
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteDocument(@PathVariable("id") long documentId) {
        final Document document = documentRepository.findOne(documentId);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        documentRepository.delete(document);
        return ResponseUtils.noContent();
    }
}

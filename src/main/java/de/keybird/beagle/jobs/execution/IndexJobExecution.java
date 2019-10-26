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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.opennms.plugins.elasticsearch.rest.bulk.BulkRequest;
import org.opennms.plugins.elasticsearch.rest.bulk.BulkResultWrapper;
import org.opennms.plugins.elasticsearch.rest.bulk.BulkWrapper;
import org.opennms.plugins.elasticsearch.rest.bulk.FailedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import de.keybird.beagle.api.Page;
import de.keybird.beagle.api.PageState;
import de.keybird.beagle.elastic.AttachmentPipelineInitializer;
import de.keybird.beagle.jobs.IndexJob;
import de.keybird.beagle.jobs.LogLevel;
import de.keybird.beagle.repository.PageRepository;
import io.searchbox.client.JestClient;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;

// Syncs database content with elastic
@Service
@Scope("prototype")
public class IndexJobExecution implements JobExecution<IndexJob> {

    private static final Logger LOG = LoggerFactory.getLogger(IndexJobExecution.class);

    @Autowired
    private JestClient client;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private EntityManager entityManager;

    @Value("${index.bulkSize}")
    private int bulkSize;

    @Value("${index.retryCount}")
    private int retryCount;

    @Value("${index.pipeline.retryCount}")
    private int pipelineInitRetryCount;

    @Value("${index.pipeline.retryDelay}")
    private int pipelineInitRetryDelay;

    private Pageable pageRequest;

    public void setPageRequest(Pageable pageRequest) {
        this.pageRequest = pageRequest;
    }

    @Override
    public void execute(JobExecutionContext<IndexJob> context) {
        // Before we do anything, let's initialize the elastic backend
        try {
            context.logEntry(LogLevel.Info, "Initializing elastic pipeline for attachments ...");
            new AttachmentPipelineInitializer(client, pipelineInitRetryCount, pipelineInitRetryDelay).initialize();
            context.logEntry(LogLevel.Success, "Initializing elastic pipeline for attachments was successful");
        } catch (IOException ex) {
            context.setErrorMessage(ex.getMessage());
            context.logEntry(LogLevel.Error, "Initializing elastic pipeline for attachments failed. Reason: {}", ex.getMessage(), ex);
            return;
        }

        final List<Page> pagesToImport = pageRepository.findByState(PageState.Imported, pageRequest).stream().collect(Collectors.toList());
        final int totalSize = pagesToImport.size() == pageRequest.getPageSize() ? pageRequest.getPageSize() : pagesToImport.size();
        context.updateProgress(0, totalSize);

        // Now partition and index
        try {
            if (pagesToImport.isEmpty()) {
                LOG.warn("No pages are available for indexing.");
                return;
            }
            int offset = 0;
            while(!pagesToImport.isEmpty()) {
                final List<List<Page>> partitions = Lists.partition(pagesToImport, bulkSize);
                final List<Page> partition = partitions.get(0);

                // Perform Indexing
                boolean success = indexBatch(context, partition, offset, partition.size());

                // Flush the data
                entityManager.flush();

                // Cleanup
                offset += partition.size();
                context.updateProgress(offset);
                pagesToImport.removeAll(partition);

                if (!success) {
                    return;
                }
            }
        } finally {
            context.updateProgress(totalSize);
        }
    }

    private boolean indexBatch(JobExecutionContext<IndexJob> context, List<Page> partition, int offset, int batchSize) {
        context.logEntry(LogLevel.Info, "{}/{}", offset == 0 ? offset : offset + 1, offset + batchSize);

        try {
            final BulkRequest<Page> bulkRequest = new BulkRequest<>(client, partition, pages -> {
                // Convert to actions
                final List<Index> elasticActions = pages.stream()
                        .map(eachPage -> {
                            final byte[] base64bytes = Base64.getEncoder().encode(eachPage.getPayload());
                            final JsonObject json = new JsonObject();
                            json.addProperty("data", new String(base64bytes));
                            json.addProperty("id", eachPage.getId()); // we add the id to ensure it is referencable

                            final Index action = new Index.Builder(json)
                                    .index("documents")
                                    .type("pages")
                                    .setParameter("pipeline", "attachment")
                                    .build();
                            return action;
                        }).collect(Collectors.toList());
                // TODO MVR add test to verify the pipelaine actually indexes the data
                return new BulkWrapper(new Bulk.Builder().addAction(elasticActions).setParameter("pipeline", "attachment"));
            }, retryCount);
            final BulkResultWrapper bulkResultWrapper = bulkRequest.execute();
            final List<FailedItem<Page>> failedItems = bulkResultWrapper.isSucceeded() ? new ArrayList<>() : bulkResultWrapper.getFailedItems();
            final List<Page> successPages = bulkResultWrapper.isSucceeded() ? partition : extractSuccessItems(bulkResultWrapper, partition);
            failedItems.forEach(eachItem -> {
                Page page = eachItem.getItem();
                page.setErrorMessage(eachItem.getCause().getMessage());
                context.logEntry(LogLevel.Error, "Page {}/{} could not be indexed. Reason: {}", page.getDocument().getFilename(), page.getPageNumber(), eachItem.getCause().getMessage());
            });
            if (!failedItems.isEmpty()) {
                context.setErrorMessage("At least one page was not indexed properly");
            }
            successPages.forEach(page -> {
                page.setState(PageState.Indexed);
                page.setErrorMessage(null);
                context.logEntry(LogLevel.Success, "Page {}/{} was indexed successfully", page.getDocument().getFilename(), page.getPageNumber());
            });
            return true;
        } catch (IOException e) {
            partition.forEach(page -> {
                LOG.error("Could not index page {}. Reason: ", page.getName(), e.getMessage());
                page.setErrorMessage(e.getMessage());
                context.logEntry(LogLevel.Error, "Page {}/{} could not be indexed. Reason: {}", page.getDocument().getFilename(), page.getPageNumber(), e.getMessage());
            });
            context.setErrorMessage("All pages were not indexed properly: " + e.getMessage());
            return false;
        }
    }

    private static List<Page> extractSuccessItems(BulkResultWrapper<Page> resultWrapper, List<Page> allPages) {
        final List<Page> successPages = Lists.newArrayList(allPages);
        successPages.removeAll(resultWrapper.getFailedDocuments());
        return successPages;
    }
}

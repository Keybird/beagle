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
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

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
import de.keybird.beagle.elastic.BulkResultWrapper;
import de.keybird.beagle.elastic.FailedItem;
import de.keybird.beagle.jobs.IndexJob;
import de.keybird.beagle.jobs.LogLevel;
import de.keybird.beagle.repository.PageRepository;
import io.searchbox.client.JestClient;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;

// Syncs database content with elastic
@Service
@Scope("prototype")
public class IndexJobExecution implements JobExecution<IndexJob> {

    private static final Logger LOG = LoggerFactory.getLogger(IndexJobExecution.class);

    private static final int[] SLEEP_TIME = new int[] { 5000, 5000, 5000, 5000 };

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

    private Pageable pageRequest;

    public void setPageRequest(Pageable pageRequest) {
        this.pageRequest = pageRequest;
    }

    public void execute(JobExecutionContext<IndexJob> context) {
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
            final BulkResultWrapper bulkResultWrapper = executeBulk(partition, 0);
            final List<FailedItem<Page>> failedItems = bulkResultWrapper.isSucceeded() ? Collections.emptyList() : bulkResultWrapper.getFailedItems(partition);
            final List<Page> successPages = bulkResultWrapper.isSucceeded() ? partition : bulkResultWrapper.getSuccessItems(partition);
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

    private BulkResultWrapper executeBulk(List<Page> pages, int retry) throws IOException {
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
        // TODO MVR write test for this
        Bulk bulk = new Bulk.Builder().addAction(elasticActions).setParameter("pipeline", "attachment").build();
        try {
            BulkResult bulkResult = client.execute(bulk);
            BulkResultWrapper bulkResultWrapper = new BulkResultWrapper(bulkResult);
            if (!bulkResultWrapper.isSucceeded()) {
                if (retry == retryCount) { // Bail
                    return bulkResultWrapper;
                }
                final List<FailedItem<Page>> failedItems = bulkResultWrapper.getFailedItems(pages);
                final List<Page> failedPages = getFailedPages(failedItems);

                // free some memory before retrying
                elasticActions.clear();
                return retry(failedPages, retry + 1, bulkResultWrapper.getErrorMessage());
            }
            return bulkResultWrapper;
        } catch (IOException ex) {
            if (retry == retryCount) {
                throw ex;
            }
            // free some memory
            bulk = null;
            elasticActions.clear();
            return retry(pages, retry + 1, ex.getMessage());
        }
    }

    private BulkResultWrapper retry(List<Page> pages, int retry, String errorMessage) throws IOException {
        LOG.info("An error occurred while executing the bulk request: {}.", errorMessage);
        // Wait a bit, before actually retrying
        try {
            long sleepTime = getSleepTime(retry);
            if (sleepTime > 0) {
                LOG.info("Waiting {} ms before retrying", sleepTime);
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOG.info("Retrying now ...");
        return executeBulk(pages, retry);
    }

    private List<Page> getFailedPages(List<FailedItem<Page>> failedItems) {
        final List<Page> failedPages = failedItems.stream().map(item -> item.getItem()).collect(Collectors.toList());
        return failedPages;
    }

    private long getSleepTime(int retry) {
        if (retry == 0) return 0;
        if ((retry - 1) > SLEEP_TIME.length - 1) {
            return SLEEP_TIME[SLEEP_TIME.length - 1];
        }
        return SLEEP_TIME[retry - 1];
    }
}

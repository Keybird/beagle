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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.common.eventbus.EventBus;

import de.keybird.beagle.api.Document;
import de.keybird.beagle.events.JobExecutionProgressChangedEvent;
import de.keybird.beagle.events.JobExecutionStartedEvent;
import de.keybird.beagle.events.JobExecutionStateChangedEvent;
import de.keybird.beagle.jobs.ArchiveJob;
import de.keybird.beagle.jobs.DetectJob;
import de.keybird.beagle.jobs.ImportJob;
import de.keybird.beagle.jobs.IndexJob;
import de.keybird.beagle.jobs.Job;
import de.keybird.beagle.jobs.JobState;
import de.keybird.beagle.jobs.JobVisitor;
import de.keybird.beagle.jobs.LogEntry;
import de.keybird.beagle.jobs.LogLevel;
import de.keybird.beagle.jobs.Progress;
import de.keybird.beagle.jobs.persistence.ArchiveJobEntity;
import de.keybird.beagle.jobs.persistence.DetectJobEntity;
import de.keybird.beagle.jobs.persistence.ImportJobEntity;
import de.keybird.beagle.jobs.persistence.IndexJobEntity;
import de.keybird.beagle.jobs.persistence.JobEntity;
import de.keybird.beagle.jobs.persistence.LogEntity;
import de.keybird.beagle.repository.DocumentRepository;
import de.keybird.beagle.repository.JobRepository;

// TODO MVR es wird jetzt zwar geloggt, welche dokumente usw. abgewiesen wurden, aber ein übergeordneter status für die dokumente, pages fehlt noch
// Das muss noch eingeführt werden, damit die queries funktionieren
@Service
@Scope("prototype")
public class JobRunner<T extends Job> implements JobExecutionContext<T> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static AtomicInteger atomicInteger = new AtomicInteger(1);

    @Value("${working.directory}")
    private String workingDirectory;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Value("${working.directory}")
    private Path workingPath;
    private Path inboxPath;
    private Path archivePath;

    private T job;

    private ErrorHandler errorHandler;

    private SuccessHandler successHandler;

    @PostConstruct
    public void init() {
        workingDirectory = workingDirectory.replaceAll("~", System.getProperty("user.home"));
        this.workingPath = Paths.get(workingDirectory);
        this.inboxPath = workingPath.resolve("1_inbox");
        this.archivePath = workingPath.resolve("2_archive");
    }

    @Transactional
    public void execute(T job, JobExecution<T> execution) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("No active Session/Transaction found. Bailing");
        }
        executeInTransactionScope(job, execution);
    }

    private void executeInTransactionScope(T job, JobExecution<T> execution) {
        this.job = Objects.requireNonNull(job);
        Objects.requireNonNull(execution);

        try {
            setState(JobState.Initializing);
            start();

            execution.execute(this);

            success();
            onSuccess();
        } catch (Throwable t) {
            error(t);
            onError(t);

            // We want to propagate properly without having to add Exception to the signature
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            }
            throw new RuntimeException(t);
        } finally {
            complete();
        }
    }

    public Progress getProgress() {
        return job.getProgress();
    }

    public T getJob() {
        return this.job;
    }

    public Path getInboxPath() {
        return inboxPath;
    }

    public Path getArchivePath() {
        return archivePath;
    }

    private EventBus getEventBus() {
        return eventBus;
    }

    public Path getWorkingPath() {
        return workingPath;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setSuccessHandler(SuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    public void setState(JobState state) {
        JobState oldState = state;
        job.setState(state);
        getEventBus().post(new JobExecutionStateChangedEvent(job, oldState, state));
    }

    void setStartTime(Date startTime) {
        job.setStartTime(startTime);
    }

    void setCompleteTime(Date completeTime) {
        job.setCompleteTime(completeTime);
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        job.setErrorMessage(errorMessage);
    }

    protected void start() {
        getEventBus().post(new JobExecutionStartedEvent(job));
        Thread.currentThread().setName(getClass().getName() + " - " + job.getType() + " - " + atomicInteger.getAndIncrement());
        setStartTime(new Date());
        setState(JobState.Running);
    }

    protected void success() {
        setCompleteTime(new Date());
        setState(JobState.Completed);
        // DO NOT set the errorMessage to null here
    }

    protected void error(Throwable t) {
        setErrorMessage(t.getMessage());
    }

    protected void complete() {
        final JobEntity entity = job.accept(
                new JobVisitor<JobEntity>() {
                    void applyDefaults(JobEntity input) {
                        input.setCompleteTime(job.getCompleteTime());
                        input.setErrorMessage(job.getErrorMessage());
                        input.setStartTime(job.getStartTime());
                        input.setState(job.getState());
                        input.setCreateTime(job.getCreateTime());
                        input.setLogs(job.getLogs().stream().map(log -> {
                            final LogEntity logEntity = new LogEntity();
                            logEntity.setLevel(log.getLogLevel());
                            logEntity.setMessage(log.getMessage());
                            logEntity.setDate(log.getDate());
                            return logEntity;
                        }).collect(Collectors.toList()));
                    }

                    @Override
                    public JobEntity visit(DetectJob detectJob) {
                        final DetectJobEntity detectJobEntity = new DetectJobEntity();
                        applyDefaults(detectJobEntity);
                        detectJobEntity.setSource(detectJob.getDocumentSource().getDescription());
                        return detectJobEntity;
                    }

                    @Override
                    public JobEntity visit(IndexJob indexJob) {
                        final IndexJobEntity indexJobEntity = new IndexJobEntity(indexJob.getPage());
                        applyDefaults(indexJobEntity);
                        return indexJobEntity;
                    }

                    @Override
                    public JobEntity visit(ImportJob importJob) {
                        final Document importedDocument = documentRepository.findOne(importJob.getDocumentId());
                        if (importedDocument == null) {
                            throw new IllegalStateException("Document with id " + importJob.getDocumentId() + " not found");
                        }
                        final ImportJobEntity importJobEntity = new ImportJobEntity(importedDocument);
                        applyDefaults(importJobEntity);
                        return importJobEntity;
                    }

                    @Override
                    public JobEntity visit(ArchiveJob job) {
                        final ArchiveJobEntity jobEntity = new ArchiveJobEntity();
                        applyDefaults(jobEntity);
                        return jobEntity;
                    }
                });
        jobRepository.save(entity);
    }

    public void logEntry(LogLevel logLevel, String message, Object... args) {
        // Log the message to console, to see what is going on. However if multiple jobs are running in parallel it is hard to determine
        // Where the job is coming from, therefore we append the description of the job, but only if it is not identical to the message
        if (!job.getDescription().equals(message)) {
            logger.info(job.getDescription() + ": " + message, args);
        } else {
            logger.info(message, args);
        }

        // Append log entry to job entity
        final FormattingTuple formattingTuple = MessageFormatter.arrayFormat(message, args);
        final LogEntry logEntry = new LogEntry(logLevel, formattingTuple.getMessage());
        job.addLog(logEntry);
    }

    public void updateProgress(int currentProgress, int totalProgress) {
        final Progress oldProgress = new Progress(job.getProgress());
        job.updateProgress(currentProgress, totalProgress);
        getEventBus().post(new JobExecutionProgressChangedEvent(job, oldProgress, getProgress()));
    }

    public void updateProgress(int currentProgress) {
        updateProgress(currentProgress, getProgress().getTotalProgress());
    }

    public void onSuccess() {
        if (successHandler != null) {
            successHandler.handle(this);
        }
    }

    public void onError(Throwable t) {
        if (errorHandler != null) {
            errorHandler.handle(this, t);
        }
    }
}
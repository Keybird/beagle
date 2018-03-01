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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;

import de.keybird.beagle.events.JobExecutionProgressChangedEvent;
import de.keybird.beagle.events.JobExecutionStateChangedEvent;
import de.keybird.beagle.jobs.Progress;
import de.keybird.beagle.jobs.persistence.JobEntity;
import de.keybird.beagle.jobs.persistence.JobState;
import de.keybird.beagle.jobs.persistence.LogEntity;
import de.keybird.beagle.jobs.persistence.LogLevel;
import de.keybird.beagle.repository.JobRepository;

@Service
@Scope("prototype")
public class JobExecutionContext<J extends JobEntity> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${working.directory}")
    private String workingDirectory;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private JobRepository jobRepository;

    private Path workingPath;
    private Path inboxPath;
    private Path archivePath;

    private ErrorHandler errorHandler;

    private SuccessHandler successHandler;

    private J jobEntity;

    private Progress progress = new Progress();

    @PostConstruct
    public void init() {
        workingDirectory = workingDirectory.replaceAll("~", System.getProperty("user.home"));
        this.workingPath = Paths.get(workingDirectory);
        this.inboxPath = workingPath.resolve("1_inbox");
        this.archivePath = workingPath.resolve("2_archive");
        this.progress = new Progress();
    }

    public Progress getProgress() {
        return progress;
    }

    public J getJobEntity() {
        return this.jobEntity;
    }

    public void setJobEntity(J jobEntity) {
        this.jobEntity = jobEntity;
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

    public JobRepository getJobRepository() {
        return jobRepository;
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

    protected void setState(JobState state) {
        JobState oldState = state;
        jobEntity.setState(state);
        getEventBus().post(new JobExecutionStateChangedEvent(this, oldState, state));
    }

    protected void setStartTime(Date startTime) {
        jobEntity.setStartTime(startTime);
    }

    protected void setCompleteTime(Date completeTime) {
        jobEntity.setCompleteTime(completeTime);
    }

    protected void setErrorMessage(String errorMessage) {
        jobEntity.setErrorMessage(errorMessage);
    }

    protected void start() {
        setStartTime(new Date());
        setState(JobState.Running);
    }

    protected void success() {
        setCompleteTime(new Date());
        setErrorMessage(null);
        setState(JobState.Completed);
    }

    protected void error(Throwable t) {
        setErrorMessage(t.getMessage());
    }

    protected void complete() {
        if (jobRepository != null && jobEntity != null) {
            jobRepository.save(jobEntity); // Update job entity
        }
    }

    public void logEntry(LogLevel logLevel, String message, Object... args) {
        // Log the message to console, to see what is going on. However if multiple jobs are running in parallel it is hard to determine
        // Where the job is coming from, therefore we append the description of the job, but only if it is not identical to the message
        if (!jobEntity.getDescription().equals(message)) {
            logger.info(jobEntity.getDescription() + ": " + message, args);
        } else {
            logger.info(message, args);
        }

        // Append log entry to job entity
        final FormattingTuple formattingTuple = MessageFormatter.arrayFormat(message, args);
        final LogEntity logEntity = new LogEntity();
        logEntity.setLevel(logLevel);
        logEntity.setMessage(formattingTuple.getMessage());
        jobEntity.addLogEntry(logEntity);
    }

    protected void updateProgress(int currentProgress, int totalProgress) {
        Progress oldProgress = new Progress(getProgress());
        getProgress().setIndeterminate(false);
        getProgress().setProgress(currentProgress);
        getProgress().setTotalProgress(totalProgress);
        getEventBus().post(new JobExecutionProgressChangedEvent(this, oldProgress, getProgress()));
    }

    protected void onSuccess() {
        if (successHandler != null) {
            successHandler.handle(this);
        }
    }

    protected void onError(Throwable t) {
        if (errorHandler != null) {
            errorHandler.handle(this, t);
        }
    }
}

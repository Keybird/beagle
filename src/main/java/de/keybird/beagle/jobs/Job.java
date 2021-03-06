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

import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;

public abstract class Job {

    private Long id;
    private String errorMessage;
    private JobState state = JobState.Pending;
    private Date startTime;
    private Date createTime = new Date();
    private Date completeTime;
    private Progress progress = new Progress();

    private List<LogEntry> logs = Lists.newArrayList();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getCompleteTime() {
        return completeTime;
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public void setCompleteTime(Date completeTime) {
        this.completeTime = completeTime;
    }

    public void updateProgress(int currentProgress, int totalProgress) {
        getProgress().setIndeterminate(false);
        getProgress().setProgress(currentProgress);
        getProgress().setTotalProgress(totalProgress);
    }

    public Progress getProgress() {
        return progress;
    }

    public void addLog(LogEntry logEntry) {
        logs.add(Objects.requireNonNull(logEntry));
    }

    public abstract JobType getType();

    public abstract String getDescription();

    public abstract <T> T accept(JobVisitor<T> visitor);
}

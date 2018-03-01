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

package de.keybird.beagle.api.source;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import de.keybird.beagle.jobs.execution.JobExecutionContext;
import de.keybird.beagle.jobs.persistence.JobEntity;
import de.keybird.beagle.jobs.persistence.LogLevel;

// TODO MVR name is "FileSystemSource" but reads hard coded from "InboxPath". Either make it configurable or name it to "InboxSource"
public class FileSystemSource implements DocumentSource {
    @Override
    public List<DocumentEntry> getEntries(JobExecutionContext<? extends JobEntity> context) throws IOException {
        Files.createDirectories(context.getInboxPath());
        context.logEntry(LogLevel.Info,"Reading contents from directory '{}'", context.getInboxPath());

        return Files.list(context.getInboxPath())
                // TODO MVR inform user that only pdf files are loaded (or others are ignored, this is not transparent enough)
                .filter(entry -> !Files.isDirectory(entry) && entry.toString().toLowerCase().endsWith(".pdf"))
                .map(FileSystemDocument::new)
                .collect(Collectors.toList());
    }

    @Override
    public void cleanUp(DocumentEntry entry) {
        if (entry instanceof FileSystemDocument) {
            ((FileSystemDocument) entry).delete();
        }
    }
}

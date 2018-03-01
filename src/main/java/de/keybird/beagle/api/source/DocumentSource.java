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
import java.util.List;

import de.keybird.beagle.jobs.execution.JobExecutionContext;
import de.keybird.beagle.jobs.persistence.JobEntity;

public interface DocumentSource {
    List<DocumentEntry> getEntries(JobExecutionContext<? extends JobEntity> context) throws IOException;

    void cleanUp(DocumentEntry entry);
}

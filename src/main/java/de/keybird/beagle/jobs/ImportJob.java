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

import java.util.Objects;

import de.keybird.beagle.api.Document;

public class ImportJob extends Job {

    private final Long documentId;
    private final String filename;

    public ImportJob(Document theDocument) {
        Objects.requireNonNull(theDocument);
        this.documentId = Objects.requireNonNull(theDocument.getId());
        this.filename = Objects.requireNonNull(theDocument.getFilename());
    }

    @Override
    public JobType getType() {
        return JobType.Import;
    }

    @Override
    public String getDescription() {
        return String.format("Importing '%s'", filename);
    }

    @Override
    public <T> T accept(JobVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public String getFilename() {
        return filename;
    }

    public Long getDocumentId() {
        return documentId;
    }
}

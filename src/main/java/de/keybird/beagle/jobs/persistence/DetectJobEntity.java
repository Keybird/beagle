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

package de.keybird.beagle.jobs.persistence;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import de.keybird.beagle.api.source.DocumentSource;
import de.keybird.beagle.api.source.FileSystemSource;

@Entity
@DiscriminatorValue("detect")
public class DetectJobEntity extends JobEntity {

    // TODO MVR do we want to persist this?
    @Transient
    private DocumentSource documentSource = new FileSystemSource();

    @Override
    public JobType getType() {
        return JobType.Detect;
    }

    @Override
    public String getDescription() {
        return "Detecting new files";
    }

    public DetectJobEntity() {

    }

    public DetectJobEntity(DocumentSource source) {
        this.documentSource = source;
    }

    public DocumentSource getDocumentSource() {
        return documentSource;
    }

}

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
import java.util.Objects;
import java.util.function.Supplier;

import com.google.common.collect.Lists;

import de.keybird.beagle.jobs.execution.JobExecutionContext;
import de.keybird.beagle.jobs.persistence.JobEntity;
import de.keybird.beagle.jobs.persistence.LogLevel;

public class ByteDocumentSource implements DocumentSource {

    private final Supplier<byte[]> byteSupplier;
    private final Supplier<String> nameSupplier;

    public ByteDocumentSource(Supplier<byte[]> byteSupplier, Supplier<String> nameSupplier) {
        this.byteSupplier = Objects.requireNonNull(byteSupplier);
        this.nameSupplier = Objects.requireNonNull(nameSupplier);
    }

    @Override
    public List<DocumentEntry> getEntries(JobExecutionContext<? extends JobEntity> context) throws IOException {
        context.logEntry(LogLevel.Info,"Reading contents from InputStream");
        return Lists.newArrayList(
                new DocumentEntry() {
                    @Override public String getName() { return nameSupplier.get(); }
                    @Override public byte[] getPayload() throws IOException { return byteSupplier.get(); }
                }
        );
    }

    @Override
    public void cleanUp(DocumentEntry entry) {
        // nothing to do
    }
}

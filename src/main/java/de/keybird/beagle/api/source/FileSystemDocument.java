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
import java.nio.file.Path;
import java.util.Objects;

class FileSystemDocument implements DocumentEntry {

    private final Path path;

    protected FileSystemDocument(Path path) {
        this.path = Objects.requireNonNull(path);
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path '" + path + "' must be a file, but was a directory");
        }
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    public byte[] getPayload() throws IOException {
        return Files.readAllBytes(path);
    }

    void delete() {
        try {
            Files.delete(path);
        } catch (Exception ex) {
            // swallow it
        }
    }
}

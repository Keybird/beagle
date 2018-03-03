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

package de.keybird.beagle.rest.model;

import java.util.Date;

import de.keybird.beagle.api.Document;
import de.keybird.beagle.api.DocumentState;
import de.keybird.beagle.api.PageState;

public class DocumentDTO {

    private Long id;

    private String filename;

    private String checksum;

    private String errorMessage;

    private int pageCount;
    // number of pages indexed
    private int indexedCount;

    private DocumentState state;

    private Date importDate;

    public DocumentDTO() {

    }

    public DocumentDTO(Document document) {
        setId(document.getId());
        setChecksum(document.getChecksum());
        setErrorMessage(document.getErrorMessage());
        setFilename(document.getFilename());
        setImportDate(document.getImportDate());
        setPageCount(document.getPageCount());
        setState(document.getState());
        setIndexedCount((int) document.getPages().stream().filter(p -> p.getState() == PageState.Indexed).count());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public DocumentState getState() {
        return state;
    }

    public void setState(DocumentState state) {
        this.state = state;
    }

    public Date getImportDate() {
        return importDate;
    }

    public void setImportDate(Date importDate) {
        this.importDate = importDate;
    }

    public void setIndexedCount(int indexedCount) {
        this.indexedCount = indexedCount;
    }

    public int getIndexedCount() {
        return indexedCount;
    }
}

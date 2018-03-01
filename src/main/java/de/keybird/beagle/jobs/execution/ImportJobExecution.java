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

import static de.keybird.beagle.Utils.closeSilently;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import de.keybird.beagle.api.Document;
import de.keybird.beagle.api.DocumentState;
import de.keybird.beagle.api.Page;
import de.keybird.beagle.api.PageState;
import de.keybird.beagle.jobs.persistence.ImportJobEntity;
import de.keybird.beagle.jobs.persistence.LogLevel;
import de.keybird.beagle.repository.DocumentRepository;
import de.keybird.beagle.services.PdfManager;

// Imports files to database
@Service
@Scope("prototype")
public class ImportJobExecution implements JobExecution<ImportJobEntity> {

    private final Logger logger = LoggerFactory.getLogger(ImportJobExecution.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Override
    public void execute(JobExecutionContext<ImportJobEntity> context) throws Exception {
        context.logEntry(LogLevel.Info, "Importing '{}'", context.getJobEntity().getDocument().getFilename());

        context.setSuccessHandler((SuccessHandler<ImportJobEntity>) context1 -> {
            context1.getJobEntity().getDocument().setState(DocumentState.Imported);
            context1.getJobEntity().getDocument().setErrorMessage(null);
            documentRepository.save(context1.getJobEntity().getDocument());
        });

        context.setErrorHandler((ErrorHandler<ImportJobEntity>) (context12, throwable) -> {
            context12.getJobEntity().getDocument().setErrorMessage(throwable.getMessage());
            documentRepository.save(context12.getJobEntity().getDocument());
        });

        final Document importDocument = context.getJobEntity().getDocument();
        final PDDocument pdfDocument = PdfManager.load(importDocument.getPayload());
        final Splitter splitter = new Splitter();
        final List<PDDocument> splitDocuments = splitter.split(pdfDocument);

        // Update progress, as we now know how many pages there are
        int index = 0;
        context.updateProgress(index, splitDocuments.size());

        for (PDDocument splitDocument : splitDocuments) {
            final Page page = new Page();
            importDocument.getPages().add(page);
            page.setDocument(importDocument);
            page.setPageNumber(index + 1);

            try {
                // Determine payload
                final ByteArrayOutputStream payloadByteStream = new ByteArrayOutputStream();
                splitDocument.save(payloadByteStream);

                // Define Payload
                page.setName(UUID.randomUUID().toString() + ".pdf");
                page.setPayload(payloadByteStream.toByteArray());

                // Calculate checksum
                final HashCode hashCode = Hashing.sha256().hashBytes(page.getPayload());
                page.setChecksum(hashCode.toString());

                // Create thumbnail
                final ByteArrayOutputStream thumbnailByteStream = new ByteArrayOutputStream();
                final BufferedImage bufferedImage = new PDFRenderer(splitDocument).renderImage(0);
                ImageIO.write(bufferedImage, "png", thumbnailByteStream);
                page.setThumbnail(thumbnailByteStream.toByteArray());

                // Mark as imported
                page.setState(PageState.Imported);
                page.setErrorMessage(null);
                context.logEntry(LogLevel.Success, "Page {} was imported successful", index + 1);
                context.updateProgress(++index, splitDocuments.size());
            } catch (Exception ex) {
                logger.error("Error while importing page", ex);
                page.setErrorMessage(ex.getMessage());
            }
            closeSilently(splitDocument);
        }
        closeSilently(pdfDocument);
    }
}

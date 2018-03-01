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

package de.keybird.beagle.rest;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.keybird.beagle.api.Page;
import de.keybird.beagle.api.PageState;
import de.keybird.beagle.repository.PageRepository;
import de.keybird.beagle.rest.model.PageCountDTO;
import de.keybird.beagle.rest.model.PageDTO;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;

@RestController
@RequestMapping("/pages")
public class PageRestController {

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private JestClient jestClient;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Page>> listPages() {
        final Iterable<Page> pages = pageRepository.findAll();
        if (!pages.iterator().hasNext()) {
            return ResponseUtils.noContent();
        }
        // Convert to DTOS
        final List<PageDTO> pageDTOs = StreamSupport.stream(pages.spliterator(), false)
                .map(page -> new PageDTO(page))
                .collect(Collectors.toList());
        return new ResponseEntity(pageDTOs, HttpStatus.OK);
    }

    @RequestMapping(value="count", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity count() {
        final List<Page> indexedDocuments = pageRepository.findByState(PageState.Indexed);
        final List<Page> importedDocuments = pageRepository.findByState(PageState.Imported);
        final PageCountDTO pageCountDTO = new PageCountDTO()
                .withImportedCount(importedDocuments.size() + indexedDocuments.size())
                .withIndexedCount(indexedDocuments.size());
        return new ResponseEntity(pageCountDTO, HttpStatus.OK);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    @Transactional
    public ResponseEntity getPayload(@PathVariable("id") long pageId) {
        final Page page = pageRepository.findOne(pageId);
        if (page == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(page.getPayload());
    }

    @RequestMapping(value = "{id}/thumbnail", method = RequestMethod.GET)
    @Transactional
    public ResponseEntity getThumbnail(@PathVariable("id") long pageId) {
        final Page page = pageRepository.findOne(pageId);
        if (page == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(page.getThumbnail());
    }

    // TODO MVR we should not propagate the exception to the user,
    // but have a reasonable response return
    @RequestMapping(value="search", method=RequestMethod.GET)
    public ResponseEntity search(@RequestParam("query") String query) throws IOException {
        if (query == null || query.isEmpty()) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        // Build json query
        final JsonObject rootObject = new JsonObject();
        final JsonObject queryObject = new JsonObject();
        final JsonObject matchObject = new JsonObject();
        matchObject.addProperty("attachment.content", query);
        queryObject.add("match", matchObject);
        rootObject.add("query", queryObject);

        // Build search
        final Search search = new Search.Builder(rootObject.toString())
                .addIndex("documents")
                .addType("pages")
                .addSourceExcludePattern("data")
                .build();
        final SearchResult result = jestClient.execute(search);
        final Set<PageDTO> pages = new HashSet<>();
        if (result.isSucceeded()) {
            final JsonArray hits = result.getJsonObject()
                    .get("hits").getAsJsonObject()
                    .get("hits").getAsJsonArray();
            for (int i=0; i<hits.size();i++) {
                final JsonElement jsonElement = hits.get(i).getAsJsonObject()
                        .get("_source").getAsJsonObject()
                        .get("id");
                if (jsonElement != null) {
                    final long internalId = jsonElement.getAsLong();
                    // TODO MVR We should identify a page by its checksum instead
                    final Page page = pageRepository.findOne(internalId);
                    if (page != null) {
                        pages.add(new PageDTO(page));
                    }
                }
            }
        }
        return new ResponseEntity(pages, HttpStatus.OK);
    }
}

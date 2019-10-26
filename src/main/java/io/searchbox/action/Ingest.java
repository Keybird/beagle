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

package io.searchbox.action;

import java.util.Objects;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.searchbox.client.JestResult;

public class Ingest extends AbstractAction<JestResult> {

    private final Builder builder;

    protected Ingest(Builder builder) {
        super(builder);
        this.builder = Objects.requireNonNull(builder);
        setURI(buildURI());
    }

    @Override
    public String getData(Gson gson) {
        if (Strings.isNullOrEmpty(builder.query)) {
            return super.getData(gson);
        }
        JsonObject queryObject = gson.fromJson(builder.query, JsonObject.class);
        return gson.toJson(queryObject);
    }

    @Override
    protected String buildURI() {
        return "_ingest/" + builder.ingestPart + (builder.name == null ? "" : "/" + builder.name);
    }

    @Override
    public String getRestMethodName() {
        return builder.method;
    }

    @Override
    public JestResult createNewElasticSearchResult(String responseBody, int statusCode, String reasonPhrase, Gson gson) {
        return createNewElasticSearchResult(new JestResult(gson), responseBody, statusCode, reasonPhrase, gson);
    }

    public static class Builder extends AbstractAction.Builder {

        private final String ingestPart;
        private String method = "GET";
        private String query = null;
        private String name;

        public Builder(String ingestPart) {
            this.ingestPart = ingestPart;
        }

        public Builder withMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder withQuery(String query) {
            this.query = query;
            return this;
        }

        public Builder withName(String pipelineName) {
            this.name = pipelineName;
            return this;
        }

        @Override
        public Ingest build() {
            return new Ingest(this);
        }
    }

    public static class PipelineBuilder extends Builder {

        public PipelineBuilder() {
            super("pipeline");
            withMethod("GET");
        }

        public PipelineBuilder(String query) {
            super("pipeline");
            withMethod("GET");
            withQuery(query);
        }

        @Override
        public Ingest build() {
            return new Ingest(this);
        }
    }


}

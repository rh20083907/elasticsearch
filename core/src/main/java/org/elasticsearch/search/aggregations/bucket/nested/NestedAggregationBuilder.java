/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.search.aggregations.bucket.nested;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories.Builder;
import org.elasticsearch.search.aggregations.AggregatorFactory;
import org.elasticsearch.search.aggregations.support.AggregationContext;

import java.io.IOException;
import java.util.Objects;

public class NestedAggregationBuilder extends AggregationBuilder<NestedAggregationBuilder> {
    public static final String NAME = InternalNested.TYPE.name();
    public static final ParseField AGGREGATION_FIELD_NAME = new ParseField(NAME);

    private final String path;

    /**
     * @param name
     *            the name of this aggregation
     * @param path
     *            the path to use for this nested aggregation. The path must
     *            match the path to a nested object in the mappings.
     */
    public NestedAggregationBuilder(String name, String path) {
        super(name, InternalNested.TYPE);
        if (path == null) {
            throw new IllegalArgumentException("[path] must not be null: [" + name + "]");
        }
        this.path = path;
    }

    /**
     * Read from a stream.
     */
    public NestedAggregationBuilder(StreamInput in) throws IOException {
        super(in, InternalNested.TYPE);
        path = in.readString();
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeString(path);
    }

    /**
     * Get the path to use for this nested aggregation.
     */
    public String path() {
        return path;
    }

    @Override
    protected AggregatorFactory<?> doBuild(AggregationContext context, AggregatorFactory<?> parent, Builder subFactoriesBuilder)
            throws IOException {
        return new NestedAggregatorFactory(name, type, path, context, parent, subFactoriesBuilder, metaData);
    }

    @Override
    protected XContentBuilder internalXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(NestedAggregator.PATH_FIELD.getPreferredName(), path);
        builder.endObject();
        return builder;
    }

    public static NestedAggregationBuilder parse(String aggregationName, QueryParseContext context) throws IOException {
        String path = null;

        XContentParser.Token token;
        String currentFieldName = null;
        XContentParser parser = context.parser();
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.VALUE_STRING) {
                if (context.getParseFieldMatcher().match(currentFieldName, NestedAggregator.PATH_FIELD)) {
                    path = parser.text();
                } else {
                    throw new ParsingException(parser.getTokenLocation(),
                            "Unknown key for a " + token + " in [" + aggregationName + "]: [" + currentFieldName + "].");
                }
            } else {
                throw new ParsingException(parser.getTokenLocation(), "Unexpected token " + token + " in [" + aggregationName + "].");
            }
        }

        if (path == null) {
            // "field" doesn't exist, so we fall back to the context of the ancestors
            throw new ParsingException(parser.getTokenLocation(), "Missing [path] field for nested aggregation [" + aggregationName + "]");
        }

        return new NestedAggregationBuilder(aggregationName, path);
    }


    @Override
    protected int doHashCode() {
        return Objects.hash(path);
    }

    @Override
    protected boolean doEquals(Object obj) {
        NestedAggregationBuilder other = (NestedAggregationBuilder) obj;
        return Objects.equals(path, other.path);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }
}

/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Tools;

import javax.inject.Singleton;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Representing the message type mapping in ElasticSearch. This is giving ES more
 * information about what the fields look like and how it should analyze them.
 */
@Singleton
public class IndexMapping {
    public static final String TYPE_MESSAGE = "message";

    public Map<String, Object> messageMapping(final String analyzer) {
        return ImmutableMap.of(
                "properties", partFieldProperties(analyzer),
                "dynamic_templates", partDefaultAllInDynamicTemplate(),
                // Compress source field
                "_source", enabledAndCompressed(),
                // Enable purging by TTL
                "_ttl", enabled());
    }

    /*
     * Disable analyzing for every field by default.
     */
    private List<Map<String, Map<String, Object>>> partDefaultAllInDynamicTemplate() {
        final Map<String, Serializable> mappingInternal = ImmutableMap.<String, Serializable>of(
                "index", "not_analyzed",
                "doc_values", true);
        final Map<String, Object> defaultInternal = ImmutableMap.of(
                "match", "gl2_*",
                "mapping", mappingInternal);
        final Map<String, Map<String, Object>> templateInternal = ImmutableMap.of("internal_fields", defaultInternal);

        final Map<String, String> mappingAll = ImmutableMap.of("index", "not_analyzed");
        final Map<String, Object> defaultAll = ImmutableMap.of(
                // Match all
                "match", "*",
                // Analyze nothing by default
                "mapping", mappingAll);
        final Map<String, Map<String, Object>> templateAll = ImmutableMap.of("store_generic", defaultAll);

        return ImmutableList.of(templateInternal, templateAll);
    }

    /*
     * Enable analyzing for some fields again. Like for message and full_message.
     */
    private Map<String, Map<String, ? extends Serializable>> partFieldProperties(String analyzer) {
        return ImmutableMap.of(
                "message", analyzedString(analyzer),
                "full_message", analyzedString(analyzer),
                // http://joda-time.sourceforge.net/api-release/org/joda/time/format/DateTimeFormat.html
                // http://www.elasticsearch.org/guide/reference/mapping/date-format.html
                "timestamp", typeTimeWithMillis(true),
                // to support wildcard searches in source we need to lowercase the content (wildcard search lowercases search term)
                "source", analyzedString("analyzer_keyword"));
    }

    private Map<String, String> analyzedString(String analyzer) {
        return ImmutableMap.of(
                "index", "analyzed",
                "type", "string",
                "analyzer", analyzer);
    }

    private Map<String, Serializable> typeTimeWithMillis(boolean storeAsDocValues) {
        return ImmutableMap.<String, Serializable>of(
                "type", "date",
                "format", Tools.ES_DATE_FORMAT,
                "doc_values", storeAsDocValues);
    }

    private Map<String, Boolean> enabled() {
        return ImmutableMap.of("enabled", true);
    }

    private Map<String, Boolean> enabledAndCompressed() {
        return ImmutableMap.of(
                "enabled", true,
                "compress", true);
    }
}
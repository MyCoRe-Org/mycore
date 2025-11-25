/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.solr;

import static org.mycore.solr.MCRSolrConstants.SOLR_CONFIG_PREFIX;

import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.services.http.MCRHttpUtils;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSolrUtils {

    public static final String USE_HTTP_1_1_PROPERTY = "MCR.Solr.UseHttp_1_1";

    /**
     * Escapes characters in search values that need to be escaped for SOLR.
     * @see <a href="http://lucene.apache.org/core/4_3_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Escaping_Special_Characters">List of special characters</a>
     * @param value any value to look for in a field
     * @return null if value is null
     */
    public static String escapeSearchValue(final String value) {
        //    specialChars = "!&|+-(){}[]\"~*?:\\/^";
        /* '*' and '?' should always be threatened as special character */
        String specialChars = "!&|+-(){}[]\"~:\\/^";
        Pattern patternRestricted = Pattern.compile("([\\Q" + specialChars + "\\E])");
        if (value == null) {
            return null;
        }
        return patternRestricted.matcher(value).replaceAll("\\\\$1");
    }

    /**
     * Checks if the application uses nested documents. If so, each reindex requires
     * an extra deletion. Using nested documents slows the solr index performance.
     *
     * @return true if nested documents are used, otherwise false
     */
    public static boolean useNestedDocuments() {
        return MCRConfiguration2.getBoolean(SOLR_CONFIG_PREFIX + "NestedDocuments").orElse(true);
    }

    public static MCRConfigurationException getCoreConfigMissingException(String coreID) {
        return new MCRConfigurationException(
            "Missing property: " + MCRSolrConstants.SOLR_CORE_PREFIX + coreID
                + MCRSolrConstants.SOLR_CORE_NAME_SUFFIX);
    }

    /**
     * Returns a HttpRequest.Builder with the default settings for the Solr client.
     * @since 2024.06 introduced to workarround the missing support for HTTP/1.1 in the Solr Cloud Proxy (SOLR-17502)
     * @return a HttpRequest.Builder with the default settings for the Solr client
     */
    public static HttpRequest.Builder getRequestBuilder() {
        HttpRequest.Builder requestBuilder = MCRHttpUtils.getRequestBuilder();

        MCRConfiguration2.getBoolean(USE_HTTP_1_1_PROPERTY)
            .filter(useHttp11 -> useHttp11)
            .ifPresent(useHttp11 -> {
                requestBuilder.version(HttpClient.Version.HTTP_1_1);
            });

        return requestBuilder;
    }

    /**
     * Parses a URL-encoded Solr query string into a {@link ModifiableSolrParams} instance.
     * <p>
     * This method accepts query strings in the form of {@code key1=value1&key2=value2...},
     * optionally prefixed with a {@code '?'} character. Each key–value pair is URL-decoded
     * using UTF-8 and added to the resulting parameter set. Duplicate keys are preserved
     * as multivalued parameters (e.g. {@code fq=a&fq=b} becomes two values for {@code fq}).
     * <p>
     * Examples:
     * <pre>
     * parseQueryString("q=*:*&amp;rows=10");
     * parseQueryString("?fq=type&#58;book&amp;fq=lang&#58;de");
     * </pre>
     *
     * @param query the URL-encoded query string, with or without a leading {@code '?'}; may be {@code null}
     * @return a {@link ModifiableSolrParams} object containing all decoded parameters;
     *         never {@code null}
     */
    public static ModifiableSolrParams parseQueryString(String query) {
        if (query == null) {
            return new ModifiableSolrParams();
        }
        if (query.startsWith("?")) {
            query = query.substring(1);
        }

        ModifiableSolrParams params = new ModifiableSolrParams();
        if (query.isEmpty()) {
            return params;
        }

        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int indexOfEqual = pair.indexOf('=');
            String key = indexOfEqual >= 0 ? pair.substring(0, indexOfEqual) : pair;
            String value = indexOfEqual >= 0 ? pair.substring(indexOfEqual + 1) : "";
            String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
            String decodedValue = URLDecoder.decode(value, StandardCharsets.UTF_8);
            params.add(decodedKey, decodedValue);
        }
        return params;
    }

}

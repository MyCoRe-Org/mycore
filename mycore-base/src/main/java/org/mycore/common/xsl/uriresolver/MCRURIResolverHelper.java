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

package org.mycore.common.xsl.uriresolver;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.TransformerException;

/**
 * Utility methods for URI resolvers.
 */
public final class MCRURIResolverHelper {

    private MCRURIResolverHelper() {
    }

    /**
     * Parses a query string of the form {@code key1=value1&key2=value2}
     * into a map. Keys and values are URL-decoded.
     *
     * @param query the query string to parse; must not be {@code null}
     * @return an unmodifiable map of decoded key-value pairs
     */
    public static Map<String, String> parseQueryParameters(String query) {
        String[] param;
        StringTokenizer tok = new StringTokenizer(query, "&");
        Map<String, String> params = new HashMap<>();

        while (tok.hasMoreTokens()) {
            param = tok.nextToken().split("=");
            params.put(URLDecoder.decode(param[0], StandardCharsets.UTF_8),
                param.length >= 2 ? URLDecoder.decode(param[1], StandardCharsets.UTF_8) : "");
        }
        return params;
    }

    /**
     * Returns the first {@link TransformerException} found in the cause chain,
     * or wraps the given exception in a new one if none is found.
     *
     * @param e the exception to inspect
     * @return a {@link TransformerException} for rethrowing
     */
    public static TransformerException asTransformerException(Exception e) throws TransformerException {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof TransformerException te) {
                throw te;
            }
            cause = cause.getCause();
        }
        throw new TransformerException(cause);
    }

}

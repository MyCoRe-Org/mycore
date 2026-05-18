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

package org.mycore.orcid2.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;
import org.mycore.common.xsl.uriresolver.MCRURIResolverResponse;

/**
 * {@link URIResolver} that hashes a given input string and returns the result as an XML source.
 */
public class MCRORCIDHashResolver implements URIResolver {

    /**
     * Hashes the given input with the specified algorithm and returns the result as an XML source.
     * <p>Both {@code input} and {@code salt} are URL-decoded before processing.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{input}:{algorithm}
     *   &lt;scheme&gt;:{input}:{algorithm}:{salt}
     *   &lt;scheme&gt;:{input}:{algorithm}:{salt}:{iterations}
     * </pre>
     * <p>Example request:
     * <pre>
     *   hash:mySecret:SHA-256
     *   hash:mySecret:SHA-256:mySalt
     *   hash:mySecret:SHA-256:mySalt:1000
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <string>a3f1c2...</string>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping a {@code <string>} element containing the hex-encoded hash
     * @throws IllegalArgumentException if the URI does not contain at least scheme, input, and algorithm
     * @throws MCRException if the iteration count is not a valid integer or the algorithm is not supported
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final String[] split = href.split(":", 4);
        if (split.length < 3) {
            throw new IllegalArgumentException("Invalid format of uri for retrieval of hash: " + href);
        }
        final String input = URLDecoder.decode(split[1], StandardCharsets.UTF_8);
        final String algorithm = split[2];
        String result;
        try {
            if (split.length == 3) {
                result = MCRUtils.hashString(input, algorithm, null, 1);
            } else {
                final String optional = split[3];
                final int separatorIndex = optional.indexOf(':');
                if (separatorIndex >= 0) {
                    final String salt = URLDecoder.decode(optional.substring(0, separatorIndex),
                        StandardCharsets.UTF_8);
                    final int iterations = Integer.parseInt(optional.substring(separatorIndex + 1));
                    result = MCRUtils.hashString(input, algorithm, salt.getBytes(StandardCharsets.UTF_8), iterations);
                } else {
                    final String salt = URLDecoder.decode(optional, StandardCharsets.UTF_8);
                    result = MCRUtils.hashString(input, algorithm, salt.getBytes(StandardCharsets.UTF_8), 1);
                }
            }
        } catch (NumberFormatException e) {
            throw new MCRException("Invalid format of uri for retrieval of hash: " + href, e);
        }
        return MCRURIResolverResponse.ofString(result);
    }

}

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

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.mycore.common.xml.MCRXMLFunctions;

/**
 * {@link URIResolver} that checks whether a derivate is enabled for display for a given intent.
 */
public class MCRCheckDerivateDisplayEnabledResolver implements URIResolver {

    /**
     * Resolves whether a derivate is enabled for display and returns the result as an XML boolean.
     * <p>If no intent is provided, {@code true} is returned unconditionally.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:&lt;id&gt;[:&lt;intent&gt;]
     * </pre>
     * <p>Example request:
     * <pre>
     *   checkDerivateDisplayEnabled:mcr_derivate_00000001:thumbnail
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <boolean>true</boolean>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping an XML element {@code <boolean>} with value {@code true} or {@code false}
     * @throws IllegalArgumentException if the URI does not contain exactly one or two segments after the scheme
     */
    @Override
    public Source resolve(String href, String base) {
        final String[] split = href.split(":");
        boolean result = switch (split.length) {
            case 2 -> true;
            case 3 -> MCRXMLFunctions.isDerivateDisplayEnabled(split[1], split[2]);
            default -> throw new IllegalArgumentException(
                "Invalid format of uri for retrieval of checkDerivateDisplayEnabled: " + href);
        };
        return MCRURIResolverResponse.ofBoolean(result);
    }

}

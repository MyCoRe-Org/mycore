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

public class MCRCheckDerivateDisplayEnabledResolver implements URIResolver {

    /**
     * returns the boolean value for the given derivate and intent.
     * <p>
     * Syntax: <code>checkDerivateDisplayEnabled:{id}:{intent}</code>
     *
     * @param href
     *            URI in the syntax above
     * @param base
     *            not used
     * @return the root element "boolean" of the XML document with content string true of false
     * @see javax.xml.transform.URIResolver
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
        return MCRURIResolver.createBooleanResponse(result);
    }

}

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

import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.mycore.resource.MCRResourceHelper;

/**
 * {@link URIResolver} that reads static XML files from the web application context.
 */
public class MCRWebAppResolver implements URIResolver {

    /**
     * Resolves the given web application path and returns its content as a source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{path/to/resource}
     * </pre>
     * <p>Example request:
     * <pre>
     *   webapp:path/to/file.xml
     * </pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link StreamSource} pointing to the resolved web resource
     * @throws TransformerException if the resource cannot be found or loaded
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String path = href.substring(href.indexOf(':') + 1);
        try {
            URL resource = MCRResourceHelper.getWebResourceUrl(path);
            if (resource != null) {
                return new StreamSource(resource.toURI().toASCIIString());
            } else {
                throw new TransformerException("Could not find web resource: " + path);
            }
        } catch (Exception ex) {
            throw new TransformerException("Could not load web resource: " + path, ex);
        }
    }

}

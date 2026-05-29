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

package org.mycore.datamodel.common;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.common.content.MCRByteContent;

/**
 * {@link URIResolver} that resolves a data URL and returns its content as an XML source.
 */
public class MCRDataURLURIResolver implements URIResolver {

    /**
     * Resolves the given data URL and returns its decoded content as a source.
     * <p>URI Syntax:
     * <pre>
     *   data:[&lt;mediatype&gt;][;base64],&lt;data&gt;
     * </pre>
     * <p>Example request:
     * <pre>
     *   data:application/xml;base64,PGZvby8+
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <foo/>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping the decoded content of the data URL
     * @throws TransformerException if the data URL cannot be parsed or the content cannot be read
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        try {
            final MCRDataURL dataURL = MCRDataURL.parse(href);

            final MCRByteContent content = new MCRByteContent(dataURL.getData());
            content.setSystemId(href);
            content.setMimeType(dataURL.getMimeType());
            content.setEncoding(dataURL.getCharset().name());

            return content.getSource();
        } catch (IOException e) {
            throw new TransformerException(e);
        }
    }

}

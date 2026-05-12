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

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.common.content.MCRByteContent;
import org.mycore.datamodel.common.MCRDataURL;

/**
 * Resolves an data url and returns the content.
 *
 * @see MCRDataURL
 */
public class MCRDataURLResolver implements URIResolver {

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

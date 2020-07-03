/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.services.staticcontent;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.mycore.common.MCRException;
import org.mycore.common.xsl.MCRLazyStreamSource;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRStaticContentResolver implements URIResolver {

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final String[] parts = href.split(":", 3);
        if (parts.length != 3) {
            throw new MCRException("href needs to be staticContent:ContentGeneratorID:ObjectID but was " + href);
        }

        final String contentGeneratorID = parts[1];
        final MCRObjectID objectID = MCRObjectID.getInstance(parts[2]);
        final MCRObjectStaticContentGenerator generator = new MCRObjectStaticContentGenerator(contentGeneratorID);

        return new MCRLazyStreamSource(() -> generator.get(objectID), href);
    }
}

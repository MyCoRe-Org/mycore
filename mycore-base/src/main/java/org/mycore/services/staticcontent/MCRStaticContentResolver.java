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

package org.mycore.services.staticcontent;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.mycore.common.MCRException;
import org.mycore.common.xsl.MCRLazyStreamSource;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * {@link URIResolver} that returns statically generated content for an object as a stream source.
 */
public class MCRStaticContentResolver implements URIResolver {

    /**
     * Resolves the static content for the given object using the specified content generator
     * and returns it as a lazy stream source.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{contentGeneratorId}:{mcrId}
     * </pre>
     * <p>Example request:
     * <pre>
     *   staticcontent:myGenerator:mcr_document_00000001
     * </pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link MCRLazyStreamSource} that lazily generates the content for the given object
     * @throws MCRException if the URI does not contain exactly three {@code :}-separated segments
     */
    @Override
    public Source resolve(String href, String base) {
        final String[] parts = href.split(":", 3);
        if (parts.length != 3) {
            throw new MCRException("href needs to be staticContent:ContentGeneratorID:ObjectID but was " + href);
        }

        final String contentGeneratorID = parts[1];
        final MCRObjectID objectID = MCRObjectID.getInstance(parts[2]);
        final MCRObjectStaticContentGenerator generator =
            MCRObjectStaticContentGenerator.obtainInstance(contentGeneratorID);

        return new MCRLazyStreamSource(() -> generator.get(objectID), href);
    }

}

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.tools.MCRObjectFactory;

/**
 * {@link URIResolver} that returns the XML of a deleted object by its ID.
 */
public class MCRDeletedObjectURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the last known XML of a deleted MCR object and returns it as a source.
     * <p>If no version can be found, a dummy object with an empty metadata element is returned.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{mcrId}
     * </pre>
     * <p>Example request:
     * <pre>
     *   deletedMcrObject:mcr_document_00000001
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <mycoreobject ID="mcr_document_00000001">
     *     <metadata/>
     *   </mycoreobject>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping the last known XML of the object,
     *         or a dummy object if no version is found
     * @throws TransformerException if the content cannot be read
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] parts = href.split(":");
        MCRObjectID mcrId = MCRObjectID.getInstance(parts[parts.length - 1]);
        LOGGER.info("Resolving deleted object {}", mcrId);
        try {
            MCRContent lastPresentVersion = MCRXMLMetadataManager.obtainInstance().retrieveContent(mcrId);
            if (lastPresentVersion == null) {
                LOGGER.warn("Could not resolve deleted object {}", mcrId);
                return new JDOMSource(MCRObjectFactory.getSampleObject(mcrId));
            }
            return lastPresentVersion.getSource();
        } catch (IOException e) {
            throw new TransformerException(e);
        }
    }

}

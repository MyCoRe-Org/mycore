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

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRFileMetadata;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * {@link URIResolver} that returns file metadata of a derivate or a specific file within it as XML.
 */
public class MCRFileMetadataResolver implements URIResolver {

    /**
     * Resolves file metadata for a derivate or a specific file path within a derivate.
     * <p>If only a derivate ID is given, the full fileset metadata including URN is returned.
     * If a file path is also given, only the metadata for that specific file is returned.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{derivateId}[/{filePath}]
     * </pre>
     * <p>Example request:
     * <pre>
     *   fileMeta:mcr_derivate_00000001
     *   fileMeta:mcr_derivate_00000001/path/to/file.pdf
     * </pre>
     * <p>Example response for a derivate:
     * <pre>{@code
     *   <fileset urn="urn:nbn:de:...">
     *     <file name="/file.pdf">...</file>
     *   </fileset>
     * }</pre>
     * <p>Example response for a file:
     * <pre>{@code
     *   <file name="/path/to/file.pdf">...</file>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping either the {@code <fileset>} or {@code <file>} element
     */
    @Override
    public Source resolve(String href, String base) {
        String[] parts = href.split(":");
        String completePath = parts[1];
        String[] pathParts = completePath.split("/", 2);
        MCRObjectID derivateID = MCRObjectID.getInstance(pathParts[0]);
        MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateID);
        MCRObjectDerivate objectDerivate = derivate.getDerivate();
        if (pathParts.length == 1) {
            //only derivate is given;
            Element fileset = new Element(MCRObjectDerivate.ELEMENT_FILESET);
            if (objectDerivate.getURN() != null) {
                fileset.setAttribute(MCRObjectDerivate.ATTRIBUTE_FILESET_URN, objectDerivate.getURN());
                for (MCRFileMetadata fileMeta : objectDerivate.getFileMetadata()) {
                    fileset.addContent(fileMeta.createXML());
                }
            }
            return new JDOMSource(fileset);
        }
        MCRFileMetadata fileMetadata = objectDerivate.getOrCreateFileMetadata("/" + pathParts[1]);
        return new JDOMSource(fileMetadata.createXML());
    }

}

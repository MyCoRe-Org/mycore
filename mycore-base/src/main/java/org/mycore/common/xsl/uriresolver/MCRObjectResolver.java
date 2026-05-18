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
import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRExpandedObjectManager;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * {@link URIResolver} that reads a object from the metadata store and returns it as an XML source.
 */
public class MCRObjectResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the given object ID and returns its XML representation.
     * <p>By default, non-derivate objects are returned in their expanded form via
     * {@link MCRExpandedObjectManager}. This can be suppressed with {@code expanded=false}.
     * A specific revision can be requested with the {@code r} parameter.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{mcrId}[?r={revision}]
     *   &lt;scheme&gt;:{mcrId}[?expanded=false]
     * </pre>
     * <p>Example request:
     * <pre>
     *   mcrobject:mcr_document_00000001
     *   mcrobject:mcr_document_00000001?r=3
     *   mcrobject:mcr_document_00000001?expanded=false
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <mycoreobject ID="mcr_document_00000001" ...>
     *     <metadata>...</metadata>
     *   </mycoreobject>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping the XML representation of the object,
     *         or {@code null} if no content is found
     * @throws TransformerException if the content cannot be read
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String id = href.substring(href.indexOf(':') + 1);
        LOGGER.debug("Reading MCRObject with ID {}", id);
        Map<String, String> params;
        StringTokenizer tok = new StringTokenizer(id, "?");
        id = tok.nextToken();

        if (tok.hasMoreTokens()) {
            params = MCRURIResolverHelper.parseQueryParameters(tok.nextToken());
        } else {
            params = Collections.emptyMap();
        }

        MCRObjectID mcrid = MCRObjectID.getInstance(id);
        try {
            MCRXMLMetadataManager xmlmm = MCRXMLMetadataManager.obtainInstance();

            MCRContent content;
            if (params.containsKey("r")) {
                content = xmlmm.retrieveContent(mcrid, params.get("r"));
            } else {
                if (mcrid.getTypeId().equals(MCRDerivate.OBJECT_TYPE) ||
                    (params.containsKey("expanded") && params.get("expanded").equals("false"))) {
                    content = xmlmm.retrieveContent(mcrid);
                } else {
                    MCRObject expanded = MCRMetadataManager.retrieveMCRObject(mcrid);
                    MCRExpandedObject expandedObject =
                        MCRExpandedObjectManager.getInstance().getExpandedObject(expanded);
                    content = new MCRBaseContent(expandedObject);
                }
            }
            if (content == null) {
                return null;
            }
            LOGGER.debug("end resolving {}", href);
            return content.getSource();
        } catch (IOException e) {
            throw new TransformerException(e);
        }
    }

}

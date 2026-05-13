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

public class MCRObjectResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Reads local MCRObject with a given ID from the store.
     *
     * @param href
     *            for example, "mcrobject:DocPortal_document_07910401"
     * @return XML representation from MCRXMLContainer
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

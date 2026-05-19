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

package org.mycore.mets.tools;

import java.nio.file.Files;
import java.util.Collection;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.model.MCRMETSGeneratorFactory;

/**
 * {@link URIResolver} that returns the METS document for a given MCR object or derivate as XML.
 */
public class MCRMetsURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Resolves the METS document for the given MCR object or derivate ID and returns it as an XML source.
     * <p>If a MCR object ID is given, the first displayable derivate is used. If no displayable
     * derivate is found, an empty {@code <mets:mets>} element is returned. If a {@code mets.xml}
     * file exists in the derivate, it is returned directly; otherwise a METS document is generated
     * on the fly.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{mcrId|derivateId}
     * </pre>
     * <p>Example request:
     * <pre>
     *   mets:mcr_document_00000001
     *   mets:mcr_derivate_00000001
     * </pre>
     * <p>Example response:
     * <pre>{@code
     *   <mets:mets xmlns:mets="http://www.loc.gov/METS/">
     *     ...
     *   </mets:mets>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link Source} wrapping the METS document, or an empty {@code <mets:mets>}
     *         element if no displayable derivate is found
     * @throws TransformerException if the METS document cannot be read or generated
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String id = href.substring(href.indexOf(':') + 1);
        LOGGER.debug("Reading METS for ID {}", id);
        MCRObjectID objId = MCRObjectID.getInstance(id);
        if (!objId.getTypeId().equals(MCRDerivate.OBJECT_TYPE)) {
            MCRObjectID derivateID = getDerivateFromObject(id);
            if (derivateID == null) {
                return new JDOMSource(new Element("mets", Namespace.getNamespace("mets", "http://www.loc.gov/METS/")));
            }
            id = derivateID.toString();
        }
        MCRPath metsPath = MCRPath.getPath(id, "/mets.xml");
        try {
            if (Files.exists(metsPath)) {
                //TODO: generate new METS Output
                //ignoreNodes.add(metsFile);
                return new MCRPathContent(metsPath).getSource();
            }
            Document mets = MCRMETSGeneratorFactory.create(MCRPath.getPath(id, "/")).generate().asDocument();
            return new JDOMSource(mets);
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }

    private MCRObjectID getDerivateFromObject(String id) {
        Collection<MCRObjectID> derivates = MCRMetadataManager.getDerivateIds(MCRObjectID.getInstance(id));
        for (MCRObjectID derID : derivates) {
            if (MCRAccessManager.checkDerivateDisplayPermission(derID.toString())) {
                return derID;
            }
        }
        return null;
    }

}

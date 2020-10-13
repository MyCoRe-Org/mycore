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

package org.mycore.mets.iiif;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRPathContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.content.transformer.MCRParameterizedTransformer;
import org.mycore.common.xsl.MCRParameterCollector;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.iiif.presentation.impl.MCRIIIFPresentationImpl;
import org.mycore.iiif.presentation.model.basic.MCRIIIFManifest;
import org.mycore.mets.model.MCRMETSGeneratorFactory;
import org.mycore.mets.tools.MCRMetsSave;
import org.xml.sax.SAXException;

public class MCRMetsIIIFPresentationImpl extends MCRIIIFPresentationImpl {

    private static final String TRANSFORMER_ID_CONFIGURATION_KEY = "Transformer";

    private static final Logger LOGGER = LogManager.getLogger();

    public static final boolean STORE_METS_ON_GENERATE = MCRConfiguration2
        .getOrThrow("MCR.Mets.storeMetsOnGenerate", Boolean::parseBoolean);

    public MCRMetsIIIFPresentationImpl(String implName) {
        super(implName);
    }

    @Override
    public MCRIIIFManifest getManifest(String id) {
        try {
            Document metsDocument = getMets(id);
            LOGGER.debug(() -> new XMLOutputter(Format.getPrettyFormat()).outputString(metsDocument));
            return getConverter(id, metsDocument).convert();
        } catch (IOException | JDOMException | SAXException e) {
            throw new MCRException(e);
        }
    }

    protected MCRMetsMods2IIIFConverter getConverter(String id, Document metsDocument) {
        return new MCRMetsMods2IIIFConverter(metsDocument, id);
    }

    protected MCRContentTransformer getTransformer() {
        String transformerID = getProperties().get(TRANSFORMER_ID_CONFIGURATION_KEY);
        MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer(transformerID);

        if (transformer == null) {
            throw new MCRConfigurationException("Could not resolve transformer with id : " + transformerID);
        }

        return transformer;
    }

    public Document getMets(String id) throws IOException, JDOMException, SAXException {

        String objectid = MCRLinkTableManager.instance().getSourceOf(id).iterator().next();
        MCRContentTransformer transformer = getTransformer();
        MCRParameterCollector parameter = new MCRParameterCollector();

        if (objectid != null && objectid.length() != 0) {
            MCRDerivate derObj = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(id));
            MCRObjectID ownerID = derObj.getOwnerID();
            objectid = ownerID.toString();

            parameter.setParameter("objectID", objectid);
            parameter.setParameter("derivateID", id);
        }

        final MCRPath metsPath = MCRPath.getPath(id, "mets.xml");
        MCRContent source = Files.exists(metsPath) ? new MCRPathContent(metsPath) : generateMets(id);
        MCRContent content = transformer instanceof MCRParameterizedTransformer
            ? ((MCRParameterizedTransformer) transformer).transform(source, parameter)
            : transformer.transform(source);
        return content.asXML();
    }

    private synchronized MCRJDOMContent generateMets(String id) {
        final Document document = MCRMETSGeneratorFactory.create(MCRPath.getPath(id, "/")).generate().asDocument();
        if (STORE_METS_ON_GENERATE) {
            MCRMetsSave.saveMets(document, MCRObjectID.getInstance(id));
        }
        return new MCRJDOMContent(document);
    }
}

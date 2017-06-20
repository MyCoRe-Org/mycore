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
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
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
import org.xml.sax.SAXException;

public class MCRMetsIIIFPresentationImpl extends MCRIIIFPresentationImpl {

    private static final String TRANSFORMER_ID_CONFIGURATION_KEY = "Transformer";

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRMetsIIIFPresentationImpl(String implName) {
        super(implName);
    }

    @Override
    public MCRIIIFManifest getManifest(String id) {
        try {
            Document metsDocument = getMets(id);
            LOGGER.info(new XMLOutputter(Format.getPrettyFormat()).outputString(metsDocument));
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

        MCRPath metsPath = MCRPath.getPath(id, "mets.xml");
        if (!Files.exists(metsPath)) {
            throw new MCRException("File not found: " + id);
        }

        MCRPathContent source = new MCRPathContent(metsPath);
        MCRContent content = transformer instanceof MCRParameterizedTransformer
            ? ((MCRParameterizedTransformer) transformer).transform(source, parameter) : transformer.transform(source);
        return content.asXML();
    }
}

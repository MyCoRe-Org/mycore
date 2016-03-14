package org.mycore.pi.doi;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.xml.sax.SAXException;

/**
 *
 */
public class MCRDOIRegistrationService extends MCRPIRegistrationService<MCRDigitalObjectIdentifier> {

    public static final Namespace DATACITE_NAMESPACE = Namespace.getNamespace("datacite", "http://datacite.org/schema/kernel-3");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String TYPE = "doi";
    public static final String TEST_PREFIX = "UseTestPrefix";

    private String username;
    private String password;
    private String transformer;
    private String host;
    private String registerURL;
    private boolean useTestPrefix = false;

    public MCRDOIRegistrationService(String serviceID) {
        super(serviceID, TYPE);

        Map<String, String> properties = getProperties();
        username = properties.get("Username");
        password = properties.get("Password");
        useTestPrefix = (properties.containsKey(TEST_PREFIX)) ? Boolean.valueOf(properties.get(TEST_PREFIX)) : false;
        transformer = properties.get("Transformer");
        this.registerURL = properties.get("RegisterBaseURL");
        host = "mds.datacite.org";
    }

    private static void insertDOI(Document datacite, MCRDigitalObjectIdentifier doi) throws MCRPersistentIdentifierException {
        XPathExpression<Element> compile = XPathFactory.instance().compile("//datacite:identifier[@identifierType='DOI']",
                Filters.element(), null, DATACITE_NAMESPACE);
        List<Element> doiList = compile.evaluate(datacite);

        if (doiList.size() > 1) {
            throw new MCRPersistentIdentifierException("There is more then one identifier with type DOI!");
        } else if (doiList.size() == 1) {
            Element doiElement = doiList.stream().findAny().get();
            LOGGER.warn("Found existing DOI(" + doiElement.getTextTrim() + ") in Document will be replaced with" + doi.asString());
            doiElement.setText(doi.asString());
        } else {
            // must be 0
            Element doiElement = new Element("identifier", DATACITE_NAMESPACE);
            datacite.getRootElement().addContent(doiElement);
            doiElement.setAttribute("identifierType", "DOI");
            doiElement.setText(doi.asString());
        }
    }

    @Override
    public MCRDigitalObjectIdentifier registerIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        if (!additional.equals("")) {
            throw new MCRPersistentIdentifierException(getClass().getName() + " doesn't support additional information! (" + additional + ")");
        }

        MCRDigitalObjectIdentifier newDOI = getNewIdentifier(obj.getId(), additional);

        MCRObject mcrBase = MCRMetadataManager.retrieveMCRObject(obj.getId());
        Document dataciteDocument = transformToDatacite(newDOI, mcrBase);

        MCRDataciteClient dataciteClient = getDataciteClient();
        dataciteClient.storeMetadata(dataciteDocument);

        List<Map.Entry<String, URI>> entryList = new ArrayList<>();

        if (obj instanceof MCRObject) {
            insertFirstDerivate((MCRObject) obj, entryList);
        }

        URI registeredURI;
        try {
            registeredURI = new URI(this.registerURL + "/receive/" + obj.getId().toString());
            dataciteClient.mintDOI(newDOI, registeredURI);
        } catch (URISyntaxException e) {
            throw new MCRException("Base-URL seems to be invalid!", e);
        }

        dataciteClient.setMediaList(newDOI, entryList);

        return newDOI;
    }

    private void insertFirstDerivate(MCRObject obj, List<Map.Entry<String, URI>> entryList) {
        MCRObject mcrObject = obj;
        Optional<MCRObjectID> derivateIdOptional = MCRMetadataManager.getDerivateIds(mcrObject.getId(), 1, TimeUnit.MINUTES).stream().findFirst();
        derivateIdOptional.ifPresent(derivateId -> {
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
            String mainDoc = derivate.getDerivate().getInternals().getMainDoc();
            MCRPath mainDocumentPath = MCRPath.getPath(derivateId.toString(), mainDoc);
            try {
                String contentType = Files.probeContentType(mainDocumentPath);
                contentType = contentType == null ? "" : contentType;
                entryList.add(new AbstractMap.SimpleEntry<String, URI>("application/octet-stream", new URI(this.registerURL + "/servlets/MCRFileNodeServlet/" + derivateId.toString() + "/" + mainDoc)));
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Error while detecting the file to register!", e);
            }
        });
    }

    private MCRDataciteClient getDataciteClient() {
        return new MCRDataciteClient(host, username, password, false, this.useTestPrefix);
    }

    private Document transformToDatacite(MCRDigitalObjectIdentifier doi, MCRBase mcrBase) throws MCRPersistentIdentifierException {
        MCRObjectID id = mcrBase.getId();
        MCRBaseContent content = new MCRBaseContent(mcrBase);

        try {
            MCRContent transform = MCRContentTransformerFactory.getTransformer(this.transformer).transform(content);
            Document dataciteDocument = transform.asXML();
            insertDOI(dataciteDocument, doi);
            return dataciteDocument;
        } catch (IOException | JDOMException | SAXException e) {
            throw new MCRPersistentIdentifierException("Could not transform the content of " + id.toString() + " with the transformer " + transformer, e);
        }
    }

    @Override
    public void onDelete(MCRDigitalObjectIdentifier doi, MCRBase obj) throws MCRPersistentIdentifierException {
        throw new MCRPersistentIdentifierException("Object should not be deleted!");
    }

    @Override
    public void onUpdate(MCRDigitalObjectIdentifier doi, MCRBase obj) throws MCRPersistentIdentifierException {
        Document datacite = transformToDatacite(doi, obj);
        MCRDataciteClient dataciteClient = getDataciteClient();
        dataciteClient.deleteMetadata(doi);
        dataciteClient.storeMetadata(datacite);
    }

}

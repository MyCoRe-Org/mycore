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
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.xml.MCRXMLFunctions;
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
    public static final String TEST_PREFIX = "UseTestPrefix";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String TYPE = "doi";
    private String username;
    private String password;
    private String transformer;
    private String host;
    private String registerURL;
    private boolean useTestPrefix = false;

    public String getRegisterURL() {
        return registerURL;
    }

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

        URI registeredURI;
        try {
            registeredURI = new URI(this.registerURL + "/receive/" + obj.getId().toString());
            dataciteClient.mintDOI(newDOI, registeredURI);
        } catch (URISyntaxException e) {
            throw new MCRException("Base-URL seems to be invalid!", e);
        }

        List<Map.Entry<String, URI>> entryList = getMediaList((MCRObject) obj);
        dataciteClient.setMediaList(newDOI, entryList);

        return newDOI;
    }

    /**
     * Builds a list with with right content types and media urls assigned of a specific Object
     * @param obj the object
     * @return a list of entrys Media-Type, URL
     */
    public List<Map.Entry<String, URI>> getMediaList(MCRObject obj) {
        List<Map.Entry<String, URI>> entryList = new ArrayList<>();
        Optional<MCRObjectID> derivateIdOptional = MCRMetadataManager.getDerivateIds(obj.getId(), 1, TimeUnit.MINUTES).stream().findFirst();
        derivateIdOptional.ifPresent(derivateId -> {
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
            String mainDoc = derivate.getDerivate().getInternals().getMainDoc();
            MCRPath mainDocumentPath = MCRPath.getPath(derivateId.toString(), mainDoc);
            try {
                String contentType = Files.probeContentType(mainDocumentPath);
                contentType = contentType == null ? "application/octet-stream" : contentType;
                // TODO: maybe add link to viewer if PDF or other supported format
                entryList.add(new AbstractMap.SimpleEntry<>(contentType, new URI(this.registerURL + MCRXMLFunctions.encodeURIPath("/servlets/MCRFileNodeServlet/" + derivateId.toString() + "/" + mainDoc))));
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Error while detecting the file to register!", e);
            }
        });
        return entryList;
    }

    public MCRDataciteClient getDataciteClient() {
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
    public void delete(MCRDigitalObjectIdentifier doi, MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        if (MCRSessionMgr.getCurrentSession().getUserInformation().getUserID().equals(MCRSystemUserInformation.getSuperUserInstance().getUserID())) {
            LOGGER.warn("SuperUser deletes object " + obj.getId().toString() + " with registered doi " + doi.asString() + ". Try to set DOI inactive.");
            try {
                getDataciteClient().deleteMetadata(doi);
            } catch (MCRPersistentIdentifierException e) {
                LOGGER.error("Error while setting " + doi.asString() + " inactive! Delete of object should continue!");
            }
        } else {
            throw new MCRPersistentIdentifierException("Object should not be deleted! (It has a registered DOI)");
        }
    }

    @Override
    public void update(MCRDigitalObjectIdentifier doi, MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        Document datacite = transformToDatacite(doi, obj);
        MCRDataciteClient dataciteClient = getDataciteClient();
        dataciteClient.deleteMetadata(doi);
        dataciteClient.storeMetadata(datacite);
    }

}

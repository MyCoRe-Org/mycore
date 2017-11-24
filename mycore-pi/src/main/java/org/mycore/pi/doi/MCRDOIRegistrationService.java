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

package org.mycore.pi.doi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;

/**
 *
 */
public class MCRDOIRegistrationService extends MCRPIRegistrationService<MCRDigitalObjectIdentifier> {

    public static final Namespace DATACITE_NAMESPACE = Namespace.getNamespace("datacite",
        "http://datacite.org/schema/kernel-3");

    public static final String TEST_PREFIX = "UseTestPrefix";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String TYPE = "doi";

    /**
     * A media URL is longer then 255 chars
     */
    private static final int ERR_CODE_1_1 = 0x1001;

    /**
     * The datacite document is not valid
     */
    private static final int ERR_CODE_1_2 = 0x1002;

    private static final int MAX_URL_LENGTH = 255;

    private static final String DATACITE_SCHEMA_PATH = "xsd/datacite/metadata.xsd";

    private static final String TRANSLATE_PREFIX = "component.pi.register.error.";

    private String username;

    private String password;

    private String transformer;

    private String host;

    private String registerURL;

    private boolean useTestPrefix;

    public MCRDOIRegistrationService(String serviceID) {
        super(serviceID, MCRDigitalObjectIdentifier.TYPE);

        Map<String, String> properties = getProperties();
        username = properties.get("Username");
        password = properties.get("Password");
        useTestPrefix = properties.containsKey(TEST_PREFIX) && Boolean.valueOf(properties.get(TEST_PREFIX));
        transformer = properties.get("Transformer");
        this.registerURL = properties.get("RegisterBaseURL").replaceFirst("\\/$", "");
        host = "mds.datacite.org";
    }

    private static void insertDOI(Document datacite, MCRDigitalObjectIdentifier doi)
        throws MCRPersistentIdentifierException {
        XPathExpression<Element> compile = XPathFactory.instance().compile(
            "//datacite:identifier[@identifierType='DOI']",
            Filters.element(), null, DATACITE_NAMESPACE);
        List<Element> doiList = compile.evaluate(datacite);

        if (doiList.size() > 1) {
            throw new MCRPersistentIdentifierException("There is more then one identifier with type DOI!");
        } else if (doiList.size() == 1) {
            Element doiElement = doiList.stream().findAny().get();
            LOGGER.warn("Found existing DOI({}) in Document will be replaced with {}", doiElement.getTextTrim(),
                doi.asString());
            doiElement.setText(doi.asString());
        } else {
            // must be 0
            Element doiElement = new Element("identifier", DATACITE_NAMESPACE);
            datacite.getRootElement().addContent(doiElement);
            doiElement.setAttribute("identifierType", "DOI");
            doiElement.setText(doi.asString());
        }
    }

    private static Schema loadDataciteSchema() throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false);
        URL localSchemaURL = MCRDOIRegistrationService.class.getClassLoader().getResource(DATACITE_SCHEMA_PATH);

        if (localSchemaURL == null) {
            throw new MCRException(DATACITE_SCHEMA_PATH + " was not found!");
        }
        return schemaFactory.newSchema(localSchemaURL);
    }

    public boolean usesTestPrefix() {
        return useTestPrefix;
    }

    public String getRegisterURL() {
        return registerURL;
    }

    @Override
    public void validateRegistration(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException, MCRAccessException {
        List<Map.Entry<String, URI>> mediaList = getMediaList((MCRObject) obj);

        for (Map.Entry<String, URI> stringURIEntry : mediaList) {
            if (stringURIEntry.getValue().toString().length() > MAX_URL_LENGTH) {
                throw new MCRPersistentIdentifierException(
                    "The URI " + stringURIEntry + " from media-list is to long!",
                    MCRTranslation.translate(TRANSLATE_PREFIX + ERR_CODE_1_1),
                    ERR_CODE_1_1);
            }
        }

        super.validateRegistration(obj, additional);
    }

    @Override
    public MCRDigitalObjectIdentifier registerIdentifier(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        if (!additional.equals("")) {
            throw new MCRPersistentIdentifierException(
                getClass().getName() + " doesn't support additional information! (" + additional + ")");
        }

        MCRDigitalObjectIdentifier newDOI = getNewIdentifier(obj.getId(), additional);
        if (useTestPrefix) {
            newDOI = newDOI.toTestPrefix();
        }

        MCRObject mcrBase = MCRMetadataManager.retrieveMCRObject(obj.getId());
        Document dataciteDocument = transformToDatacite(newDOI, mcrBase);

        MCRDataciteClient dataciteClient = getDataciteClient();
        dataciteClient.storeMetadata(dataciteDocument);

        URI registeredURI;
        try {
            registeredURI = getRegisteredURI(obj);
            dataciteClient.mintDOI(newDOI, registeredURI);
        } catch (URISyntaxException e) {
            throw new MCRException("Base-URL seems to be invalid!", e);
        }

        List<Map.Entry<String, URI>> entryList = getMediaList((MCRObject) obj);
        dataciteClient.setMediaList(newDOI, entryList);

        return newDOI;
    }

    public URI getRegisteredURI(MCRBase obj) throws URISyntaxException {
        return new URI(this.registerURL + "/receive/" + obj.getId());
    }

    /**
     * Builds a list with with right content types and media urls assigned of a specific Object
     * @param obj the object
     * @return a list of entrys Media-Type, URL
     */
    public List<Map.Entry<String, URI>> getMediaList(MCRObject obj) {
        List<Map.Entry<String, URI>> entryList = new ArrayList<>();
        Optional<MCRObjectID> derivateIdOptional = MCRMetadataManager.getDerivateIds(obj.getId(), 1, TimeUnit.MINUTES)
            .stream().findFirst();
        derivateIdOptional.ifPresent(derivateId -> {
            MCRDerivate derivate = MCRMetadataManager.retrieveMCRDerivate(derivateId);
            String mainDoc = derivate.getDerivate().getInternals().getMainDoc();
            MCRPath mainDocumentPath = MCRPath.getPath(derivateId.toString(), mainDoc);
            try {
                String contentType = Optional.ofNullable(MCRContentTypes.probeContentType(mainDocumentPath))
                    .orElse("application/octet-stream");
                entryList.add(new AbstractMap.SimpleEntry<>(contentType, new URI(this.registerURL + MCRXMLFunctions
                    .encodeURIPath("/servlets/MCRFileNodeServlet/" + derivateId + "/" + mainDoc))));
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Error while detecting the file to register!", e);
            }
        });
        return entryList;
    }

    public MCRDataciteClient getDataciteClient() {
        return new MCRDataciteClient(host, username, password, false, this.useTestPrefix);
    }

    private Document transformToDatacite(MCRDigitalObjectIdentifier doi, MCRBase mcrBase)
        throws MCRPersistentIdentifierException {
        MCRObjectID id = mcrBase.getId();
        MCRBaseContent content = new MCRBaseContent(mcrBase);

        try {
            MCRContent transform = MCRContentTransformerFactory.getTransformer(this.transformer).transform(content);
            Document dataciteDocument = transform.asXML();
            insertDOI(dataciteDocument, doi);

            Schema dataciteSchema = loadDataciteSchema();

            try {
                dataciteSchema.newValidator().validate(new JDOMSource(dataciteDocument));
            } catch (SAXException e) {
                String translatedInformation = MCRTranslation.translate(TRANSLATE_PREFIX + ERR_CODE_1_2);
                throw new MCRPersistentIdentifierException(
                    "The document " + id + " does not generate well formed Datacite!",
                    translatedInformation, ERR_CODE_1_2, e);
            }
            return dataciteDocument;
        } catch (IOException | JDOMException | SAXException e) {
            throw new MCRPersistentIdentifierException(
                "Could not transform the content of " + id + " with the transformer " + transformer, e);
        }
    }

    @Override
    public void delete(MCRDigitalObjectIdentifier doi, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        if (MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
            .equals(MCRSystemUserInformation.getSuperUserInstance().getUserID())) {
            LOGGER.warn("SuperUser deletes object {} with registered doi {}. Try to set DOI inactive.", obj.getId(),
                doi.asString());
            try {
                getDataciteClient().deleteMetadata(doi);
            } catch (MCRPersistentIdentifierException e) {
                LOGGER.error("Error while setting {} inactive! Delete of object should continue!", doi.asString());
            }
        } else {
            throw new MCRPersistentIdentifierException("Object should not be deleted! (It has a registered DOI)");
        }
    }

    @Override
    public void update(MCRDigitalObjectIdentifier doi, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        Document newDataciteMetadata = transformToDatacite(doi, obj);
        MCRDataciteClient dataciteClient = getDataciteClient();

        try {
            URI uri = dataciteClient.resolveDOI(doi);
            URI registeredURI = getRegisteredURI(obj);
            if (!uri.equals(registeredURI)) {
                LOGGER.info("Sending new URL({}) to Datacite!", registeredURI);
                dataciteClient.mintDOI(doi, registeredURI);
            }
        } catch (URISyntaxException e) {
            throw new MCRPersistentIdentifierException("Error while updating URL!", e);
        }

        Document oldDataciteMetadata = dataciteClient.resolveMetadata(doi);
        if (!MCRXMLHelper.deepEqual(newDataciteMetadata, oldDataciteMetadata)) {
            LOGGER.info("Sending new Metadata of {} to Datacite!", obj.getId().toString());
            dataciteClient.deleteMetadata(doi);
            dataciteClient.storeMetadata(newDataciteMetadata);
        }
    }

}

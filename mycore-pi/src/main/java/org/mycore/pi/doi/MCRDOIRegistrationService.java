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
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

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
import org.mycore.backend.hibernate.MCRHIBConnection;
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
import org.mycore.pi.MCRPIJobRegistrationService;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.services.i18n.MCRTranslation;
import org.xml.sax.SAXException;

/**
 * Registers {@link MCRDigitalObjectIdentifier} at Datacite.
 *
 * Properties:
 * <dl>
 *     <dt>MetadataManager</dt>
 *     <dd>A metadata manager which inserts the {@link MCRDigitalObjectIdentifier} to a object</dd>
 *     <dt>Generator</dt>
 *     <dd>A {@link org.mycore.pi.MCRPersistentIdentifierGenerator} which generates {@link MCRDigitalObjectIdentifier}</dd>
 *     <dt>Username</dt>
 *     <dd>The username which will be used for authentication </dd>
 *     <dt>Password</dt>
 *     <dd>The password which will be used for authentication</dd>
 *     <dt>RegisterBaseURL</dt>
 *     <dd>The BaseURL (everything before /receive/mcr_object_0000000) which will be send to Datacite.</dd>
 *     <dt>UseTestPrefix</dt>
 *     <dd>If true the prefix of the created {@link MCRDigitalObjectIdentifier} will be replaced with the Datacite test
 *     prefix</dd>
 *     <dt>RegistrationConditionProvider</dt>
 *     <dd>Used to detect if the registration should happen. DOI will be created but the real registration will if the
 *     Condition is true. The Parameter is optional and the default condition is always true.</dd>
 *     <dt>Schema</dt>
 *     <dd>The path to the schema. (must be in classpath; default is {@link #DEFAULT_DATACITE_SCHEMA_PATH})</dd>
 *     <dt>Namespace</dt>
 *     <dd>The namespace for the Datacite version (Default is {@link #KERNEL_3_NAMESPACE_URI}</dd>
 * </dl>
 */
public class MCRDOIRegistrationService extends MCRPIJobRegistrationService<MCRDigitalObjectIdentifier> {

    private static final String KERNEL_3_NAMESPACE_URI = "http://datacite.org/schema/kernel-3";

    private static final String TEST_PREFIX = "UseTestPrefix";

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

    private static final String DEFAULT_DATACITE_SCHEMA_PATH = "xsd/datacite/v3/metadata.xsd";

    private static final String TRANSLATE_PREFIX = "component.pi.register.error.";

    private static final String DEFAULT_CONTEXT_PATH = "receive/$ID";

    private static final String CONTEXT_OBJ = "obj";

    private static final String CONTEXT_DOI = "doi";

    private String username;

    private String password;

    private String transformer;

    private String host;

    private String registerURL;

    private String registerURLContext;

    private String schemaPath;

    private Namespace nameSpace;

    private boolean useTestPrefix;

    public MCRDOIRegistrationService(String serviceID) {
        super(serviceID, MCRDigitalObjectIdentifier.TYPE);

        Map<String, String> properties = getProperties();
        username = properties.get("Username");
        password = properties.get("Password");
        useTestPrefix = properties.containsKey(TEST_PREFIX) && Boolean.valueOf(properties.get(TEST_PREFIX));
        transformer = properties.get("Transformer");
        registerURL = properties.get("RegisterBaseURL");
        schemaPath = properties.getOrDefault("Schema", DEFAULT_DATACITE_SCHEMA_PATH);
        nameSpace = Namespace.getNamespace("datacite", properties.getOrDefault("Namespace", KERNEL_3_NAMESPACE_URI));

        if (!registerURL.endsWith("/")) {
            registerURL += "/";
        }

        registerURLContext = properties.getOrDefault("RegisterURLContext", DEFAULT_CONTEXT_PATH);
        host = "mds.datacite.org";
    }

    private void insertDOI(Document datacite, MCRDigitalObjectIdentifier doi)
        throws MCRPersistentIdentifierException {
        XPathExpression<Element> compile = XPathFactory.instance().compile(
            "//datacite:identifier[@identifierType='DOI']",
            Filters.element(), null, nameSpace);
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
            Element doiElement = new Element("identifier", nameSpace);
            datacite.getRootElement().addContent(doiElement);
            doiElement.setAttribute("identifierType", "DOI");
            doiElement.setText(doi.asString());
        }
    }

    private Schema loadDataciteSchema() throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false);
        URL localSchemaURL = MCRDOIRegistrationService.class.getClassLoader().getResource(schemaPath);

        if (localSchemaURL == null) {
            throw new MCRException(DEFAULT_DATACITE_SCHEMA_PATH + " was not found!");
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
    protected Date provideRegisterDate(MCRBase obj, String additional) {
        return null;
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

        // just to check if valid
        transformToDatacite(newDOI, obj);

        return newDOI;
    }

    @Override
    public MCRPI insertIdentifierToDatabase(MCRBase obj, String additional, MCRDigitalObjectIdentifier identifier) {
        Date registrationStarted = null;
        if (getRegistrationCondition(obj.getId().getTypeId()).test(obj)) {
            registrationStarted = new Date();
            startRegisterJob(obj, identifier);
        }

        MCRPI databaseEntry = new MCRPI(identifier.asString(), getType(), obj.getId().toString(), additional,
            this.getRegistrationServiceID(), provideRegisterDate(obj, additional), registrationStarted);
        MCRHIBConnection.instance().getSession().save(databaseEntry);
        return databaseEntry;
    }

    private void startRegisterJob(MCRBase obj, MCRDigitalObjectIdentifier newDOI) {
        HashMap<String, String> contextParameters = new HashMap<>();
        contextParameters.put(CONTEXT_DOI, newDOI.asString());
        contextParameters.put(CONTEXT_OBJ, obj.getId().toString());
        this.addRegisterJob(contextParameters);
    }

    public URI getRegisteredURI(MCRBase obj) throws URISyntaxException {
        return new URI(this.registerURL + registerURLContext.replaceAll("\\$[iI][dD]", obj.getId().toString()));
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

    protected Document transformToDatacite(MCRDigitalObjectIdentifier doi, MCRBase mcrBase)
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
        if (hasRegistrationStarted(obj.getId(), additional) || this.isRegistered(obj.getId(), additional)) {
            if (MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
                .equals(MCRSystemUserInformation.getSuperUserInstance().getUserID())) {
                LOGGER.warn("SuperUser deletes object {} with registered doi {}. Try to set DOI inactive.", obj.getId(),
                    doi.asString());
                if (this.isRegistered(obj.getId(), additional)) {
                    HashMap<String, String> contextParameters = new HashMap<>();
                    contextParameters.put(CONTEXT_DOI, doi.asString());
                    contextParameters.put(CONTEXT_OBJ, obj.getId().toString());
                    this.addDeleteJob(contextParameters);
                }
            } else {
                throw new MCRPersistentIdentifierException("Object should not be deleted! (It has a registered DOI)");
            }
        }
        // just delete
    }

    @Override
    public void update(MCRDigitalObjectIdentifier doi, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        if (isRegistered(obj.getId(), additional)) {
            HashMap<String, String> contextParameters = new HashMap<>();
            contextParameters.put(CONTEXT_DOI, doi.asString());
            contextParameters.put(CONTEXT_OBJ, obj.getId().toString());
            this.addUpdateJob(contextParameters);
        } else if (!hasRegistrationStarted(obj.getId(), additional)) {
            Predicate<MCRBase> registrationCondition = getRegistrationCondition(obj.getId().getTypeId());
            if (registrationCondition.test(obj)) {
                this.updateStartRegistrationDate(obj.getId(), "", new Date());
                startRegisterJob(obj, doi);
            }
        }
    }

    @Override
    public void deleteJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        MCRDigitalObjectIdentifier doi = getDOIFromJob(parameters);

        try {
            getDataciteClient().deleteMetadata(doi);
        } catch (MCRPersistentIdentifierException e) {
            LOGGER.error("Error while setting {} inactive! Delete of object should continue!", doi.asString());
        }
    }

    @Override
    public void updateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        MCRDigitalObjectIdentifier doi = getDOIFromJob(parameters);
        String idString = parameters.get(CONTEXT_OBJ);

        MCRObjectID objectID = MCRObjectID.getInstance(idString);
        this.validateJobUserRights(objectID);
        MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);

        Document newDataciteMetadata = transformToDatacite(doi, object);
        MCRDataciteClient dataciteClient = getDataciteClient();

        try {
            URI uri = dataciteClient.resolveDOI(doi);
            URI registeredURI = getRegisteredURI(object);
            if (!uri.equals(registeredURI)) {
                LOGGER.info("Sending new URL({}) to Datacite!", registeredURI);
                dataciteClient.mintDOI(doi, registeredURI);
            }
        } catch (URISyntaxException e) {
            throw new MCRPersistentIdentifierException("Error while updating URL!", e);
        }

        Document oldDataciteMetadata = dataciteClient.resolveMetadata(doi);
        if (!MCRXMLHelper.deepEqual(newDataciteMetadata, oldDataciteMetadata)) {
            LOGGER.info("Sending new Metadata of {} to Datacite!", idString);
            dataciteClient.deleteMetadata(doi);
            dataciteClient.storeMetadata(newDataciteMetadata);
        }
    }

    @Override
    public void registerJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        MCRDigitalObjectIdentifier doi = getDOIFromJob(parameters);
        String idString = parameters.get(CONTEXT_OBJ);

        MCRObjectID objectID = MCRObjectID.getInstance(idString);
        this.validateJobUserRights(objectID);
        MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);

        MCRObject mcrBase = MCRMetadataManager.retrieveMCRObject(objectID);
        Document dataciteDocument = transformToDatacite(doi, mcrBase);

        MCRDataciteClient dataciteClient = getDataciteClient();
        dataciteClient.storeMetadata(dataciteDocument);

        URI registeredURI;
        try {
            registeredURI = getRegisteredURI(object);
            dataciteClient.mintDOI(doi, registeredURI);
        } catch (URISyntaxException e) {
            throw new MCRException("Base-URL seems to be invalid!", e);
        }

        List<Map.Entry<String, URI>> entryList = getMediaList((MCRObject) object);
        dataciteClient.setMediaList(doi, entryList);
        this.updateRegistrationDate(objectID, "", new Date());
    }

    /**
     * Gets the {@link MCRDigitalObjectIdentifier} from the job parameters. This method does not ensure that the
     * returned {@link MCRDigitalObjectIdentifier} object instance is the same as the generated one.
     * @param parameters the job parameters
     * @return the parsed DOI
     * @throws MCRPersistentIdentifierException
     */
    private MCRDigitalObjectIdentifier getDOIFromJob(Map<String, String> parameters)
        throws MCRPersistentIdentifierException {
        String doiString = parameters.get(CONTEXT_DOI);
        return parseIdentifier(doiString)
            .orElseThrow(
                () -> new MCRPersistentIdentifierException("The String " + doiString + " can not be parsed to a DOI!"));
    }

    @Override
    protected Optional<String> getJobInformation(Map<String, String> contextParameters) {
        String pattern = "{0} DOI: {1} for object: {2}";
        return Optional.of(MessageFormat
            .format(pattern, getAction(contextParameters).toString(), contextParameters.get(CONTEXT_DOI),
                contextParameters.get(CONTEXT_OBJ)));
    }

}

package org.mycore.pi.doi;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.persistence.NoResultException;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.jdom2.Document;
import org.jdom2.transform.JDOMSource;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIJobService;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.xml.sax.SAXException;

/**
 * A doi Base Service which contains common DOI registration code.
 */
public abstract class MCRDOIBaseService extends MCRPIJobService<MCRDigitalObjectIdentifier> {

    protected static final String CONTEXT_OBJ = "obj";

    protected static final String CONTEXT_DOI = "doi";

    private static final String CONFIG_TRANSFORMER = "Transformer";

    private static final String CONFIG_USER_NAME = "Username";

    private static final String CONFIG_PASSWORD = "Password";

    private static final String CONFIG_SCHEMA = "Schema";

    private static final String TYPE = "doi";

    private String username;

    private String password;

    private Schema schema;

    private String transformerID;

    public MCRDOIBaseService(String registrationServiceID) {
        super(registrationServiceID, TYPE);
    }

    protected void initCommonProperties() {
        setUsername(requireNotEmptyProperty(CONFIG_USER_NAME));
        setPassword(requireNotEmptyProperty(CONFIG_PASSWORD));
        setTransformerID(requireNotEmptyProperty(CONFIG_TRANSFORMER));

        final MCRContentTransformer transformer = getTransformer();

        final Map<String, String> properties = getProperties();
        final String schemaURL = properties.getOrDefault(CONFIG_SCHEMA, getDefaultSchemaPath());
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false);

            URL localSchemaURL = MCRClassTools.getClassLoader().getResource(schemaURL);
            if (localSchemaURL == null) {
                throw new MCRConfigurationException(schemaURL + " was not found!");
            }
            setSchema(schemaFactory.newSchema(localSchemaURL));
        } catch (SAXException e) {
            throw new MCRConfigurationException("Error while loading " + getServiceID() + " schema!", e);
        }
    }

    protected abstract String getDefaultSchemaPath();

    @Override
    protected Optional<String> getJobInformation(Map<String, String> contextParameters) {
        String pattern = "{0} DOI: {1} for object: {2}";
        return Optional.of(String.format(Locale.ROOT, pattern, getAction(contextParameters).toString(),
            contextParameters.get(CONTEXT_DOI), contextParameters.get(CONTEXT_OBJ)));
    }

    protected boolean checkJobValid(String mycoreID, PiJobAction action) {
        final MCRObjectID objectID = MCRObjectID.getInstance(mycoreID);
        final boolean exists = MCRMetadataManager.exists(objectID);

        try {
            MCRPIManager.getInstance().get(getServiceID(), mycoreID, "");
        } catch (NoResultException r) {
            return false;
        }

        return exists;
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
            Predicate<MCRBase> registrationCondition = getRegistrationCondition();
            if (registrationCondition.test(obj)) {
                // validate
                transform(obj, doi.asString());
                this.updateStartRegistrationDate(obj.getId(), "", new Date());
                startRegisterJob(obj, doi);
            }
        }
    }

    @Override
    public MCRPI insertIdentifierToDatabase(MCRBase obj, String additional, MCRDigitalObjectIdentifier identifier) {
        Date registrationStarted = null;
        if (getRegistrationCondition().test(obj)) {
            registrationStarted = new Date();
            startRegisterJob(obj, identifier);
        }

        MCRPI databaseEntry = new MCRPI(identifier.asString(), getType(), obj.getId().toString(), additional,
            this.getServiceID(), provideRegisterDate(obj, additional), registrationStarted);
        MCRHIBConnection.instance().getSession().save(databaseEntry);
        return databaseEntry;
    }

    protected void startRegisterJob(MCRBase obj, MCRDigitalObjectIdentifier newDOI) {
        HashMap<String, String> contextParameters = new HashMap<>();
        contextParameters.put(CONTEXT_DOI, newDOI.asString());
        contextParameters.put(CONTEXT_OBJ, obj.getId().toString());
        this.addRegisterJob(contextParameters);
    }

    protected MCRContentTransformer getTransformer() {
        return MCRContentTransformerFactory.getTransformer(transformerID);
    }

    protected void validateDocument(String id, Document resultDocument)
        throws MCRPersistentIdentifierException {
        try {
            getSchema().newValidator().validate(new JDOMSource(resultDocument));
        } catch (SAXException | IOException e) {
            throw new MCRPersistentIdentifierException(
                "Error while validating generated xml for " + id, e);
        }
    }

    @Override
    protected Date provideRegisterDate(MCRBase obj, String additional) {
        return null;
    }

    protected abstract Document transform(MCRBase obj, String pi)
        throws MCRPersistentIdentifierException;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public String getTransformerID() {
        return transformerID;
    }

    public void setTransformerID(String transformerID) {
        this.transformerID = transformerID;
    }
}

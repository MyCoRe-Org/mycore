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

package org.mycore.pi.doi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.xml.validation.Schema;

import org.jdom2.Document;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIJobService;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.xml.sax.SAXException;

import jakarta.persistence.NoResultException;

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

    public MCRDOIBaseService() {
        super(TYPE);
    }

    protected void initCommonProperties() {
        setUsername(requireNotEmptyProperty(CONFIG_USER_NAME));
        setPassword(requireNotEmptyProperty(CONFIG_PASSWORD));
        setTransformerID(requireNotEmptyProperty(CONFIG_TRANSFORMER));

        final Map<String, String> properties = getProperties();
        final String schemaURLString = properties.getOrDefault(CONFIG_SCHEMA, getDefaultSchemaPath());
        setSchema(resolveSchema(schemaURLString));
    }

    public static Schema resolveSchema(final String schemaURLString) {
        try {
            return MCRXMLHelper.resolveSchema(schemaURLString, true);
        } catch (SAXException | IOException e) {
            throw new MCRConfigurationException("Error while loading " + schemaURLString + " schema!", e);
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
    protected HashMap<String, String> createJobContextParams(PiJobAction action, MCRBase obj,
        MCRDigitalObjectIdentifier doi, String additional) {
        HashMap<String, String> params = new HashMap<>();
        params.put(CONTEXT_DOI, doi.asString());
        params.put(CONTEXT_OBJ, obj.getId().toString());
        return params;
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

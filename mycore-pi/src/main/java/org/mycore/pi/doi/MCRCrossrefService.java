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
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.naming.OperationNotSupportedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRBaseContent;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.pi.doi.client.crossref.MCRCrossrefClient;
import org.mycore.pi.doi.crossref.MCRCrossrefUtil;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRCrossrefService extends MCRDOIBaseService {

    private static final String CONFIG_TEST = "Test";

    private static final String CONFIG_REGISTRANT = "Registrant";

    private static final String CONFIG_DEPOSITOR_MAIL = "DepositorMail";

    private static final String CONFIG_DEPOSITOR = "Depositor";

    public static final String DEFAULT_SCHEMA = "http://data.crossref.org/schemas/crossref4.4.1.xsd";

    private static final String TEST_HOST = "test.crossref.org";

    private static final String PRODUCTION_HOST = "doi.crossref.org";

    private static final Logger LOGGER = LogManager.getLogger();

    private String registrant;

    private String depositor;

    private String depositorMail;

    @Override
    protected String getDefaultSchemaPath() {
        return DEFAULT_SCHEMA;
    }

    @Override
    protected void checkConfiguration() throws MCRConfigurationException {
        super.checkConfiguration();
        init();
    }

    private void init() {
        initCommonProperties();
        registrant = requireNotEmptyProperty(CONFIG_REGISTRANT);
        depositor = requireNotEmptyProperty(CONFIG_DEPOSITOR);
        depositorMail = requireNotEmptyProperty(CONFIG_DEPOSITOR_MAIL);
    }

    @Override
    protected void registerIdentifier(MCRBase obj, String additional, MCRDigitalObjectIdentifier pi)
        throws MCRPersistentIdentifierException {
        final Document resultDocument = transform(obj, pi.asString());
        validateDocument(obj.getId().toString(), resultDocument);
    }

    @Override
    protected Document transform(MCRBase obj, String pi)
        throws MCRPersistentIdentifierException {
        Document resultDocument;
        try {
            final MCRContent result = getTransformer().transform(new MCRBaseContent(obj));
            resultDocument = result.asXML();
        } catch (IOException | JDOMException e) {
            throw new MCRConfigurationException(
                String.format(Locale.ROOT, "Could not transform the object %s with the trasformer %s", obj.getId(),
                    getTransformerID()),
                e);
        }

        final Element root = resultDocument.getRootElement();

        final Element headElement = root.getChild("head", MCRConstants.CROSSREF_NAMESPACE);
        final String batchID = UUID.randomUUID() + "_" + obj.getId();
        final String timestampMilliseconds = String.valueOf(new Date().getTime());
        MCRCrossrefUtil.insertBatchInformation(headElement, batchID, timestampMilliseconds, depositor, depositorMail,
            registrant);

        MCRCrossrefUtil.replaceDOIData(root, (objectID) -> Objects.equals(obj.getId().toString(), objectID) ? pi : null,
            MCRFrontendUtil.getBaseURL());

        return resultDocument;
    }

    private String getHost() {
        return Optional.ofNullable(getProperties().get(CONFIG_TEST))
            .map(Boolean::valueOf)
            .map(testMode -> testMode ? TEST_HOST : PRODUCTION_HOST)
            .orElse(PRODUCTION_HOST);
    }

    @Override
    protected void delete(MCRDigitalObjectIdentifier identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        throw new MCRPersistentIdentifierException("Delete is not Supported!",
            new OperationNotSupportedException("Delete is not Supported!"));
    }

    @Override
    protected void deleteJob(Map<String, String> parameters) {

    }

    @Override
    protected void updateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        String doi = parameters.get(CONTEXT_DOI);
        String idString = parameters.get(CONTEXT_OBJ);

        if (!checkJobValid(idString, PiJobAction.UPDATE)) {
            return;
        }

        MCRObjectID objectID = MCRObjectID.getInstance(idString);
        this.validateJobUserRights(objectID);
        MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);

        Document newCrossrefBatch = transform(object, doi);
        final MCRCrossrefClient client = new MCRCrossrefClient(getHost(), getUsername(), getPassword());

        client.doMDUpload(newCrossrefBatch);
    }

    @Override
    protected void registerJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        String doi = parameters.get(CONTEXT_DOI);
        String idString = parameters.get(CONTEXT_OBJ);

        if (!checkJobValid(idString, PiJobAction.REGISTER)) {
            return;
        }

        MCRObjectID objectID = MCRObjectID.getInstance(idString);
        this.validateJobUserRights(objectID);
        MCRObject mcrBase = MCRMetadataManager.retrieveMCRObject(objectID);

        final Document resultDocument = transform(mcrBase, doi);
        final MCRCrossrefClient client = new MCRCrossrefClient(getHost(), getUsername(), getPassword());
        client.doMDUpload(resultDocument);
    }

}

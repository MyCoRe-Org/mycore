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

package org.mycore.pi.purl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIJobRegistrationService;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPURLJobRegistrationService extends MCRPIJobRegistrationService<MCRPersistentUniformResourceLocator> {

    private static final String TYPE = "purl";

    private static final String CONTEXT_PURL = "PURL";

    private static final String CONTEXT_OBJECT = "ObjectID";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PURL_SERVER_CONFIG = "Server";

    private static final String PURL_USER_CONFIG = "Username";

    private static final String PURL_PASSWORD_CONFIG = "Password";

    private static final String PURL_BASE_URL = "RegisterBaseURL";

    public MCRPURLJobRegistrationService(String registrationServiceID) {
        super(registrationServiceID, TYPE);
    }

    @Override
    public void registerJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        MCRPersistentUniformResourceLocator purl = getPURLFromJob(parameters);
        String idString = parameters.get(CONTEXT_OBJECT);
        LOGGER.info("TODO: Register PURL at PURL-Server!");

        doWithPURLManager(
            manager -> manager.registerNewPURL(purl.getUrl().getPath(), buildTargetURL(idString), "302", "test")
        );
        this.updateStartRegistrationDate(MCRObjectID.getInstance(idString), "", new Date());
    }

    private String buildTargetURL(String objId) {
        String baseURL = getProperties().get(PURL_BASE_URL);
        return baseURL + "receive/" + objId;
    }

    @Override
    protected Optional<String> getJobInformation(Map<String, String> contextParameters) {
        return Optional.empty();
    }

    private MCRPersistentUniformResourceLocator getPURLFromJob(Map<String, String> parameters)
        throws MCRPersistentIdentifierException {
        String purlString = parameters.get(CONTEXT_PURL);

        try {
            return new MCRPersistentUniformResourceLocator(new URL(purlString));
        } catch (MalformedURLException e) {
            throw new MCRPersistentIdentifierException("Cannot parse " + purlString);
        }
    }

    @Override
    public void updateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {

    }

    @Override
    public void deleteJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {

    }

    @Override
    public MCRPI insertIdentifierToDatabase(MCRBase obj, String additional,
        MCRPersistentUniformResourceLocator identifier) {
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

    @Override
    protected MCRPersistentUniformResourceLocator registerIdentifier(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        if (!additional.equals("")) {
            throw new MCRPersistentIdentifierException(
                getClass().getName() + " doesn't support additional information! (" + additional + ")");
        }

        MCRPersistentUniformResourceLocator purl = getNewIdentifier(obj.getId(), additional);

        MCRPURLManager app = new MCRPURLManager();

        return purl;
    }

    @Override
    protected void delete(MCRPersistentUniformResourceLocator identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {

    }

    @Override
    protected void update(MCRPersistentUniformResourceLocator purl, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        if (!hasRegistrationStarted(obj.getId(), additional)) {
            Predicate<MCRBase> registrationCondition = getRegistrationCondition(obj.getId().getTypeId());
            if (registrationCondition.test(obj)) {
                this.updateStartRegistrationDate(obj.getId(), "", new Date());
                startRegisterJob(obj, purl);
            }
        }
    }

    private void startRegisterJob(MCRBase obj, MCRPersistentUniformResourceLocator newDOI) {
        HashMap<String, String> contextParameters = new HashMap<>();
        contextParameters.put(CONTEXT_PURL, newDOI.asString());
        contextParameters.put(CONTEXT_OBJECT, obj.getId().toString());
        this.addRegisterJob(contextParameters);
    }

    private void doWithPURLManager(Consumer<MCRPURLManager> action) {
        Map<String, String> props = getProperties();
        String serverURL = props.get(PURL_SERVER_CONFIG);
        String username = props.get(PURL_USER_CONFIG);
        String password = props.get(PURL_PASSWORD_CONFIG);

        MCRPURLManager manager = new MCRPURLManager();
        manager.login(serverURL, username, password);

        try {
            action.accept(manager);
        } finally {
            manager.logout();
        }
    }
}

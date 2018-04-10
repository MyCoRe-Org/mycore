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

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIJobService;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPURLJobService extends MCRPIJobService<MCRPURL> {

    private static final String TYPE = "purl";

    private static final String CONTEXT_PURL = "PURL";

    private static final String CONTEXT_OBJECT = "ObjectID";

    private static final String PURL_SERVER_CONFIG = "Server";

    private static final String PURL_USER_CONFIG = "Username";

    private static final String PURL_PASSWORD_CONFIG = "Password";

    private static final String PURL_BASE_URL = "RegisterBaseURL";

    private static final String PURL_CONTEXT_CONFIG = "RegisterContext";

    private static final String PURL_MAINTAINER_CONFIG = "Maintainer";

    private static final String DEFAULT_CONTEXT_PATH = "receive/$ID";

    public MCRPURLJobService(String registrationServiceID) {
        super(registrationServiceID, TYPE);
    }

    @Override
    public void registerJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        MCRPURL purl = getPURLFromJob(parameters);
        String idString = parameters.get(CONTEXT_OBJECT);
        validateJobUserRights(MCRObjectID.getInstance(idString));

        doWithPURLManager(
            manager -> manager
                .registerNewPURL(purl.getUrl().getPath(), buildTargetURL(idString), "302", getProperties().getOrDefault(
                    PURL_MAINTAINER_CONFIG, "test"))
        );
        this.updateStartRegistrationDate(MCRObjectID.getInstance(idString), "", new Date());
    }

    private String buildTargetURL(String objId) {
        String baseURL = getProperties().get(PURL_BASE_URL);
        return baseURL + getProperties().getOrDefault(PURL_CONTEXT_CONFIG, DEFAULT_CONTEXT_PATH)
            .replaceAll("\\$[iI][dD]", objId);

    }

    @Override
    protected Optional<String> getJobInformation(Map<String, String> contextParameters) {
        return Optional.empty();
    }

    private MCRPURL getPURLFromJob(Map<String, String> parameters)
        throws MCRPersistentIdentifierException {
        String purlString = parameters.get(CONTEXT_PURL);

        try {
            return new MCRPURL(new URL(purlString));
        } catch (MalformedURLException e) {
            throw new MCRPersistentIdentifierException("Cannot parse " + purlString);
        }
    }

    @Override
    public void updateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        String purlString = parameters.get(CONTEXT_PURL);
        String objId = parameters.get(CONTEXT_OBJECT);

        validateJobUserRights(MCRObjectID.getInstance(objId));
        MCRPURL purl;

        try {
            purl = new MCRPURL(
                new URL(purlString));
        } catch (MalformedURLException e) {
            throw new MCRPersistentIdentifierException("Could not parse purl: " + purlString, e);
        }

        doWithPURLManager((purlManager) -> {
            if (!purlManager.isPURLTargetURLUnchanged(purl.getUrl().toString(), buildTargetURL(
                objId))) {
                purlManager.updateExistingPURL(purl.getUrl().getPath(), buildTargetURL(objId), "302",
                    getProperties().getOrDefault(
                        PURL_MAINTAINER_CONFIG, "test"));
            }
        });
    }

    @Override
    public void deleteJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        // deleting ist not supported
    }

    @Override
    public MCRPI insertIdentifierToDatabase(MCRBase obj, String additional,
        MCRPURL identifier) {
        Date registrationStarted = null;
        if (getRegistrationCondition(obj.getId().getTypeId()).test(obj)) {
            registrationStarted = new Date();
            startRegisterJob(obj, identifier);
        }

        MCRPI databaseEntry = new MCRPI(identifier.asString(), getType(), obj.getId().toString(), additional,
            this.getServiceID(), provideRegisterDate(obj, additional), registrationStarted);
        MCRHIBConnection.instance().getSession().save(databaseEntry);
        return databaseEntry;
    }

    @Override
    protected void registerIdentifier(MCRBase obj, String additional, MCRPURL purl)
        throws MCRPersistentIdentifierException {
        if (!"".equals(additional)) {
            throw new MCRPersistentIdentifierException(
                getClass().getName() + " doesn't support additional information! (" + additional + ")");
        }
    }

    @Override
    protected void delete(MCRPURL identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        // not supported
    }

    @Override
    protected void update(MCRPURL purl, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        if (!hasRegistrationStarted(obj.getId(), additional)) {
            Predicate<MCRBase> registrationCondition = getRegistrationCondition(obj.getId().getTypeId());
            if (registrationCondition.test(obj)) {
                this.updateStartRegistrationDate(obj.getId(), "", new Date());
                startRegisterJob(obj, purl);
            }
        } else {
            if (isRegistered(obj.getId(), "")) {
                startUpdateJob(obj, purl);
            }
        }
    }

    private void startUpdateJob(MCRBase obj, MCRPURL purl) {
        HashMap<String, String> contextParameters = new HashMap<>();
        contextParameters.put(CONTEXT_PURL, purl.asString());
        contextParameters.put(CONTEXT_OBJECT, obj.getId().toString());
        this.addUpdateJob(contextParameters);
    }

    private void startRegisterJob(MCRBase obj, MCRPURL purl) {
        HashMap<String, String> contextParameters = new HashMap<>();
        contextParameters.put(CONTEXT_PURL, purl.asString());
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

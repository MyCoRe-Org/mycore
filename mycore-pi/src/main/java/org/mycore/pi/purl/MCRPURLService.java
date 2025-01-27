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

package org.mycore.pi.purl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIJobService;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPURLService extends MCRPIJobService<MCRPURL> {

    private static final String TYPE = "purl";

    private static final String CONTEXT_PURL = "PURL";

    private static final String CONTEXT_OBJECT = "ObjectID";

    private static final String PURL_SERVER_CONFIG = "Server";

    private static final String PURL_USER_CONFIG = "Username";

    private static final String PURL_PASSWORD_CONFIG = "Password";

    private static final String PURL_BASE_URL = "RegisterBaseURL";

    private static final String PURL_CONTEXT_CONFIG = "RegisterURLContext";

    private static final String PURL_MAINTAINER_CONFIG = "Maintainer";

    private static final String DEFAULT_CONTEXT_PATH = "receive/$ID";

    public MCRPURLService() {
        super(TYPE);
    }

    @Override
    public void registerJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        MCRPURL purl = getPURLFromJob(parameters);
        String idString = parameters.get(CONTEXT_OBJECT);
        validateJobUserRights(MCRObjectID.getInstance(idString));

        doWithPURLManager(
            manager -> manager
                .registerNewPURL(purl.getUrl().getPath(), buildTargetURL(idString), "302", getProperties().getOrDefault(
                    PURL_MAINTAINER_CONFIG, "test")));
        this.updateStartRegistrationDate(MCRObjectID.getInstance(idString), "", new Date());
    }

    protected String buildTargetURL(String objId) {
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
            return new MCRPURL(new URI(purlString).toURL());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new MCRPersistentIdentifierException("Cannot parse " + purlString, e);
        }
    }

    @Override
    public void updateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        String purlString = parameters.get(CONTEXT_PURL);
        String objId = parameters.get(CONTEXT_OBJECT);

        validateJobUserRights(MCRObjectID.getInstance(objId));
        MCRPURL purl;

        try {
            purl = new MCRPURL(new URI(purlString).toURL());
        } catch (MalformedURLException | URISyntaxException e) {
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
    public void deleteJob(Map<String, String> parameters) {
        // delete is not supported
    }

    @Override
    protected void delete(MCRPURL identifier, MCRBase obj, String additional) {
        // not supported
    }

    @Override
    protected boolean validateRegistrationDocument(MCRBase obj, MCRPURL identifier, String additional) {
        return true;
    }

    @Override
    protected Map<String, String> createJobContextParams(PiJobAction action, MCRBase obj, MCRPURL purl,
        String additional) {
        Map<String, String> params = new HashMap<>();
        params.put(CONTEXT_PURL, purl.asString());
        params.put(CONTEXT_OBJECT, obj.getId().toString());
        return params;
    }

    protected void doWithPURLManager(Consumer<MCRPURLManager> action) {
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

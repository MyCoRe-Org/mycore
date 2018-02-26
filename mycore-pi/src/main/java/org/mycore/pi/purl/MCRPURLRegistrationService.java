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

import java.util.Map;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPURLRegistrationService extends MCRPIRegistrationService<MCRPersistentUniformResourceLocator> {

    private static final String TYPE = "purl";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PURL_SERVER_CONFIG = "Server";

    private static final String PURL_USER_CONFIG = "Username";

    private static final String PURL_PASSWORD_CONFIG = "Password";

    private static final String PURL_MAINTAINER_CONFIG = "Maintainer";

    private static final String PURL_BASE_URL = "RegisterBaseURL";

    private static final String PURL_CONTEXT_CONFIG = "RegisterContext";

    private static final String DEFAULT_CONTEXT_PATH = "receive/$ID";

    public MCRPURLRegistrationService(String registrationServiceID) {
        super(registrationServiceID, TYPE);
    }

    @Override
    protected MCRPersistentUniformResourceLocator registerIdentifier(MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        if (!"".equals(additional)) {
            throw new MCRPersistentIdentifierException(
                getClass().getName() + " doesn't support additional information! (" + additional + ")");
        }

        MCRPersistentUniformResourceLocator purl = getNewIdentifier(obj.getId(), additional);
        LOGGER.info("TODO: Register PURL at PURL-Server!");

        doWithPURLManager(
            manager -> manager
                .registerNewPURL(purl.getUrl().getPath(), buildTargetURL(obj), "302", getProperties().getOrDefault(
                    PURL_MAINTAINER_CONFIG, "test")));

        return purl;
    }

    protected String buildTargetURL(MCRBase obj) {
        String baseURL = getProperties().get(PURL_BASE_URL);
        return baseURL + getProperties().getOrDefault(PURL_CONTEXT_CONFIG, DEFAULT_CONTEXT_PATH)
            .replaceAll("\\$[iI][dD]", obj.getId().toString());
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

    @Override
    protected void delete(MCRPersistentUniformResourceLocator identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        /* deletion is not supported */
        throw new MCRPersistentIdentifierException("Delete is not supported for " + getType());
    }

    @Override
    protected void update(MCRPersistentUniformResourceLocator purl, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        doWithPURLManager((purlManager) -> {
            String targetURL = buildTargetURL(obj);
            if (!purlManager.isPURLTargetURLUnchanged(purl.getUrl().toString(), targetURL)) {
                purlManager.updateExistingPURL(purl.getUrl().getPath(), targetURL, "302",
                    getProperties().getOrDefault(
                        PURL_MAINTAINER_CONFIG, "test"));
            }
        });

    }

}

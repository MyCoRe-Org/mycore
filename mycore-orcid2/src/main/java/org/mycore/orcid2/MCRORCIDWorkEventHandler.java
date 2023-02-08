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

package org.mycore.orcid2;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.user2.MCRUser;
import org.mycore.orcid2.user.MCRORCIDCredentials;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDUserUtils;
import org.mycore.orcid2.user.MCRIdentifier;
import org.orcid.jaxb.model.message.ScopeConstants;

/**
 * When a publication is created or updated locally in this application,
 * collects all orcid name identifiers from the MODS metadata,
 * looks up login users that have one of these identifiers stored in their user attributes,
 * checks if these users have an ORCID profile we know of
 * and have authorized us to update their profile as trusted party,
 * and then creates/updates the publication in the works section of that profile.
 */
public abstract class MCRORCIDWorkEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject object) {
        handlePublication(object);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject object) {
        handlePublication(object);
    }

    private void handlePublication(MCRObject object) {
        if (!MCRMODSWrapper.isSupported(object)) {
            return;
        }
        for (Element nameElement : MCRORCIDUtils.listNameElements(new MCRMODSWrapper(object))) {
            MCRUser user = null;
            String orcid = null;
            final List<MCRIdentifier> ids = MCRORCIDUtils.getNameIdentifiers(nameElement).stream()
                .filter(i -> MCRORCIDUser.TRUSTED_NAME_IDENTIFIER_TYPES.contains(i.getType())).toList();
            for (MCRIdentifier id : ids) {
                final List<MCRUser> tmp = MCRORCIDUserUtils.getUserByID(id).stream().toList();
                if (tmp.size() == 1) {
                    user = tmp.get(0);
                    if ("orcid".equals(id.getType())) {
                        orcid = id.getValue();
                    }
                } else if (tmp.size() > 1) {
                    user = null;
                    LOGGER.info("Found more than one user for id: {}", id);
                    break;
                }
            }
            if (user != null) {
                MCRORCIDCredentials credentials = null;
                if (orcid != null) {
                    credentials = MCRORCIDUserUtils.getCredentials(orcid); // save because of uniqueness check
                } else { // try to fetch orcid user
                    final MCRORCIDUser orcidUser = new MCRORCIDUser(user);
                    final List<MCRORCIDCredentials> tmp = orcidUser.listCredentials();
                    if (tmp.size() == 1) {
                        credentials = tmp.get(0);
                    } else {
                        LOGGER.info("Found more than one ORCID credentials for user: {}", user.getUserID());
                        continue;
                    }
                }
                if (credentials != null) {
                    final String scope = credentials.getScope();
                    if (scope != null && !scope.contains(ScopeConstants.ACTIVITIES_UPDATE)) {
                        LOGGER.info("The scope is invalid. Skipping");
                    } else {
                        publishToORCID(object, credentials);
                    }
                } 
            }
        }
    }

    /**
     * Publishes MCRObject in user's orcid profile.
     * Creates a new record if no corresponding publication can be found in the profile.
     * Otherwise, the publication in the profile will be updated.
     * 
     * @param object the object
     * @param credentials the credentials
     */
    abstract protected void publishToORCID(MCRObject object, MCRORCIDCredentials credentials);
}

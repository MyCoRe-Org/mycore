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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.user2.MCRUser;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDUserUtils;
import org.mycore.orcid2.util.MCRIdentifier;
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

    /**
     * Publishes MCRObject to all linked ORCID profiles.
     * MCRObject can be filtered with ORCIDMODSFilter transformer.
     * If the MODS is empty, the MCRObject is skipped.
     * 
     * @param object the MCRObject
     */
    private void handlePublication(MCRObject object) {
        final MCRObjectID objectID = object.getId();
        LOGGER.info("Start publishing {} to ORCID.", objectID);
        if (!MCRMODSWrapper.isSupported(object)) {
            return;
        }
        if (!MCRORCIDUtils.checkPublishState(object)) {
            LOGGER.info("Object has wrong state. Skipping {}.", objectID);
            return;
        }
        final Map<String, MCRORCIDCredential> credentials = listOrcidCredentials(object);
        try {
            publishObject(object, credentials);
            LOGGER.info("Finished publishing {} to ORCID.", objectID);
        } catch (Exception e) {
            LOGGER.warn("Error while publishing {} to ORCID:", objectID, e);
        }
    }

    private Map<String, MCRORCIDCredential> listOrcidCredentials(MCRObject object) {
        final Map<String, MCRORCIDCredential> orcidCredentials = new HashMap<>();
        for (Element nameElement : MCRORCIDUtils.listNameElements(new MCRMODSWrapper(object))) {
            MCRUser user = null;
            String orcid = null;
            for (MCRIdentifier id : listTrustedNameIdentifiers(nameElement)) {
                final List<MCRUser> tmp = MCRORCIDUserUtils.getUserByID(id).stream().toList();
                if (tmp.size() == 1) {
                    user = tmp.get(0);
                    if (Objects.equals(id.getType(), "orcid")) {
                        orcid = id.getValue();
                    }
                } else if (tmp.size() > 1) {
                    user = null;
                    LOGGER.info("Found more than one user for id: {}", id);
                    break;
                }
            }
            if (user != null) {
                MCRORCIDCredential credential = null;
                if (orcid != null) {
                    // save because of uniqueness check
                    credential = MCRORCIDUserUtils.getCredentialsByORCID(orcid);
                } else {
                    // try to fetch orcid user
                    final MCRORCIDUser orcidUser = new MCRORCIDUser(user);
                    final Map<String, MCRORCIDCredential> tmp = orcidUser.getCredentials();
                    if (tmp.size() == 1) {
                        credential = tmp.entrySet().iterator().next().getValue();
                    } else {
                        LOGGER.info("Found multiple ORCID credentials for user: {}", user.getUserID());
                        continue;
                    }
                }
                if (credential != null) {
                    final String scope = credential.getScope();
                    if (scope != null && !scope.contains(ScopeConstants.ACTIVITIES_UPDATE)) {
                        LOGGER.info("The scope is invalid. Skipping...");
                    } else {
                        orcidCredentials.put(orcid, credential);
                    }
                } 
            }
        }
        return orcidCredentials;
    }

    private List<MCRIdentifier> listTrustedNameIdentifiers(Element nameElement) {
        return MCRORCIDUtils.getNameIdentifiers(nameElement).stream()
            .filter(i -> MCRORCIDUser.TRUSTED_NAME_IDENTIFIER_TYPES.contains(i.getType())).toList();
    }

    /**
     * Publishes MCRObject in all orcid profiles specified by Map of MCRORCIDCredentials by ORCID iD.
     * Creates a new record if no corresponding publication can be found in the profile.
     * Otherwise, the publication in the profile will be updated.
     * 
     * @param object the MCRObject
     * @param credentials List of MCRORCIDCredential
     * @throws MCRORCIDException if Publish fails in general
     */
    abstract protected void publishObject(MCRObject object, Map<String, MCRORCIDCredential> credentials);
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.user2.MCRUser;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
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
public abstract class MCRORCIDWorkEventHandler<T> extends MCREventHandlerBase {

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
     * 
     * @param object the MCRObject
     * @throws MCRORCIDTransformationException if transformation to orcid model fails
     */
    private void handlePublication(MCRObject object) throws MCRORCIDTransformationException {
        if (!MCRMODSWrapper.isSupported(object)) {
            return;
        }
        final T work = transformContent(new MCRJDOMContent(object.createXML()));
        final Set<MCRIdentifier> identifiers = MCRORCIDUtils.getIdentifiers(new MCRMODSWrapper(object));
        final List<MCRORCIDCredentials> credentials = listOrcidCredentials(object);
        publishWork(object, work, identifiers, credentials);
    }

    private List<MCRORCIDCredentials> listOrcidCredentials(MCRObject object) {
        List<MCRORCIDCredentials> orcidCredentials = new ArrayList<MCRORCIDCredentials>();
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
                MCRORCIDCredentials credentials = null;
                if (orcid != null) {
                    credentials = MCRORCIDUserUtils.getCredentialsByORCID(orcid); // save because of uniqueness check
                } else { // try to fetch orcid user
                    final MCRORCIDUser orcidUser = new MCRORCIDUser(user);
                    final List<MCRORCIDCredentials> tmp = orcidUser.listCredentials();
                    if (tmp.size() == 1) {
                        credentials = tmp.get(0);
                    } else {
                        LOGGER.info("Found multiple ORCID credentials for user: {}", user.getUserID());
                        continue;
                    }
                }
                if (credentials != null) {
                    final String scope = credentials.getScope();
                    if (scope != null && !scope.contains(ScopeConstants.ACTIVITIES_UPDATE)) {
                        LOGGER.info("The scope is invalid. Skipping...");
                    } else {
                        orcidCredentials.add(credentials);
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
     * Transforms MCRContent to work.
     * 
     * @param content the MCRContent
     * @return the work
     * @throws MCRORCIDTransformationException if transformation fails
     */
    abstract protected T transformContent(MCRContent content) throws MCRORCIDTransformationException;

    /**
     * Publishes MCRObject in all orcid profiles specified by list of credentials.
     * Creates a new record if no corresponding publication can be found in the profile.
     * Otherwise, the publication in the profile will be updated.
     * 
     * @param object the MCRObject
     * @param work the work
     * @param identifiers List of MCRIdentifier
     * @param credentials list of credentials
     */
    abstract protected void publishWork(MCRObject object, T work, Set<MCRIdentifier> identifiers,
        List<MCRORCIDCredentials> credentials);
}

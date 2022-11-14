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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * When a publication is created or updated locally in this application,
 * collects all name identifiers from the MODS metadata,
 * looks up login users that have one of these identifiers, e.g. ORCID iD,
 * stored in their user attributes,
 * checks if these users have an ORCID profile we know of
 * and have authorized us to update their profile as trusted party,
 * and then creates/updates the publication in the works section of that profile.
 */
public abstract class MCRORCIDWorkEventHandler extends MCREventHandlerBase {

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
        final Set<String> nameIdentifierKeys = MCRORCIDUtils.getNameIdentifierKeys(new MCRMODSWrapper(object));
        getUsersForGivenNameIdentifiers(nameIdentifierKeys).stream().map(user -> new MCRORCIDUser(user))
            .forEach(user -> publishToORCID(object, user));
    }

    private Set<MCRUser> getUsersForGivenNameIdentifiers(Set<String> nameIdentifierKeys) {
        final Set<MCRUser> users = new HashSet<>();
        for (String key : nameIdentifierKeys) {
            final String name = MCRORCIDUser.ATTR_ID_PREFIX + key.split(":")[0];
            final String value = key.split(":")[1];
            users.addAll(MCRUserManager.getUsers(name, value).collect(Collectors.toSet()));
        }
        return users;
    }

    /**
     * Publishes MCRObject in user's orcid profile.
     * Requires orcid credentals.
     * 
     * @param object the object
     * @param user the user
     */
    abstract protected void publishToORCID(MCRObject object, MCRORCIDUser user);
}

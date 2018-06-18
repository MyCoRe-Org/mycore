package org.mycore.orcid.works;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid.user.MCRORCIDPublicationStatus;
import org.mycore.orcid.user.MCRORCIDUser;
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
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRWorkEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = LogManager.getLogger(MCRWorkEventHandler.class);

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

        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        MCRObjectID oid = object.getId();

        Set<String> nameIdentifierKeys = MCRORCIDUser.getNameIdentifierKeys(wrapper);
        Set<MCRUser> users = getUsersForGivenNameIdentifiers(nameIdentifierKeys);

        users.stream()
            .map(user -> new MCRORCIDUser(user))
            .filter(user -> user.hasORCIDProfile())
            .filter(user -> user.weAreTrustedParty())
            .forEach(user -> publishToORCID(oid, user));
    }

    private void publishToORCID(MCRObjectID oid, MCRORCIDUser user) {
        try {
            MCRWorksSection works = user.getORCIDProfile().getWorksSection();
            MCRORCIDPublicationStatus status = user.getPublicationStatus(oid.toString());

            if (status == MCRORCIDPublicationStatus.IN_MY_ORCID_PROFILE) {
                works.findWork(oid).get().update();
            }
            if (status == MCRORCIDPublicationStatus.NOT_IN_MY_ORCID_PROFILE) {
                works.addWorkFrom(oid);
            }

        } catch (Exception ex) {
            LOGGER.warn("Could not publish {} in ORCID profile {} of user {}", oid,
                user.getORCIDProfile().getORCID(), user.getUser().getUserName(), ex);
        }
    }

    private Set<MCRUser> getUsersForGivenNameIdentifiers(Set<String> nameIdentifierKeys) {
        Set<MCRUser> users = new HashSet<MCRUser>();
        for (String key : nameIdentifierKeys) {
            String name = MCRORCIDUser.ATTR_ID_PREFIX + key.split(":")[0];
            String value = key.split(":")[1];

            // Workaround, because MCRUserManager.getUsers(name,value) returns users with incomplete attributes
            MCRUserManager.listUsers(null, null, null).stream()
                .filter(u -> value.equals(u.getUserAttribute(name))).forEach(u -> users.add(u));
        }
        return users;
    }
}

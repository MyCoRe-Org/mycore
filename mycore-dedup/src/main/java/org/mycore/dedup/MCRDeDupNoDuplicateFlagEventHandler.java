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

package org.mycore.dedup;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;

/**
 * Turns the service flags that name confirmed non duplicates into permanent no-duplicate markings.
 * <p>
 * An application may let the user confirm that an object which is about to be created is no duplicate of
 * the objects presented to them. At that point the new object usually has no id yet, so the confirmation
 * cannot be stored as a marking. Instead it is recorded as one service flag per confirmed object id, using
 * the flag type configured by {@value #FLAG_TYPE_PROPERTY}.
 * <p>
 * This handler evaluates those flags: for every flag a no-duplicate marking between the stored object and
 * the flagged object is created and the flag itself is removed, so that it never reaches the metadata
 * store. It therefore has to be registered before the event handler that writes the object metadata
 * (see {@code MCR.EventHandler.MCRObject.020.Class}).
 */
public class MCRDeDupNoDuplicateFlagEventHandler extends MCREventHandlerBase {

    /** Configuration key holding the type of the service flags that name the confirmed non duplicates. */
    public static final String FLAG_TYPE_PROPERTY = "MCR.DeDup.NoDuplicateFlagType";

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void handleObjectCreated(MCREvent event, MCRObject object) {
        convertFlags(object);
    }

    @Override
    protected void handleObjectUpdated(MCREvent event, MCRObject object) {
        convertFlags(object);
    }

    private void convertFlags(MCRObject object) {
        String flagType = MCRConfiguration2.getStringOrThrow(FLAG_TYPE_PROPERTY);
        MCRObjectService service = object.getService();
        List<String> flags = service.getFlags(flagType);
        if (flags.isEmpty()) {
            return;
        }
        service.removeFlags(flagType);
        String creator = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        MCRDeDupKeyManager keyManager = MCRDeDupKeyManager.obtainInstance();
        flags.stream().distinct().forEach(flag -> markAsNoDuplicate(keyManager, object.getId(), flag, creator));
    }

    private void markAsNoDuplicate(MCRDeDupKeyManager keyManager, MCRObjectID objectId, String flag, String creator) {
        if (!MCRObjectID.isValid(flag)) {
            LOGGER.warn("Ignoring no-duplicate flag of object {} with invalid object id {}", objectId, flag);
            return;
        }
        MCRObjectID other = MCRObjectID.getInstance(flag);
        if (other.equals(objectId)) {
            LOGGER.warn("Ignoring no-duplicate flag of object {} referencing the object itself", objectId);
            return;
        }
        LOGGER.info("Marking {} and {} as no duplicates", objectId, other);
        keyManager.addNoDuplicate(objectId, other, creator);
    }
}

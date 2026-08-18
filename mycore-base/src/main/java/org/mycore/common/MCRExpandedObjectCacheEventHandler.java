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

package org.mycore.common;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.common.MCRLinkType;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Event handler that clears the {@link MCRExpandedObjectCache} for affected objects
 * when certain events occur (delete, update, repair, link update, ancestor update, derivate link update).
 * This ensures that the cache does not contain stale expanded object data.
 */
public class MCRExpandedObjectCacheEventHandler extends MCREventHandlerBase {

    /**
     * Clears the cache for the deleted object.
     *
     * @param evt the delete event
     * @param obj the deleted object
     */
    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        MCRExpandedObjectCache.getInstance().clear(obj.getId());
    }

    /**
     * Clears the cache for the object that was linked to.
     * When a link changes, the expanded representation of the linked object might change.
     *
     * @param evt      the link update event
     * @param obj      the object where the link was updated
     * @param relation the type of the link
     * @param linkedID the ID of the object that was linked to
     */
    @Override
    protected void handleObjectLinkUpdated(MCREvent evt, MCRObject obj, MCRLinkType relation, MCRObjectID linkedID) {
        MCRExpandedObjectCache.getInstance().clear(linkedID);
    }

    /**
     * Clears the cache for the updated object.
     *
     * @param evt the update event
     * @param obj the updated object
     */
    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        MCRExpandedObjectCache.getInstance().clear(obj.getId());
    }

    /**
     * Clears the cache for the repaired object.
     * The metadata of the object may have been changed without an update event, e.g. by an import.
     *
     * @param evt the repair event
     * @param obj the repaired object
     */
    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        MCRExpandedObjectCache.getInstance().clear(obj.getId());
    }

    /**
     * Clears the cache for the object whose ancestor was updated.
     * Changes in ancestors can affect the expanded representation (e.g., inherited metadata).
     *
     * @param evt the ancestor update event
     * @param obj the object whose ancestor was updated
     */
    @Override
    protected void handleAncestorUpdated(MCREvent evt, MCRObject obj) {
        MCRExpandedObjectCache.getInstance().clear(obj.getId());
    }

    /**
     * Clears the cache for the object linked by the derivate.
     * When a derivate link changes, the expanded representation of the linked object might change.
     *
     * @param evt             the derivate link update event
     * @param updatedDerivate the derivate where the link was updated
     * @param linkedID        the ID of the object linked by the derivate
     */
    @Override
    protected void handleDerivateLinkUpdated(MCREvent evt, MCRDerivate updatedDerivate, MCRObjectID linkedID) {
        MCRExpandedObjectCache.getInstance().clear(linkedID);
    }
}

package org.mycore.common;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.common.MCRLinkType;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Event handler that clears the {@link MCRExpandedObjectCache} for affected objects
 * when certain events occur (delete, update, link update, ancestor update, derivate link update).
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

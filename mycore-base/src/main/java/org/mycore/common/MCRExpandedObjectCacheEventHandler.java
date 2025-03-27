package org.mycore.common;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.common.MCRLinkType;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRExpandedObjectCacheEventHandler extends MCREventHandlerBase {

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        MCRExpandedObjectCache.getInstance().clear(obj.getId());
    }

    @Override
    protected void handleObjectLinkUpdated(MCREvent evt, MCRObject obj, MCRLinkType relation, MCRObjectID linkedID) {
        MCRExpandedObjectCache.getInstance().clear(linkedID);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        MCRExpandedObjectCache.getInstance().clear(obj.getId());
    }

    @Override
    protected void handleAncestorUpdated(MCREvent evt, MCRObject obj) {
        MCRExpandedObjectCache.getInstance().clear(obj.getId());
    }

    @Override
    protected void handleDerivateLinkUpdated(MCREvent evt, MCRDerivate updatedDerivate, MCRObjectID linkedID) {
        MCRExpandedObjectCache.getInstance().clear(linkedID);
    }
}

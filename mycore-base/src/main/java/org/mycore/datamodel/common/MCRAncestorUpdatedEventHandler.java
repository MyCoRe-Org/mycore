package org.mycore.datamodel.common;

import java.util.Collection;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This EventHandler is used to bubble up the ANCESTOR_UPDATED event to all descendants of the object. Together with
 * the {@link org.mycore.common.MCRExpandedObjectManager} it replaces the old MCRMetadataShareAgent functions.
 */
public class MCRAncestorUpdatedEventHandler extends MCREventHandlerBase {

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleAncestorUpdated(evt, obj);
    }

    @Override
    protected void handleAncestorUpdated(MCREvent evt, MCRObject obj) {
        Collection<String> sourceOf = MCRLinkTableManager.getInstance().getSourceOf(obj.getId(), MCRLinkType.PARENT);

        sourceOf.forEach(source -> {
            MCRObjectID childID = MCRObjectID.getInstance(source);
            MCRObject object = MCRMetadataManager.retrieveMCRObject(childID);
            triggerAncestorUpdatedEvent(object);
        });
    }

    private void triggerAncestorUpdatedEvent(MCRObject object) {
        MCREvent event = new MCREvent(MCREvent.ObjectType.OBJECT, MCREvent.EventType.ANCESTOR_UPDATED);
        event.put(MCREvent.OBJECT_KEY, object);
        MCREventManager.getInstance().handleEvent(event);
    }


}

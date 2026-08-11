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

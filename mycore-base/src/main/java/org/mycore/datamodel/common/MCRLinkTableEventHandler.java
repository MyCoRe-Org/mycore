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

import static org.mycore.common.events.MCREvent.OBJECT_OLD_KEY;
import static org.mycore.common.events.MCREvent.RELATED_OBJECT_KEY;
import static org.mycore.datamodel.common.MCRLinkTableManager.MCRLinkReference;
import static org.mycore.datamodel.common.MCRLinkTableManager.getInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This class manages all operations of the LinkTables for operations of an object.
 * 
 * @author Jens Kupferschmidt
 */
public class MCRLinkTableEventHandler extends MCREventHandlerBase {

    /**
     * This method add the data to the link and classification table via MCRLinkTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectCreated(MCREvent evt, MCRObject obj) {
        getInstance().create(obj);
    }

    /**
     * This method update the data to the link and classification table via MCRLinkTableManager.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        handleObjectRepaired(evt, obj);
    }


    private static void triggerLinkedObjectChanged(MCREvent evt, MCRObject obj) {
        List<MCREvent> events = new ArrayList<>();
        // First handle all objects which have a pointer to this object, since the pointers did not change
        // TODO: This is a workaround to also get types. It would be better to have a method in MCRLinkTableManager,
        //  which solves this with one query.
        Arrays.stream(MCRLinkType.values()).forEach(type -> {
            Collection<String> sourceOf = getInstance().getSourceOf(obj.getId(), type);
            sourceOf.forEach(source -> {
                if (MCRObjectID.isValid(source)) {
                    // the events are always thrown in the perspective of the updated object, to archive this, the
                    // opposite type is used
                    MCRLinkType oppositeType = type.getOppositeType();
                    events.add(createLinkedObjectChangedEvent(obj, oppositeType, MCRObjectID.getInstance(source)));
                }
            });
        });

        // now handle the object itself
        // we need to find all the old links from this object and new links to other objects
        MCRObject oldObject = evt.get(OBJECT_OLD_KEY, MCRObject.class);
        Collection<MCRLinkReference> oldLinks =
            oldObject == null ? List.of() : getInstance().getLinks(oldObject);
        Collection<MCRLinkReference> newLinks = getInstance().getLinks(obj);

        Collection<MCRLinkReference> combinedLinks = new HashSet<>(oldLinks);
        combinedLinks.addAll(newLinks);

        combinedLinks.forEach(link -> {
            events.add(createLinkedObjectChangedEvent(obj, link.type(), link.to()));
        });

        events.forEach(MCREventManager.getInstance()::handleEvent);
    }

    private static MCREvent createLinkedObjectChangedEvent(MCRBase obj, MCRLinkType type, MCRObjectID linkedObject) {
        boolean isDerivate = Objects.equals(obj.getId().getTypeId(), MCRDerivate.OBJECT_TYPE);
        MCREvent.ObjectType eventObjectType = isDerivate ? MCREvent.ObjectType.DERIVATE : MCREvent.ObjectType.OBJECT;

        MCREvent event = new MCREvent(eventObjectType, MCREvent.EventType.LINKED_UPDATED);
        event.put(isDerivate ? MCREvent.DERIVATE_KEY : MCREvent.OBJECT_KEY, obj);
        event.put(RELATED_OBJECT_KEY, linkedObject);
        event.put(MCREvent.LINK_TYPE_KEY, type);

        return event;
    }

    /**
     * This method delete the data from the link and classification table via MCRLinkTableManager.
     *
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        getInstance().delete(obj.getId());
        triggerLinkedObjectChanged(evt, obj);

    }

    /**
     * This method repair the data from the link and classification table via MCRLinkTableManager.
     *
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected final void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        getInstance().update(obj);

        triggerLinkedObjectChanged(evt, obj);
    }

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        getInstance().create(der);
        MCREventManager.getInstance()
            .handleEvent(createLinkedObjectChangedEvent(der, MCRLinkType.DERIVATE, der.getOwnerID()));
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
        getInstance().update(der);
        MCREventManager.getInstance()
            .handleEvent(createLinkedObjectChangedEvent(der, MCRLinkType.DERIVATE, der.getOwnerID()));
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        handleDerivateRepaired(evt, der);
        MCREventManager.getInstance()
            .handleEvent(createLinkedObjectChangedEvent(der, MCRLinkType.DERIVATE, der.getOwnerID()));
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        getInstance().delete(der.getId());
        MCREventManager.getInstance()
            .handleEvent(createLinkedObjectChangedEvent(der, MCRLinkType.DERIVATE, der.getOwnerID()));
    }
}

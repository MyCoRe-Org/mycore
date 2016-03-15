/**
 * 
 */
package org.mycore.datamodel.common;

import java.util.Date;

import org.apache.log4j.Logger;
import org.mycore.backend.jpa.deleteditems.MCRDeletedItemManager;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * @author shermann
 */
public class MCRDeleteObjectEventHandler extends MCREventHandlerBase {
    private static Logger LOGGER = Logger.getLogger(MCRDeleteObjectEventHandler.class);

    /**
     * This method handle all calls for EventHandler for the event types
     * MCRObject, MCRDerivate and MCRFile.
     * 
     * @param evt
     *            The MCREvent object
     */
    @Override
    public void doHandleEvent(MCREvent evt) {
        /* objects */
        if (evt.getObjectType().equals(MCREvent.OBJECT_TYPE)) {
            MCRObject obj = (MCRObject) (evt.get("object"));
            if (obj != null) {
                LOGGER.debug(getClass().getName() + " handling " + obj.getId().toString() + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    handleObjectCreated(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    handleObjectUpdated(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    handleObjectDeleted(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.REPAIR_EVENT)) {
                    handleObjectRepaired(evt, obj);
                } else {
                    LOGGER.warn("Can't find method for an object data handler for event type " + evt.getEventType());
                }
                return;
            }
            LOGGER.warn("Can't find method for " + MCREvent.OBJECT_TYPE + " for event type " + evt.getEventType());
            return;
        }

        /* derivates */
        if (evt.getObjectType().equals(MCREvent.DERIVATE_TYPE)) {
            MCRDerivate der = (MCRDerivate) (evt.get("derivate"));
            if (der != null) {
                LOGGER.debug(getClass().getName() + " handling " + der.getId().toString() + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    handleDerivateCreated(evt, der);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    handleDerivateUpdated(evt, der);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    handleDerivateDeleted(evt, der);
                } else if (evt.getEventType().equals(MCREvent.REPAIR_EVENT)) {
                    handleDerivateRepaired(evt, der);
                } else {
                    LOGGER.warn("Can't find method for a derivate data handler for event type " + evt.getEventType());
                }
                return;
            }
            LOGGER.warn("Can't find method for " + MCREvent.DERIVATE_TYPE + " for event type " + evt.getEventType());
            return;
        }
    }

    /**
     * Stores the id, timestamp,current userid and the ip address in the
     * database table MCRDELETEDITEMS
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        try {
            LOGGER.info("Saving deletion information for object " + obj.getId());
            MCRDeletedItemManager.addEntry(obj.getId().toString(), new Date());
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }

    /**
     * Stores the id, timestamp,current userid and the ip address in the
     * database table MCRDELETEDITEMS
     * 
     * @param evt
     *            the event that occured
     * @param derivate
     *            the MCRDerivate that caused the event
     */
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate derivate) {
        try {
            LOGGER.info("Saving deletion information for derivate " + derivate.getId());
            MCRDeletedItemManager.addEntry(derivate.getId().toString(), new Date());
        } catch (Exception ex) {
            LOGGER.error(ex);
        }
    }
}

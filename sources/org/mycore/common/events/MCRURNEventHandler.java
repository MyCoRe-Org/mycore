/**
 * 
 */
package org.mycore.common.events;

import org.apache.log4j.Logger;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.services.urn.MCRURNManager;

/**
 * This class is responsible for the urn after an object has been deleted in
 * the database
 * 
 * @author shermann
 */
public class MCRURNEventHandler extends MCREventHandlerBase {
    private Logger logger = Logger.getLogger(MCRURNEventHandler.class);

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
                logger.debug(getClass().getName() + " handling " + obj.getId().getId() + " "
                        + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    handleObjectCreated(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    handleObjectUpdated(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    handleObjectDeleted(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.REPAIR_EVENT)) {
                    handleObjectRepaired(evt, obj);
                } else {
                    logger.warn("Can't find method for an object data handler for event type "
                            + evt.getEventType());
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.OBJECT_TYPE + " for event type "
                    + evt.getEventType());
            return;
        }

        /* derivates */
        if (evt.getObjectType().equals(MCREvent.DERIVATE_TYPE)) {
            MCRDerivate der = (MCRDerivate) (evt.get("derivate"));
            if (der != null) {
                logger.debug(getClass().getName() + " handling " + der.getId().getId() + " "
                        + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    handleDerivateCreated(evt, der);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    handleDerivateUpdated(evt, der);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    handleDerivateDeleted(evt, der);
                } else if (evt.getEventType().equals(MCREvent.REPAIR_EVENT)) {
                    handleDerivateRepaired(evt, der);
                } else {
                    logger.warn("Can't find method for a derivate data handler for event type "
                            + evt.getEventType());
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.DERIVATE_TYPE + " for event type "
                    + evt.getEventType());
            return;
        }
    }

    /**
     * Handles object deleted events. This implementation deletes the urn
     * records in the MCRURN table
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        try {
            MCRURNManager.removeURNByObjectID(obj.getId().toString());
            logger.info("Deleting urn from database for object belonging to "
                    + obj.getId().toString());
        } catch (Exception ex) {
            logger.error("Could not delete the urn from the database for object with id "
                    + obj.getId().toString(), ex);
        }
    }

    /**
     * Handles derivate deleted events. This implementation deletes the urn
     * records in the MCRURN table
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        try {
            MCRURNManager.removeURNByObjectID(der.getId().toString());
            logger.info("Deleting urn from database for derivates belonging to "
                    + der.getId().toString());
        } catch (Exception ex) {
            logger.error("Could not delete the urn from the database for object with id "
                    + der.getId().toString(), ex);
        }
    }
}
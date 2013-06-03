/**
 * 
 */
package org.mycore.datamodel.common;

import java.text.MessageFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRFileMetadata;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.urn.MCRURNManager;

/**
 * This class is responsible for the urn after an object has been deleted in the
 * database
 * 
 * @author shermann
 */
public class MCRURNEventHandler extends MCREventHandlerBase {

    private static final Logger LOGGER = Logger.getLogger(MCRURNEventHandler.class);

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
            if (MCRURNManager.hasURNAssigned(obj.getId().toString())) {
                LOGGER.info("Deleting urn from database for object belonging to " + obj.getId().toString());
                MCRURNManager.removeURNByObjectID(obj.getId().toString());
            }

        } catch (Exception ex) {
            LOGGER.error("Could not delete the urn from the database for object with id " + obj.getId().toString(), ex);
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
            LOGGER.info("Deleting urn from database for derivates belonging to " + der.getId().toString());
        } catch (Exception ex) {
            LOGGER.error("Could not delete the urn from the database for object with id " + der.getId().toString(), ex);
        }
    }

    /**
     * Handles derivate created events. This implementation adds the urn records in the MCRURN table
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        MCRObjectID derivateID = der.getId();
        MCRObjectDerivate objectDerivate = der.getDerivate();
        String urn = objectDerivate.getURN();
        if (urn == null || !MCRURNManager.isValid(urn)) {
            return;
        }
        MCRURNManager.assignURN(urn, derivateID.toString());
        List<MCRFileMetadata> fileMetadata = objectDerivate.getFileMetadata();
        for (MCRFileMetadata metadata : fileMetadata) {
            String fileURN = metadata.getUrn();
            if (urn != null) {
                LOGGER.info(MessageFormat.format("load file urn : %s, %s, %s", fileURN, derivateID, metadata.getName())
                    .toString());
                MCRURNManager.assignURN(fileURN, derivateID.toString(), metadata.getName());
            }
        }
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        MCREvent indexEvent = new MCREvent(MCREvent.DERIVATE_TYPE, MCREvent.INDEX_EVENT);
        indexEvent.put("derivate", der);
        MCREventManager.instance().handleEvent(indexEvent);
    }
}

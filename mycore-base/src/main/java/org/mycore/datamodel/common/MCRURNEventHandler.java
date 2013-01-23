/**
 * 
 */
package org.mycore.datamodel.common;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.services.urn.MCRURNManager;

/**
 * This class is responsible for the urn after an object has been deleted in the
 * database
 * 
 * @author shermann
 */
public class MCRURNEventHandler extends MCREventHandlerBase {
    private static final Filter<Element> ELEMENT_FILTER = Filters.element();
    private static final XPathFactory XPATH_FACTORY = XPathFactory.instance();
    private static final Logger LOGGER = Logger.getLogger(MCRURNEventHandler.class);

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
        Document derivateXml = MCRXMLMetadataManager.instance().retrieveXML(der.getId());

        // get all filesets from Derivate
        XPathExpression<Element> filesetPath = XPATH_FACTORY.compile("./mycorederivate/derivate/fileset", ELEMENT_FILTER);

        // add the urn of the fileset
        Element result = filesetPath.evaluateFirst(derivateXml);
        if (result == null) {
            return;
        }
        String urn = result.getAttributeValue("urn");

        if (urn != null && MCRURNManager.isValid(urn)) {
            MCRURNManager.assignURN(urn, der.getId().toString());
            LOGGER.info("Loading fileset urn: " + urn + ", " + der.getId().toString());
            // get all files in the fileset
            XPathExpression<Element> filePath = XPATH_FACTORY.compile("./mycorederivate/derivate/fileset[@urn='" + urn + "']/file",
                ELEMENT_FILTER);
            List<Element> files = filePath.evaluate(derivateXml);

            for (Element fileResult : files) {
                // add the urn of each file
                String fileName, fileUrn, fileDirectory;

                if ((fileName = fileResult.getAttributeValue("name")) != null && (fileUrn = fileResult.getChildText("urn")) != null) {

                    File derivateFile = new File(fileName);

                    fileName = derivateFile.getName();
                    fileDirectory = derivateFile.getParent();
                    LOGGER.info(MessageFormat.format("load file urn : %s, %s, %s, %s ", fileUrn, der.getId().toString(), fileDirectory, fileName).toString());
                    MCRURNManager.assignURN(fileUrn, der.getId().toString(), fileDirectory, fileName);
                }
            }
        }
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        // TODO
    }
}

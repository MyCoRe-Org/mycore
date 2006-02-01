/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.events;

import org.apache.log4j.Logger;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Abstract helper class that can be subclassed to implement event handlers more
 * easily.
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 */
public abstract class MCREventHandlerBase implements MCREventHandler {
    private static Logger logger = Logger.getLogger(MCREventHandlerBase.class);

    /**
     * This method handle all calls for EventHandler for the event types
     * MCRObject, MCRDerivate and MCRFile.
     * 
     * @param evt
     *            The MCREvent object
     */
    public void doHandleEvent(MCREvent evt) {

        if (evt.getObjectType().equals(MCREvent.OBJECT_TYPE)) {
            MCRObject obj = (MCRObject) (evt.get("object"));
            if (obj != null) {
                logger.debug(getClass().getName() + " handling " + obj.getId().getId() + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    handleObjectCreated(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    handleObjectUpdated(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    handleObjectDeleted(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.REPAIR_EVENT)) {
                    handleObjectRepaired(evt, obj);
                } else {
                    logger.warn("Can't find method for an object data handler for event type " + evt.getEventType());
                }
                return;
            }
            MCRObjectID objid = (MCRObjectID) (evt.get("objectID"));
            if (objid != null) {
                logger.debug(getClass().getName() + " handling " + objid.getId() + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.RECEIVE_EVENT)) {
                    handleObjectReceived(evt, objid);
                } else if (evt.getEventType().equals(MCREvent.EXIST_EVENT)) {
                    handleObjectExist(evt, objid);
                } else {
                    logger.warn("Can't find method for an object ID data handler for event type " + evt.getEventType());
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.OBJECT_TYPE + " for event type " + evt.getEventType());
            return;
        }

        if (evt.getObjectType().equals(MCREvent.DERIVATE_TYPE)) {
            MCRDerivate der = (MCRDerivate) (evt.get("derivate"));
            if (der != null) {
                logger.debug(getClass().getName() + " handling " + der.getId().getId() + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    handleDerivateCreated(evt, der);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    handleDerivateUpdated(evt, der);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    handleDerivateDeleted(evt, der);
                } else if (evt.getEventType().equals(MCREvent.REPAIR_EVENT)) {
                    handleDerivateRepaired(evt, der);
                } else {
                    logger.warn("Can't find method for an derivate data handler for event type " + evt.getEventType());
                }
                return;
            }
            MCRObjectID objid = (MCRObjectID) (evt.get("objectID"));
            if (objid != null) {
                logger.debug(getClass().getName() + " handling " + objid.getId() + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.RECEIVE_EVENT)) {
                    handleDerivateReceived(evt, objid);
                } else if (evt.getEventType().equals(MCREvent.EXIST_EVENT)) {
                    handleDerivateExist(evt, objid);
                } else {
                    logger.warn("Can't find method for an derivate ID data handler for event type " + evt.getEventType());
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.DERIVATE_TYPE + " for event type " + evt.getEventType());
            return;
        }

        if (evt.getObjectType().equals(MCREvent.FILE_TYPE)) {
            MCRFile file = (MCRFile) (evt.get("file"));
            if (file != null) {
                logger.debug(getClass().getName() + " handling " + file.getOwnerID() + "/" + file.getAbsolutePath() + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    handleFileCreated(evt, file);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    handleFileUpdated(evt, file);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    handleFileDeleted(evt, file);
                } else {
                    logger.warn("Can't find method for file data handler for event type " + evt.getEventType());
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.OBJECT_TYPE + " for event type " + evt.getEventType());
            return;
        }

    }

    /**
     * This method roll back all calls for EventHandler for the event types
     * MCRObject, MCRDerivate and MCRFile.
     * 
     * @param evt
     *            The MCREvent object
     */
    public void undoHandleEvent(MCREvent evt) {

        if (evt.getObjectType().equals(MCREvent.OBJECT_TYPE)) {
            MCRObject obj = (MCRObject) (evt.get("object"));
            if (obj != null) {
                logger.debug(getClass().getName() + " handling " + obj.getId().getId() + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    undoObjectCreated(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    undoObjectUpdated(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    undoObjectDeleted(evt, obj);
                } else if (evt.getEventType().equals(MCREvent.REPAIR_EVENT)) {
                    undoObjectRepaired(evt, obj);
                } else {
                    logger.warn("Can't find method for an object data handler for event type " + evt.getEventType());
                }
                return;
            }
            MCRObjectID objid = (MCRObjectID) (evt.get("objectID"));
            if (objid != null) {
                logger.debug(getClass().getName() + " handling " + objid.getId() + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.RECEIVE_EVENT)) {
                    undoObjectReceived(evt, objid);
                } else if (evt.getEventType().equals(MCREvent.EXIST_EVENT)) {
                    undoObjectExist(evt, objid);
                } else {
                    logger.warn("Can't find method for an object ID data handler for event type " + evt.getEventType());
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.OBJECT_TYPE + " for event type " + evt.getEventType());
            return;
        }

        if (evt.getObjectType().equals(MCREvent.DERIVATE_TYPE)) {
            MCRDerivate der = (MCRDerivate) (evt.get("derivate"));
            if (der != null) {
                logger.debug(getClass().getName() + " handling " + der.getId().getId() + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    undoDerivateCreated(evt, der);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    undoDerivateUpdated(evt, der);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    undoDerivateDeleted(evt, der);
                } else if (evt.getEventType().equals(MCREvent.REPAIR_EVENT)) {
                    undoDerivateRepaired(evt, der);
                } else {
                    logger.warn("Can't find method for an derivate data handler for event type " + evt.getEventType());
                }
                return;
            }
            MCRObjectID objid = (MCRObjectID) (evt.get("objectID"));
            if (objid != null) {
                logger.debug(getClass().getName() + " handling " + objid.getId() + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.RECEIVE_EVENT)) {
                    undoDerivateReceived(evt, objid);
                } else if (evt.getEventType().equals(MCREvent.EXIST_EVENT)) {
                    undoDerivateExist(evt, objid);
                } else {
                    logger.warn("Can't find method for an derivate ID data handler for event type " + evt.getEventType());
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.DERIVATE_TYPE + " for event type " + evt.getEventType());
            return;
        }

        if (evt.getObjectType().equals(MCREvent.FILE_TYPE)) {
            MCRFile file = (MCRFile) (evt.get("file"));
            logger.debug(getClass().getName() + " handling " + file.getOwnerID() + "/" + file.getAbsolutePath() + " " + evt.getEventType());

            if (file != null) {
                logger.debug(getClass().getName() + " handling " + file.getOwnerID() + "/" + file.getAbsolutePath() + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    undoFileCreated(evt, file);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    undoFileUpdated(evt, file);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    undoFileDeleted(evt, file);
                } else {
                    logger.warn("Can't find method for file data handler for event type " + evt.getEventType());
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.FILE_TYPE + " for event type " + evt.getEventType());
            return;
        }
    }

    /** This method does nothing. It is very useful for debugging events. */
    public void doNothing(MCREvent evt) {
        logger.debug(getClass().getName() + " does nothing on " + evt.getEventType() + " " + evt.getObjectType());
    }

    /**
     * Handles object created events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        doNothing(evt);
    }

    /**
     * Handles object updated events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        doNothing(evt);
    }

    /**
     * Handles object deleted events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        doNothing(evt);
    }

    /**
     * Handles object repair events. This implementation does nothing and should
     * be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
        doNothing(evt);
    }

    /**
     * Handles object receive events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param objid
     *            the MCRObjectID that caused the event
     */
    protected void handleObjectReceived(MCREvent evt, MCRObjectID objid) {
        doNothing(evt);
    }

    /**
     * Handles object exist events. This implementation does nothing and should
     * be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param objid
     *            the MCRObjectID that caused the event
     */
    protected void handleObjectExist(MCREvent evt, MCRObjectID objid) {
        doNothing(evt);
    }

    /**
     * Handles derivate created events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        doNothing(evt);
    }

    /**
     * Handles derivate updated events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        doNothing(evt);
    }

    /**
     * Handles derivate deleted events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        doNothing(evt);
    }

    /**
     * Handles derivate repair events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
        doNothing(evt);
    }

    /**
     * Handles derivate receive events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleDerivateReceived(MCREvent evt, MCRObjectID objid) {
        doNothing(evt);
    }

    /**
     * Handles derivate exist events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void handleDerivateExist(MCREvent evt, MCRObjectID objid) {
        doNothing(evt);
    }

    /**
     * Handles file created events. This implementation does nothing and should
     * be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void handleFileCreated(MCREvent evt, MCRFile file) {
        doNothing(evt);
    }

    /**
     * Handles file updated events. This implementation does nothing and should
     * be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void handleFileUpdated(MCREvent evt, MCRFile file) {
        doNothing(evt);
    }

    /**
     * Handles file deleted events. This implementation does nothing and should
     * be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
        doNothing(evt);
    }

    /**
     * Handles undo of object created events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void undoObjectCreated(MCREvent evt, MCRObject obj) {
        doNothing(evt);
    }

    /**
     * Handles undo of object updated events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void undoObjectUpdated(MCREvent evt, MCRObject obj) {
        doNothing(evt);
    }

    /**
     * Handles undo of object deleted events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void undoObjectDeleted(MCREvent evt, MCRObject obj) {
        doNothing(evt);
    }

    /**
     * Handles undo of object repaired events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRObject that caused the event
     */
    protected void undoObjectRepaired(MCREvent evt, MCRObject obj) {
        doNothing(evt);
    }

    /**
     * Handles undo of object receive events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param objid
     *            the MCRObjectID that caused the event
     */
    protected void undoObjectReceived(MCREvent evt, MCRObjectID objid) {
        doNothing(evt);
    }

    /**
     * Handles undo of object exist events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param objid
     *            the MCRObjectID that caused the event
     */
    protected void undoObjectExist(MCREvent evt, MCRObjectID objid) {
        doNothing(evt);
    }

    /**
     * Handles undo of derivate created events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void undoDerivateCreated(MCREvent evt, MCRDerivate der) {
        doNothing(evt);
    }

    /**
     * Handles undo of derivate updated events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void undoDerivateUpdated(MCREvent evt, MCRDerivate der) {
        doNothing(evt);
    }

    /**
     * Handles undo of derivate deleted events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void undoDerivateDeleted(MCREvent evt, MCRDerivate der) {
        doNothing(evt);
    }

    /**
     * Handles undo of derivate repaired events. This implementation does
     * nothing and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param der
     *            the MCRDerivate that caused the event
     */
    protected void undoDerivateRepaired(MCREvent evt, MCRDerivate der) {
        doNothing(evt);
    }

    /**
     * Handles undo of derivate receive events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param objid
     *            the MCRObjectID that caused the event
     */
    protected void undoDerivateReceived(MCREvent evt, MCRObjectID objid) {
        doNothing(evt);
    }

    /**
     * Handles undo of derivate exist events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param objid
     *            the MCRObjectID that caused the event
     */
    protected void undoDerivateExist(MCREvent evt, MCRObjectID objid) {
        doNothing(evt);
    }

    /**
     * Handles undo of file created events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void undoFileCreated(MCREvent evt, MCRFile file) {
        doNothing(evt);
    }

    /**
     * Handles undo of file updated events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void undoFileUpdated(MCREvent evt, MCRFile file) {
        doNothing(evt);
    }

    /**
     * Handles undo of file deleted events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void undoFileDeleted(MCREvent evt, MCRFile file) {
        doNothing(evt);
    }
}

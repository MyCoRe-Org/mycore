/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.common.events;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Abstract helper class that can be subclassed to implement event handlers more
 * easily.
 * 
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 */
public abstract class MCREventHandlerBase implements MCREventHandler {
    private static Logger logger = LogManager.getLogger(MCREventHandlerBase.class);

    /**
     * This method handle all calls for EventHandler for the event types
     * MCRObject, MCRDerivate and MCRFile.
     * 
     * @param evt
     *            The MCREvent object
     */
    public void doHandleEvent(MCREvent evt) {

        if (evt.getObjectType().equals(MCREvent.OBJECT_TYPE)) {
            MCRObject obj = (MCRObject) evt.get("object");
            if (obj != null) {
                logger.debug("{} handling {} {}", getClass().getName(), obj.getId(), evt.getEventType());
                switch (evt.getEventType()) {
                    case MCREvent.CREATE_EVENT:
                        handleObjectCreated(evt, obj);
                        break;
                    case MCREvent.UPDATE_EVENT:
                        handleObjectUpdated(evt, obj);
                        break;
                    case MCREvent.DELETE_EVENT:
                        handleObjectDeleted(evt, obj);
                        break;
                    case MCREvent.REPAIR_EVENT:
                        handleObjectRepaired(evt, obj);
                        break;
                    case MCREvent.INDEX_EVENT:
                        handleObjectIndex(evt, obj);
                        break;
                    default:
                        logger
                            .warn("Can't find method for an object data handler for event type {}", evt.getEventType());
                        break;
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.OBJECT_TYPE + " for event type {}", evt.getEventType());
            return;
        }

        if (evt.getObjectType().equals(MCREvent.DERIVATE_TYPE)) {
            MCRDerivate der = (MCRDerivate) evt.get("derivate");
            if (der != null) {
                logger.debug("{} handling {} {}", getClass().getName(), der.getId(), evt.getEventType());
                switch (evt.getEventType()) {
                    case MCREvent.CREATE_EVENT:
                        handleDerivateCreated(evt, der);
                        break;
                    case MCREvent.UPDATE_EVENT:
                        handleDerivateUpdated(evt, der);
                        break;
                    case MCREvent.DELETE_EVENT:
                        handleDerivateDeleted(evt, der);
                        break;
                    case MCREvent.REPAIR_EVENT:
                        handleDerivateRepaired(evt, der);
                        break;
                    case MCREvent.INDEX_EVENT:
                        updateDerivateFileIndex(evt, der);
                        break;
                    default:
                        logger
                            .warn("Can't find method for a derivate data handler for event type {}",
                                evt.getEventType());
                        break;
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.DERIVATE_TYPE + " for event type {}", evt.getEventType());
            return;
        }

        if (evt.getObjectType().equals(MCREvent.PATH_TYPE)) {
            Path path = (Path) evt.get(MCREvent.PATH_KEY);
            if (path != null) {
                if (!path.isAbsolute()) {
                    logger.warn("Cannot handle path events on non absolute paths: {}", path);
                }
                logger.debug("{} handling {} {}", getClass().getName(), path, evt.getEventType());
                BasicFileAttributes attrs = (BasicFileAttributes) evt.get(MCREvent.FILEATTR_KEY);
                if (attrs == null && !evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    logger.warn("BasicFileAttributes for {} was not given. Resolving now.", path);
                    try {
                        attrs = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
                    } catch (IOException e) {
                        logger.error("Could not get BasicFileAttributes from path: {}", path, e);
                    }
                }
                switch (evt.getEventType()) {
                    case MCREvent.CREATE_EVENT:
                        handlePathCreated(evt, path, attrs);
                        break;
                    case MCREvent.UPDATE_EVENT:
                        handlePathUpdated(evt, path, attrs);
                        break;
                    case MCREvent.DELETE_EVENT:
                        handlePathDeleted(evt, path, attrs);
                        break;
                    case MCREvent.REPAIR_EVENT:
                        handlePathRepaired(evt, path, attrs);
                        break;
                    case MCREvent.INDEX_EVENT:
                        updatePathIndex(evt, path, attrs);
                        break;
                    default:
                        logger.warn("Can't find method for Path data handler for event type {}", evt.getEventType());
                        break;
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.PATH_TYPE + " for event type {}", evt.getEventType());
            return;
        }

        if (evt.getObjectType().equals(MCREvent.CLASS_TYPE)) {
            MCRCategory cl = (MCRCategory) evt.get("class");
            if (cl != null) {
                logger.debug("{} handling {} {}", getClass().getName(), cl.getId(), evt.getEventType());
                switch (evt.getEventType()) {
                    case MCREvent.CREATE_EVENT:
                        handleClassificationCreated(evt, cl);
                        break;
                    case MCREvent.UPDATE_EVENT:
                        handleClassificationUpdated(evt, cl);
                        break;
                    case MCREvent.DELETE_EVENT:
                        handleClassificationDeleted(evt, cl);
                        break;
                    case MCREvent.REPAIR_EVENT:
                        handleClassificationRepaired(evt, cl);
                        break;
                    default:
                        logger.warn("Can't find method for a classification data handler for event type {}",
                            evt.getEventType());
                        break;
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.CLASS_TYPE + " for event type {}", evt.getEventType());
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
            MCRObject obj = (MCRObject) evt.get("object");
            if (obj != null) {
                logger.debug("{} handling {} {}", getClass().getName(), obj.getId(), evt.getEventType());
                switch (evt.getEventType()) {
                    case MCREvent.CREATE_EVENT:
                        undoObjectCreated(evt, obj);
                        break;
                    case MCREvent.UPDATE_EVENT:
                        undoObjectUpdated(evt, obj);
                        break;
                    case MCREvent.DELETE_EVENT:
                        undoObjectDeleted(evt, obj);
                        break;
                    case MCREvent.REPAIR_EVENT:
                        undoObjectRepaired(evt, obj);
                        break;
                    default:
                        logger
                            .warn("Can't find method for an object data handler for event type {}", evt.getEventType());
                        break;
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.OBJECT_TYPE + " for event type {}", evt.getEventType());
            return;
        }

        if (evt.getObjectType().equals(MCREvent.DERIVATE_TYPE)) {
            MCRDerivate der = (MCRDerivate) evt.get("derivate");
            if (der != null) {
                logger.debug("{} handling {}{}", getClass().getName(), der.getId(), evt.getEventType());
                switch (evt.getEventType()) {
                    case MCREvent.CREATE_EVENT:
                        undoDerivateCreated(evt, der);
                        break;
                    case MCREvent.UPDATE_EVENT:
                        undoDerivateUpdated(evt, der);
                        break;
                    case MCREvent.DELETE_EVENT:
                        undoDerivateDeleted(evt, der);
                        break;
                    case MCREvent.REPAIR_EVENT:
                        undoDerivateRepaired(evt, der);
                        break;
                    default:
                        logger
                            .warn("Can't find method for a derivate data handler for event type {}",
                                evt.getEventType());
                        break;
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.DERIVATE_TYPE + " for event type {}", evt.getEventType());
            return;
        }

        if (evt.getObjectType().equals(MCREvent.PATH_TYPE)) {
            Path path = (Path) evt.get(MCREvent.PATH_KEY);
            if (path != null) {
                if (!path.isAbsolute()) {
                    logger.warn("Cannot handle path events on non absolute paths: {}", path);
                }
                logger.debug("{} handling {} {}", getClass().getName(), path, evt.getEventType());
                BasicFileAttributes attrs = (BasicFileAttributes) evt.get(MCREvent.FILEATTR_KEY);
                if (attrs == null && !evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    logger.warn("BasicFileAttributes for {} was not given. Resolving now.", path);
                    try {
                        attrs = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
                    } catch (IOException e) {
                        logger.error("Could not get BasicFileAttributes from path: {}", path, e);
                    }
                }
                switch (evt.getEventType()) {
                    case MCREvent.CREATE_EVENT:
                        undoPathCreated(evt, path, attrs);
                        break;
                    case MCREvent.UPDATE_EVENT:
                        undoPathUpdated(evt, path, attrs);
                        break;
                    case MCREvent.DELETE_EVENT:
                        undoPathDeleted(evt, path, attrs);
                        break;
                    case MCREvent.REPAIR_EVENT:
                        undoPathRepaired(evt, path, attrs);
                        break;
                    default:
                        logger.warn("Can't find method for Path data handler for event type {}", evt.getEventType());
                        break;
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.PATH_TYPE + " for event type {}", evt.getEventType());
            return;
        }

        if (evt.getObjectType().equals(MCREvent.CLASS_TYPE)) {
            MCRCategory obj = (MCRCategory) evt.get("class");
            if (obj != null) {
                logger.debug("{} handling {} {}", getClass().getName(), obj.getId(), evt.getEventType());
                switch (evt.getEventType()) {
                    case MCREvent.CREATE_EVENT:
                        undoClassificationCreated(evt, obj);
                        break;
                    case MCREvent.UPDATE_EVENT:
                        undoClassificationUpdated(evt, obj);
                        break;
                    case MCREvent.DELETE_EVENT:
                        undoClassificationDeleted(evt, obj);
                        break;
                    case MCREvent.REPAIR_EVENT:
                        undoClassificationRepaired(evt, obj);
                        break;
                    default:
                        logger.warn("Can't find method for an classification data handler for event type {}",
                            evt.getEventType());
                        break;
                }
                return;
            }
            logger.warn("Can't find method for " + MCREvent.CLASS_TYPE + " for event type {}", evt.getEventType());
        }

    }

    /** This method does nothing. It is very useful for debugging events. */
    public void doNothing(MCREvent evt, Object obj) {
        logger.debug("{} does nothing on {} {} {}", getClass().getName(), evt.getEventType(), evt.getObjectType(),
            obj.getClass().getName());
    }

    /**
     * Handles classification created events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void handleClassificationCreated(MCREvent evt, MCRCategory obj) {
        doNothing(evt, obj);
    }

    /**
     * Handles classification updated events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void handleClassificationUpdated(MCREvent evt, MCRCategory obj) {
        doNothing(evt, obj);
    }

    /**
     * Handles classification deleted events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void handleClassificationDeleted(MCREvent evt, MCRCategory obj) {
        doNothing(evt, obj);
    }

    /**
     * Handles classification repair events. This implementation does nothing and should
     * be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void handleClassificationRepaired(MCREvent evt, MCRCategory obj) {
        doNothing(evt, obj);
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
        doNothing(evt, obj);
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
        doNothing(evt, obj);
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
        doNothing(evt, obj);
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
        doNothing(evt, obj);
    }

    protected void handleObjectIndex(MCREvent evt, MCRObject obj) {
        doNothing(evt, obj);
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
        doNothing(evt, der);
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
        doNothing(evt, der);
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
        doNothing(evt, der);
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
        doNothing(evt, der);
    }

    protected void handlePathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        doNothing(evt, path);
    }

    protected void handlePathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
        doNothing(evt, path);
    }

    protected void handlePathRepaired(MCREvent evt, Path path, BasicFileAttributes attrs) {
        doNothing(evt, path);
    }

    protected void updatePathIndex(MCREvent evt, Path path, BasicFileAttributes attrs) {
        doNothing(evt, path);
    }

    protected void handlePathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        doNothing(evt, path);
    }

    /**
     * Handles undo of classification created events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void undoClassificationCreated(MCREvent evt, MCRCategory obj) {
        doNothing(evt, obj);
    }

    /**
     * Handles undo of classification updated events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void undoClassificationUpdated(MCREvent evt, MCRCategory obj) {
        doNothing(evt, obj);
    }

    /**
     * Handles undo of classification deleted events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void undoClassificationDeleted(MCREvent evt, MCRCategory obj) {
        doNothing(evt, obj);
    }

    /**
     * Handles undo of classification repaired events. This implementation does nothing
     * and should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param obj
     *            the MCRClassification that caused the event
     */
    protected void undoClassificationRepaired(MCREvent evt, MCRCategory obj) {
        doNothing(evt, obj);
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
        doNothing(evt, obj);
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
        doNothing(evt, obj);
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
        doNothing(evt, obj);
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
        doNothing(evt, obj);
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
        doNothing(evt, der);
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
        doNothing(evt, der);
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
        doNothing(evt, der);
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
        doNothing(evt, der);
    }

    protected void undoPathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        doNothing(evt, path);
    }

    protected void undoPathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        doNothing(evt, path);
    }

    protected void undoPathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
        doNothing(evt, path);
    }

    protected void undoPathRepaired(MCREvent evt, Path path, BasicFileAttributes attrs) {
        doNothing(evt, path);
    }

    /**
     * Updates the index content of the given file.
     */
    protected void updateDerivateFileIndex(MCREvent evt, MCRDerivate file) {
        doNothing(evt, file);
    }
}

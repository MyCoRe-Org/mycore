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

package org.mycore.datamodel.ifs;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.niofs.ifs1.MCRIFSFileSystem;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileEventHandlerBase extends MCREventHandlerBase {

    private static final String MCRFILE_EVENT_KEY = "file";

    private static Logger LOGGER = LogManager.getLogger(MCRFileEventHandlerBase.class);

    public static final String FILE_TYPE = "MCRFile";

    public static final String DIRECTORY_TYPE = "MCRDirectory";

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#doHandleEvent(org.mycore.common.events.MCREvent)
     */
    @Override
    public void doHandleEvent(MCREvent evt) {
        if (evt.getObjectType().equals(MCRFileEventHandlerBase.FILE_TYPE)) {
            MCRFile file = (MCRFile) evt.get(MCRFILE_EVENT_KEY);
            if (file != null) {
                LOGGER.debug("{} handling {}/{} {}", getClass().getName(), file.getOwnerID(), file.getAbsolutePath(),
                    evt.getEventType());
                switch (evt.getEventType()) {
                    case MCREvent.CREATE_EVENT:
                        handleFileCreated(evt, file);
                        break;
                    case MCREvent.UPDATE_EVENT:
                        handleFileUpdated(evt, file);
                        break;
                    case MCREvent.DELETE_EVENT:
                        handleFileDeleted(evt, file);
                        break;
                    case MCREvent.REPAIR_EVENT:
                        handleFileRepaired(evt, file);
                        break;
                    case MCREvent.INDEX_EVENT:
                        updateFileIndex(evt, file);
                        break;
                    default:
                        LOGGER.warn("Can't find method for file data handler for event type {}", evt.getEventType());
                        break;
                }
                return;
            }
            LOGGER.warn("Can't find method for " + MCRFileEventHandlerBase.FILE_TYPE + " for event type {}",
                evt.getEventType());
            return;
        }
        super.doHandleEvent(evt);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#undoHandleEvent(org.mycore.common.events.MCREvent)
     */
    @Override
    public void undoHandleEvent(MCREvent evt) {
        if (evt.getObjectType().equals(MCRFileEventHandlerBase.FILE_TYPE)) {
            MCRFile file = (MCRFile) evt.get(MCRFILE_EVENT_KEY);
            if (file != null) {
                LOGGER.debug("{} handling {}/{} {}", getClass().getName(), file.getOwnerID(), file.getAbsolutePath(),
                    evt.getEventType());
                switch (evt.getEventType()) {
                    case MCREvent.CREATE_EVENT:
                        undoFileCreated(evt, file);
                        break;
                    case MCREvent.UPDATE_EVENT:
                        undoFileUpdated(evt, file);
                        break;
                    case MCREvent.DELETE_EVENT:
                        undoFileDeleted(evt, file);
                        break;
                    case MCREvent.REPAIR_EVENT:
                        undoFileRepaired(evt, file);
                        break;
                    default:
                        LOGGER.warn("Can't find method for file data handler for event type {}", evt.getEventType());
                        break;
                }
                return;
            }
            LOGGER.warn("Can't find method for " + MCRFileEventHandlerBase.FILE_TYPE + " for event type {}",
                evt.getEventType());
            return;
        }

        super.undoHandleEvent(evt);
    }

    private void fireMCRFileEvent(MCREvent source, Path path, BasicFileAttributes attrs) {
        MCREvent target = toMCRFileEvent(source, path, attrs);
        if (target != null) {
            doHandleEvent(target);
        }
    }

    private MCREvent toMCRFileEvent(MCREvent source, Path path, BasicFileAttributes attrs) {
        if (!(path.getFileSystem() instanceof MCRIFSFileSystem)) {
            LOGGER.error("Cannot transform path from {} to MCRFile.", path.getFileSystem());
            return null;
        }
        if (attrs != null && !attrs.isDirectory()) {
            MCREvent target = new MCREvent(FILE_TYPE, source.getEventType());
            target.putAll(source);//includes probably "file";
            if (!target.contains(FILE_TYPE)) {
                if (target.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    LOGGER.warn("Could not restore MCRFile for Path event: {}", path);
                    return null;
                } else {
                    MCRFile file = MCRFile.getFile(attrs.fileKey().toString()); //fileKey is always internal id;
                    if (file == null) {
                        LOGGER.warn("Could not restore MCRFile with id {} for Path event: {}", attrs.fileKey(), path);
                        return null;
                    }
                    target.put(MCRFILE_EVENT_KEY, file);
                }
            }
            LOGGER.info("Transformed {} -> {}", source, target);
            return target;
        }
        return null;
    }

    private void fireUndoMCRFileEvent(MCREvent source, Path path, BasicFileAttributes attrs) {
        MCREvent target = toMCRFileEvent(source, path, attrs);
        if (target != null) {
            undoHandleEvent(target);
        }
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handlePathUpdated(org.mycore.common.events.MCREvent, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    protected void handlePathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        fireMCRFileEvent(evt, path, attrs);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handlePathDeleted(org.mycore.common.events.MCREvent, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    protected void handlePathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
        fireMCRFileEvent(evt, path, attrs);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handlePathRepaired(org.mycore.common.events.MCREvent, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    protected void handlePathRepaired(MCREvent evt, Path path, BasicFileAttributes attrs) {
        fireMCRFileEvent(evt, path, attrs);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#handlePathCreated(org.mycore.common.events.MCREvent, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    protected void handlePathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        fireMCRFileEvent(evt, path, attrs);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#undoPathCreated(org.mycore.common.events.MCREvent, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    protected void undoPathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        fireUndoMCRFileEvent(evt, path, attrs);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#undoPathUpdated(org.mycore.common.events.MCREvent, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    protected void undoPathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        fireUndoMCRFileEvent(evt, path, attrs);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#undoPathDeleted(org.mycore.common.events.MCREvent, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    protected void undoPathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
        fireUndoMCRFileEvent(evt, path, attrs);
    }

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#undoPathRepaired(org.mycore.common.events.MCREvent, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    protected void undoPathRepaired(MCREvent evt, Path path, BasicFileAttributes attrs) {
        fireUndoMCRFileEvent(evt, path, attrs);
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
        doNothing(evt, file);
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
        doNothing(evt, file);
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
        doNothing(evt, file);
    }

    /**
     * Handles file repair events. This implementation does nothing and should
     * be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void handleFileRepaired(MCREvent evt, MCRFile file) {
        doNothing(evt, file);
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
        doNothing(evt, file);
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
        doNothing(evt, file);
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
        doNothing(evt, file);
    }

    /**
     * Handles undo of file repair events. This implementation does nothing and
     * should be overwritted by subclasses.
     * 
     * @param evt
     *            the event that occured
     * @param file
     *            the MCRFile that caused the event
     */
    protected void undoFileRepaired(MCREvent evt, MCRFile file) {
        doNothing(evt, file);
    }

    /**
     * Updates the index content of the given file.
     */
    protected void updateFileIndex(MCREvent evt, MCRFile file) {
        doNothing(evt, file);
    }

}

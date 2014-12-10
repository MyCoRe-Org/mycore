/**
 * 
 */
package org.mycore.datamodel.ifs;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFileEventHandlerBase extends MCREventHandlerBase {

    private static final String MCRFILE_EVENT_KEY = "file";

    private static Logger LOGGER = Logger.getLogger(MCRFileEventHandlerBase.class);

    final static public String FILE_TYPE = "MCRFile";

    final static public String DIRECTORY_TYPE = "MCRDirectory";

    /* (non-Javadoc)
     * @see org.mycore.common.events.MCREventHandlerBase#doHandleEvent(org.mycore.common.events.MCREvent)
     */
    @Override
    public void doHandleEvent(MCREvent evt) {
        if (evt.getObjectType().equals(MCRFileEventHandlerBase.FILE_TYPE)) {
            MCRFile file = (MCRFile) evt.get(MCRFILE_EVENT_KEY);
            if (file != null) {
                LOGGER.debug(getClass().getName() + " handling " + file.getOwnerID() + "/" + file.getAbsolutePath()
                    + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    handleFileCreated(evt, file);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    handleFileUpdated(evt, file);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    handleFileDeleted(evt, file);
                } else if (evt.getEventType().equals(MCREvent.REPAIR_EVENT)) {
                    handleFileRepaired(evt, file);
                } else if (evt.getEventType().equals(MCREvent.INDEX_EVENT)) {
                    updateFileIndex(evt, file);
                } else {
                    LOGGER.warn("Can't find method for file data handler for event type " + evt.getEventType());
                }
                return;
            }
            LOGGER.warn("Can't find method for " + MCRFileEventHandlerBase.FILE_TYPE + " for event type "
                + evt.getEventType());
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
                LOGGER.debug(getClass().getName() + " handling " + file.getOwnerID() + "/" + file.getAbsolutePath()
                    + " " + evt.getEventType());
                if (evt.getEventType().equals(MCREvent.CREATE_EVENT)) {
                    undoFileCreated(evt, file);
                } else if (evt.getEventType().equals(MCREvent.UPDATE_EVENT)) {
                    undoFileUpdated(evt, file);
                } else if (evt.getEventType().equals(MCREvent.DELETE_EVENT)) {
                    undoFileDeleted(evt, file);
                } else if (evt.getEventType().equals(MCREvent.REPAIR_EVENT)) {
                    undoFileRepaired(evt, file);
                } else {
                    LOGGER.warn("Can't find method for file data handler for event type " + evt.getEventType());
                }
                return;
            }
            LOGGER.warn("Can't find method for " + MCRFileEventHandlerBase.FILE_TYPE + " for event type "
                + evt.getEventType());
            return;
        }

        super.undoHandleEvent(evt);
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
     * 
     * @param evt
     * @param file
     */
    protected void updateFileIndex(MCREvent evt, MCRFile file) {
        doNothing(evt, file);
    }

}

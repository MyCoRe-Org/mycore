package org.mycore.importer.event;

import java.util.EventObject;

/**
 * MCRImportMappingEvent is used to notify interested parties that 
 * the import status has changed.
 */
public class MCRImportStatusEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private String objectName;

    /**
     * Creates a new MCRImportMappingEvent object.
     * 
     * @param source the object responsible for the event
     * @param objectName a string to identify the event object
     */
    public MCRImportStatusEvent(Object source, String objectName) {
        super(source);
        this.objectName = objectName;
    }

    public String getObjectName() {
        return objectName;
    }
}
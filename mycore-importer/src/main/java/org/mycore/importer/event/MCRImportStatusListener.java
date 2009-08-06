package org.mycore.importer.event;

import java.util.EventListener;

/**
* Instances of classes that implement the <code>MCRImportStatusListener</code>
* interface can register to receive events when the import status changes.
*/
public interface MCRImportStatusListener extends EventListener {

    /**
     * Informs the listener that a record is completely mapped.
     * The listener can then invoke <code>MCRImportStatusEvent</code>
     * methods to obtain information about the event.
     * 
     * @param e event that describes the change
     */
    public void recordMapped(MCRImportStatusEvent e);

    /**
     * Informs the listener that a derivate is created and saved
     * to the file system. The listener can invoke
     * <code>MCRImportStatusEvent</code> methods to obtain
     * information about the event.
     * 
     * @param e event that describes the change
     */
    public void derivateSaved(MCRImportStatusEvent e);

    /**
     * Informs the listener that a mycore object is imported.
     * The listener can then invoke <code>MCRImportStatusEvent</code>
     * methods to obtain information about the event.
     * 
     * @param e event that describes the change
     */
    public void objectImported(MCRImportStatusEvent e);

    /**
     * Informs the listener that a derivate is imported.
     * The listener can then invoke <code>MCRImportStatusEvent</code>
     * methods to obtain information about the event.
     * 
     * @param e event that describes the change
     */
    public void derivateImported(MCRImportStatusEvent e);

}
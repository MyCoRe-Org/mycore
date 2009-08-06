package org.mycore.importer.event;

/**
 * An abstract adapter class for receiving mycore import events.
 * The methods in this class are empty. This class exists as
 * convenience for creating listener objects.
 * 
 * @author Matthias Eichner
 */
public class MCRImportStatusAdapter implements MCRImportStatusListener {

    public void derivateImported(MCRImportStatusEvent e) {}

    public void derivateSaved(MCRImportStatusEvent e) {}

    public void objectImported(MCRImportStatusEvent e) {}

    public void recordMapped(MCRImportStatusEvent e) {}

}
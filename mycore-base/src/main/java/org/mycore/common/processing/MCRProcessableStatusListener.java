package org.mycore.common.processing;

import java.util.EventListener;

/**
 * Base interface to listen to {@link MCRProcessableStatus} changes.
 * 
 * @author Matthias Eichner
 */
public interface MCRProcessableStatusListener extends EventListener {

    /**
     * Is fired when the status of the {@link MCRProcessable} has changed.
     * 
     * @param source the source {@link MCRProcessable}
     * @param oldStatus the old status
     * @param newStatus the new status
     */
    public void onStatusChange(MCRProcessable source, MCRProcessableStatus oldStatus, MCRProcessableStatus newStatus);

}

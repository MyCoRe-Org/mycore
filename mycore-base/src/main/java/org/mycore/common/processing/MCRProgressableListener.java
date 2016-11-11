package org.mycore.common.processing;

import java.util.EventListener;

/**
 * Base interface to listen to {@link MCRProgressable} changes.
 * 
 * @author Matthias Eichner
 */
public interface MCRProgressableListener extends EventListener {

    /**
     * Is fired when the progress of the {@link MCRProgressable} has changed.
     * 
     * @param source the source {@link MCRProgressable}
     * @param oldProgress the old progress
     * @param newProgress the new progress
     */
    public void onProgressChange(MCRProgressable source, Integer oldProgress, Integer newProgress);

    /**
     * Is fired when the progress text of the {@link MCRProgressable} has changed.
     * 
     * @param source the source {@link MCRProgressable}
     * @param oldProgressText the old progress text
     * @param newProgressText the new progress text
     */
    public void onProgressTextChange(MCRProgressable source, String oldProgressText, String newProgressText);

}

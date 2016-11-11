package org.mycore.common.processing;

/**
 * Same as a {@link MCRProgressable} but can listen to progress change events.
 * 
 * @author Matthias Eichner
 */
public interface MCRListenableProgressable extends MCRProgressable {

    /**
     * Adds a new {@link MCRProgressableListener} to this {@link MCRProgressable}.
     * 
     * @param listener the listener to add
     */
    public void addProgressListener(MCRProgressableListener listener);

    /**
     * Removes a {@link MCRProgressableListener} from this {@link MCRProgressable}.
     * 
     * @param listener the listener to remove
     */
    public void removeProgressListener(MCRProgressableListener listener);

}

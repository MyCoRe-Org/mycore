package org.mycore.common.processing;

import java.util.EventListener;

/**
 * Base event listener interface for adding/removing {@link MCRProcessable} of
 * {@link MCRProcessableCollection}.
 * 
 * @author Matthias Eichner
 */
public interface MCRProcessableCollectionListener extends EventListener {

    /**
     * Fired when a processable was added.
     * 
     * @param source the source collection
     * @param processable the processable added
     */
    void onAdd(MCRProcessableCollection source, MCRProcessable processable);

    /**
     * Fired when a processable was removed.
     * 
     * @param source the source collection
     * @param processable the processable removed
     */
    void onRemove(MCRProcessableCollection source, MCRProcessable processable);

    /**
     * Fired when a property changed.
     * 
     * @param source the source collection
     * @param name the name of the property
     * @param oldValue the old value
     * @param newValue the new value
     */
    void onPropertyChange(MCRProcessableCollection source, String name, Object oldValue, Object newValue);

}

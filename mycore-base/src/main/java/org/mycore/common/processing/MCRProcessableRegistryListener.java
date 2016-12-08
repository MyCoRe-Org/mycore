package org.mycore.common.processing;

/**
 * Base event listener interface for adding/removing {@link MCRProcessableCollection} of
 * an {@link MCRProcessableRegistry}.
 * 
 * @author Matthias Eichner
 */
public interface MCRProcessableRegistryListener {

    /**
     * Fired when a collection was added.
     * 
     * @param source the source registry
     * @param collection the collection added
     */
    public void onAdd(MCRProcessableRegistry source, MCRProcessableCollection collection);

    /**
     * Fired when a collection was removed.
     * 
     * @param source the source registry
     * @param collection the collection removed
     */
    public void onRemove(MCRProcessableRegistry source, MCRProcessableCollection collection);

}

package org.mycore.common.processing;

import java.util.stream.Stream;

/**
 * Registry for {@link MCRProcessable} and {@link MCRProcessableCollection}.
 * Can be used for managing and monitoring purposes.
 * 
 * @author Matthias Eichner
 */
public interface MCRProcessableRegistry {

    /**
     * Registers a new collection to the registry.
     * 
     * @param collection the collection to register
     */
    public void register(MCRProcessableCollection collection);

    /**
     * Removes a collection from the registry
     * 
     * @param collection the collection to remove
     */
    public void unregister(MCRProcessableCollection collection);

    /**
     * Streams all the collections of this registry.
     * 
     * @return stream of the registry content.
     */
    public Stream<MCRProcessableCollection> stream();

    /**
     * Adds a new listener.
     * 
     * @param listener the listener to add
     */
    public void addListener(MCRProcessableRegistryListener listener);

    /**
     * Removes a listener.
     * 
     * @param listener the listener to remove
     */
    public void removeListener(MCRProcessableRegistryListener listener);

}

package org.mycore.common.processing;

import java.util.stream.Stream;

/**
 * Defines a collection of coherent {@link MCRProcessable}.
 * 
 * @author Matthias Eichner
 */
public interface MCRProcessableCollection {

    /**
     * Returns a human readable name about this registry container.
     * 
     * @return name of this container
     */
    public String getName();

    /**
     * Adds a new {@link MCRProcessable} to this container.
     * 
     * @param processable the processable to add
     */
    public void add(MCRProcessable processable);

    /**
     * Removes a {@link MCRProcessable} from the container.
     */
    public void remove(MCRProcessable processable);

    /**
     * Streams all {@link MCRProcessable} registered by this container.
     * 
     * @return stream of {@link MCRProcessable}
     */
    public Stream<MCRProcessable> stream();

    /**
     * Adds a new listener.
     * 
     * @param listener the listener to add
     */
    public void addListener(MCRProcessableCollectionListener listener);

    /**
     * Removes a listener.
     * 
     * @param listener the listener to remove
     */
    public void removeListener(MCRProcessableCollectionListener listener);

}

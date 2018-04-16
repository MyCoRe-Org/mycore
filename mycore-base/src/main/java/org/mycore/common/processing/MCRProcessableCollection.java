package org.mycore.common.processing;

import java.util.Map;
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
    String getName();

    /**
     * Adds a new {@link MCRProcessable} to this container.
     * 
     * @param processable the processable to add
     */
    void add(MCRProcessable processable);

    /**
     * Removes a {@link MCRProcessable} from the container.
     */
    void remove(MCRProcessable processable);

    /**
     * Streams all {@link MCRProcessable} registered by this container.
     * 
     * @return stream of {@link MCRProcessable}
     */
    Stream<MCRProcessable> stream();

    /**
     * Checks if this collection contains any processable.
     *
     * @return true if this collection contains at least on processable
     */
    boolean isEmpty();

    /**
     * Returns a map of properties assigned to this processable.
     * 
     * @return the properties map
     */
    Map<String, Object> getProperties();

    /**
     * A shortcut for getProperties().get(name).
     * 
     * @param name the name of the property
     * @return the property value or null
     */
    default Object getProperty(String name) {
        return getProperties().get(name);
    }

    /**
     * Returns the property for the given name. The property
     * will be cast to the specified type. Be aware that a
     * ClassCastException is thrown if the type does not match.
     * 
     * @param name name of property
     * @param type object type of the property
     * @return the property value or null
     */
    @SuppressWarnings("unchecked")
    default <T> T getPropertyAs(String name, Class<T> type) {
        Object property = getProperty(name);
        if (property == null) {
            return null;
        }
        return (T) property;
    }

    /**
     * Adds a new listener.
     * 
     * @param listener the listener to add
     */
    void addListener(MCRProcessableCollectionListener listener);

    /**
     * Removes a listener.
     * 
     * @param listener the listener to remove
     */
    void removeListener(MCRProcessableCollectionListener listener);

}

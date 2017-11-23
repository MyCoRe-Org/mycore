/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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

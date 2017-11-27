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
    void register(MCRProcessableCollection collection);

    /**
     * Removes a collection from the registry
     * 
     * @param collection the collection to remove
     */
    void unregister(MCRProcessableCollection collection);

    /**
     * Streams all the collections of this registry.
     * 
     * @return stream of the registry content.
     */
    Stream<MCRProcessableCollection> stream();

    /**
     * Adds a new listener.
     * 
     * @param listener the listener to add
     */
    void addListener(MCRProcessableRegistryListener listener);

    /**
     * Removes a listener.
     * 
     * @param listener the listener to remove
     */
    void removeListener(MCRProcessableRegistryListener listener);

}

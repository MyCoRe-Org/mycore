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
    void onAdd(MCRProcessableRegistry source, MCRProcessableCollection collection);

    /**
     * Fired when a collection was removed.
     * 
     * @param source the source registry
     * @param collection the collection removed
     */
    void onRemove(MCRProcessableRegistry source, MCRProcessableCollection collection);

}

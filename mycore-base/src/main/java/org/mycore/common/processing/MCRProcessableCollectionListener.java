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

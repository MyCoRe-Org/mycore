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
    void addProgressListener(MCRProgressableListener listener);

    /**
     * Removes a {@link MCRProgressableListener} from this {@link MCRProgressable}.
     * 
     * @param listener the listener to remove
     */
    void removeProgressListener(MCRProgressableListener listener);

}

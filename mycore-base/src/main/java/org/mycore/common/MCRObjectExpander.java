/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.common;

import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Interface for expanding MCRObjects into MCRExpandedObjects. Should gather information from the database and other
 * resources to create a complete representation of the object, which may include duplicated metadata and structure.
 */
public interface MCRObjectExpander {

    /**
     * Expands the given MCRObject into an MCRExpandedObject. This method should gather all necessary information from
     * the database and other resources to create a complete representation of the object.
     *
     * @param mcrObject the normalized MCRObject to expand
     * @return the expanded MCRExpandedObject
     */
    MCRExpandedObject expand(MCRObject mcrObject);
    
}

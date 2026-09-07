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

package org.mycore.datamodel.metadata;

import org.mycore.access.MCRAccessException;

import java.util.List;

/**
 * Interface to define a strategy to store the order of child objects of a parent object.
 *
 */
public interface MCRChildrenOrderStrategy {

    /**
     * The strategy should return the order of the child objects of the parent object.
     * @param parentId the id of the parent object
     * @return the order of the child objects
     */
    List<MCRObjectID> getChildrenOrder(MCRObject parentId);

    /**
     * This method should set the order of the child objects of the parent object. The order is given as a list of
     * MCRObjectIDs.
     * @param parentId the id of the parent object
     * @param newOrder the new order of the child objects
     */
    void setChildrenOrder(MCRObject parentId, List<MCRObjectID> newOrder) throws MCRAccessException;

}

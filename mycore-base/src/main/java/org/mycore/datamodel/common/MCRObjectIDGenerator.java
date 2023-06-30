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
package org.mycore.datamodel.common;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * central generator for new MyCoRe Object IDs.
 *
 * @author Robert Stephan
 *
 */
public interface MCRObjectIDGenerator {

    /**
     * Returns a MCRObjectID from a given base ID string. A base ID is
     * <em>project_id</em>_<em>type_id</em>.
     * The number is computed by this method.
     *
     * @param baseId
     *            <em>project_id</em>_<em>type_id</em>
     */
    default MCRObjectID getNextFreeId(String baseId) {
        return getNextFreeId(baseId, 0);
    }

    /**
     * Returns a MCRObjectID from a given the components of a base ID string. A base ID is
     * <em>project_id</em>_<em>type_id</em>. The number is computed by this method.
     *
     * @param projectId
     *            The first component of <em>project_id</em>_<em>type_id</em>
     * @param type
     *            The second component of <em>project_id</em>_<em>type_id</em>
     */
    default MCRObjectID getNextFreeId(String projectId, String type) {
        return getNextFreeId(projectId + "_" + type);
    }

    /**
     * Returns a MCRObjectID from a given base ID string. Same as
     * {@link #getNextFreeId(String)} but the additional parameter acts as a
     * lower limit for integer part of the ID.
     *
     * @param baseId
     *            <em>project_id</em>_<em>type_id</em>
     * @param maxInWorkflow
     *            returned integer part of id will be at least
     *            <code>maxInWorkflow + 1</code>
     */
    MCRObjectID getNextFreeId(String baseId, int maxInWorkflow);

    /**
     * Returns the last ID used or reserved for the given object base type.
     *
     * @return a valid MCRObjectID, or null when there is no ID for the given
     *         type
     */
    MCRObjectID getLastID(String baseId);

}

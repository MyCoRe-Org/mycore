/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  MyCoRe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MyCoRe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.backend.jpa;

import org.mycore.datamodel.metadata.MCRObjectID;

import java.time.Instant;

public interface MCRObjectInfo {
    /**
     * @return the object id
     */
    MCRObjectID getId();

    /**
     * @return the project encoded in the object id
     */
    String getObjectProject();

    /**
     * @return the type encoded in the object id
     */
    String getObjectType();

    /**
     * @return the number encoded in the object id
     */
    int getObjectNumber();

    /**
     * @return the creation date of the object
     */
    Instant getCreateDate();

    /**
     * @return the last modify date of the object
     */
    Instant getModifyDate();

    /**
     * @return the user which last modified the object
     */
    String getModifiedBy();

    /**
     * @return returns the user which created the object.
     */
    String getCreatedBy();

    /**
     * @return returns the user which deleted the object.
     */
    String getDeletedBy();

    /**
     * @return the state classification category id e.G. state:submitted
     */
    String getState();

    /**
     * @return the date when the object was deleted.
     */
    Instant getDeleteDate();
}

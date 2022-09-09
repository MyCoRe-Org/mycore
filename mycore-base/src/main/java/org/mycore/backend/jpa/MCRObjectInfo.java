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

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import org.mycore.datamodel.metadata.MCRObjectID;

import java.time.Instant;

public interface MCRObjectInfo {
    @Id
    MCRObjectID getId();

    /**
     * @return the project encoded in the object id
     */
    @Column(name = "objectproject")
    String getObjectProject();

    /**
     * @return the type encoded in the object id
     */
    @Column(name = "objecttype")
    String getObjectType();

    /**
     * @return the number encoded in the object id
     */
    @Column(name = "objectnumber")
    int getObjectNumber();

    /**
     * @return the creation date of the object
     */
    @Column(name = "createdate")
    Instant getCreateDate();

    /**
     * @return the last modify date of the object
     */
    @Column(name = "modifydate")
    Instant getModifyDate();

    /**
     * @return the user which last modified the object
     */
    @Column(name = "modifiedby")
    String getModifiedBy();

    /**
     * @return returns the user which created the object.
     */
    @Column(name = "createdby")
    String getCreatedBy();

    /**
     * @return returns the user which deleted the object.
     */
    @Column(name = "deletedby")
    String getDeletedBy();

    /**
     * @return the state classification category id e.G. state:submitted
     */
    @Column(name = "state")
    String getState();

    /**
     * @return the date when the object was deleted.
     */
    @Column(name = "deletedate")
    Instant getDeleteDate();
}

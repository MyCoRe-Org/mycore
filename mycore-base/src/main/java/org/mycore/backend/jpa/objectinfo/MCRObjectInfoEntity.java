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

package org.mycore.backend.jpa.objectinfo;

import java.time.Instant;

import org.mycore.backend.jpa.MCRObjectIDPK;
import org.mycore.datamodel.common.MCRObjectInfo;
import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(MCRObjectIDPK.class)
@Table(name = "MCRObjectInfo")
public class MCRObjectInfoEntity implements MCRObjectInfo {

    private MCRObjectID id;

    private String state;

    private String createdBy;

    private String modifiedBy;

    private String deletedBy;

    private Instant createDate;

    private Instant modifyDate;

    private Instant deleteDate;

    @Override
    @Id
    public MCRObjectID getId() {
        return id;
    }

    public void setId(MCRObjectID id) {
        this.id = id;
    }

    @Override
    @Column(name = "objectproject")
    public String getObjectProject() {
        return this.id.getProjectId();
    }

    /**
     * This method does nothing and is only to satisfy JPA
     * @param objectProject ignored parameter
     */
    public void setObjectProject(String objectProject) {
        // read only value
    }

    @Override
    @Column(name = "objecttype")
    public String getObjectType() {
        return this.id.getTypeId();
    }

    /**
     * This method does nothing and is only to satisfy JPA
     * @param objectType ignored parameter
     */
    public void setObjectType(String objectType) {
        // read only value
    }

    @Column(name = "objectnumber")
    public int getObjectNumber() {
        return this.id.getNumberAsInteger();
    }

    /**
     * This method does nothing and is only to satisfy JPA
     * @param objectNumber ignored parameter
     */
    public void setObjectNumber(int objectNumber) {
        // read only value
    }

    @Override
    @Column(name = "createdate")
    public Instant getCreateDate() {
        return createDate;
    }

    /**
     * Updates the creation date of object in the Database
     * @param createDate the creation date
     */
    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    @Override
    @Column(name = "modifydate")
    public Instant getModifyDate() {
        return modifyDate;
    }

    /**
     * Updates the last modify date of object in the Database
     * @param modifyDate the last modify date
     */
    public void setModifyDate(Instant modifyDate) {
        this.modifyDate = modifyDate;
    }

    @Override
    @Column(name = "modifiedby")
    public String getModifiedBy() {
        return modifiedBy;
    }

    /**
     * Changes the user which last modified the object in the Database
     * @param modifiedBy the user
     */
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    @Column(name = "createdby")
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Changes the user which created the object in the Database
     * @param createdBy the user
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    @Column(name = "deletedby")
    public String getDeletedBy() {
        return deletedBy;
    }

    /**
     * Changes the user which deleted the object in the Database
     * @param deletedBy the user
     */
    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    @Override
    @Column(name = "state")
    public String getState() {
        return state;
    }

    /**
     * Changes the state of the object in the Database
     * @param state the state classification category id
     */
    public void setState(String state) {
        this.state = state;
    }

    @Override
    @Column(name = "deletedate")
    public Instant getDeleteDate() {
        return deleteDate;
    }

    /**
     * changes the date when the object was deleted.
     * @param deleteddate the date
     */
    public void setDeleteDate(Instant deleteddate) {
        this.deleteDate = deleteddate;
    }

    @Override
    public String toString() {
        return "MCRObjectInfoEntity{" +
            "id=" + id +
            ", state='" + state + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", modifiedBy='" + modifiedBy + '\'' +
            ", deletedBy='" + deletedBy + '\'' +
            ", createDate=" + createDate +
            ", modifyDate=" + modifyDate +
            ", deleteDate=" + deleteDate +
            '}';
    }
}

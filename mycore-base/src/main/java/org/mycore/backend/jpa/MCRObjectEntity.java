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

import java.time.Instant;

import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MCRObjectEntity {

    private String objectId;

    private String objectProject;

    private String objectType;

    private int objectNumber;

    private String state;

    private String createdBy;

    private String modifiedBy;

    private String deletedBy;

    private Instant createDate;

    private Instant modifyDate;

    private Instant deleteddate;

    public MCRObjectEntity() {
    }

    @Id
    @Column(name = "objectid", length = MCRObjectID.MAX_LENGTH, nullable = false)
    @Basic
    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    @Column(name = "objectproject")
    public String getObjectProject() {
        return objectProject;
    }

    public void setObjectProject(String objectProject) {
        this.objectProject = objectProject;
    }

    @Column(name = "objecttype")
    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    @Column(name = "objectnumber")
    public int getObjectNumber() {
        return objectNumber;
    }

    public void setObjectNumber(int objectNumber) {
        this.objectNumber = objectNumber;
    }

    @Column(name = "createdate")
    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    @Column(name = "modifydate")
    public Instant getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Instant modifyDate) {
        this.modifyDate = modifyDate;
    }

    @Column(name = "modifiedby")
    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Column(name = "createdby")

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Column(name = "deletedby")
    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    @Column(name = "state")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Column(name = "deletedate")
    public Instant getDeleteddate() {
        return deleteddate;
    }

    public void setDeleteddate(Instant deleteddate) {
        this.deleteddate = deleteddate;
    }
}

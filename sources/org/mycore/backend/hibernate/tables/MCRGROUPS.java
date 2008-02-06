/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.hibernate.tables;

import java.sql.Timestamp;

public class MCRGROUPS {
    private String gid;

    private String creator;

    private Timestamp creationDate;

    private Timestamp modifiedDate;

    private String description;

    /**
     * @hibernate.property column="GID" not-null="true" update="true"
     */
    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    /**
     * @hibernate.property column="CREATOR" not-null="true" update="true"
     */
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * @hibernate.property column="CREATIONDATE" not-null="true" update="true"
     */
    public Timestamp getCreationdate() {
        return creationDate;
    }

    public void setCreationdate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * @hibernate.property column="MODIFIEDDATE" not-null="true" update="true"
     */
    public Timestamp getModifieddate() {
        return modifiedDate;
    }

    public void setModifieddate(Timestamp modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    /**
     * @hibernate.property column="DESCRIPTION" not-null="true" update="true"
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

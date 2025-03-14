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

package org.mycore.datamodel.classifications2;

import java.io.Serial;
import java.io.Serializable;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @since 2.0
 */
@Embeddable
public class MCRCategLinkReference implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Basic
    private String objectID;

    @Basic
    @Column(name = "objectType", length = 128)
    private String type;

    public MCRCategLinkReference() {
    }

    public MCRCategLinkReference(MCRObjectID objectID) {
        this(objectID.toString(), objectID.getTypeId());
    }

    public MCRCategLinkReference(String objectID, String type) {
        setObjectID(objectID);
        setType(type);
    }

    public MCRCategLinkReference(MCRPath path) {
        this('/' + path.subpathComplete().toString(), path.getOwner());
    }

    public String getObjectID() {
        return objectID;
    }

    public void setObjectID(String objectID) {
        this.objectID = objectID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (objectID == null ? 0 : objectID.hashCode());
        result = prime * result + (type == null ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MCRCategLinkReference other = (MCRCategLinkReference) obj;
        if (objectID == null) {
            if (other.objectID != null) {
                return false;
            }
        } else if (!objectID.equals(other.objectID)) {
            return false;
        }
        if (type == null) {
            return other.type == null;
        } else {
            return type.equals(other.type);
        }
    }

    @Override
    public String toString() {
        return "MCRCategLinkReference [objectID=" + objectID + ", type=" + type + "]";
    }

}

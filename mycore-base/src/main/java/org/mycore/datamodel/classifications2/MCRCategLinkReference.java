package org.mycore.datamodel.classifications2;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 * @since 2.0
 */
@Embeddable
public class MCRCategLinkReference implements Serializable {

    private static final long serialVersionUID = -6457722746147666860L;

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
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (objectID == null ? 0 : objectID.hashCode());
        result = PRIME * result + (type == null ? 0 : type.hashCode());
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
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

}

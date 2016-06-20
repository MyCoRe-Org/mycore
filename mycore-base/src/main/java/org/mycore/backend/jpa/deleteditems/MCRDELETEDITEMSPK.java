/**
 * 
 */
package org.mycore.backend.jpa.deleteditems;

import java.io.Serializable;
import java.time.ZonedDateTime;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;

import org.mycore.backend.jpa.MCRZonedDateTimeConverter;

/**
 * @author shermann
 * @version $Revision: 13085 $ $Date: 2008-02-06 18:27:24 +0100 (Wed, 06 Feb 2008) $
 *
 */
@Embeddable
public class MCRDELETEDITEMSPK implements Serializable {

    private static final long serialVersionUID = -332440557096834050L;

    private String identifier;

    private ZonedDateTime dateDeleted;

    public MCRDELETEDITEMSPK() {

    }

    public MCRDELETEDITEMSPK(String identifier, ZonedDateTime dateDeleted) {
        setIdentifier(identifier);
        setDateDeleted(dateDeleted);
    }

    /**
     * @return the dateDeleted
     */
    @Basic
    @Column(name = "DATE_DELETED")
    @Convert(converter = MCRZonedDateTimeConverter.class)
    public ZonedDateTime getDateDeleted() {
        return dateDeleted;
    }

    /**
     * @param dateDeleted the dateDeleted to set
     */
    public void setDateDeleted(ZonedDateTime dateDeleted) {
        this.dateDeleted = dateDeleted;
    }

    /**
     * @return the identifier
     */
    @Basic
    @Column(length = 128, name = "IDENTIFIER")
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dateDeleted == null) ? 0 : dateDeleted.hashCode());
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
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
        MCRDELETEDITEMSPK other = (MCRDELETEDITEMSPK) obj;
        if (dateDeleted == null) {
            if (other.dateDeleted != null) {
                return false;
            }
        } else if (!dateDeleted.equals(other.dateDeleted)) {
            return false;
        }
        if (identifier == null) {
            if (other.identifier != null) {
                return false;
            }
        } else if (!identifier.equals(other.identifier)) {
            return false;
        }
        return true;
    }

}

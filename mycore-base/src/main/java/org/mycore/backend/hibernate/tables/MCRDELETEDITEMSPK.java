/**
 * 
 */
package org.mycore.backend.hibernate.tables;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author shermann
 * @version $Revision: 13085 $ $Date: 2008-02-06 18:27:24 +0100 (Wed, 06 Feb 2008) $
 *
 */
public class MCRDELETEDITEMSPK implements Serializable {

    private static final long serialVersionUID = -332440557096834050L;

    private String identifier;

    private Date dateDeleted;

    public MCRDELETEDITEMSPK(String identifier, Date dateDeleted) {
        this.identifier = identifier;
        this.dateDeleted = dateDeleted;
    }

    /**
     * @return the dateDeleted
     */
    public Date getDateDeleted() {
        return dateDeleted;
    }

    /**
     * @param dateDeleted the dateDeleted to set
     */
    public void setDateDeleted(Date dateDeleted) {
        this.dateDeleted = dateDeleted;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public boolean equals(Object other) {
        if (!(other instanceof MCRDELETEDITEMSPK)) {
            return false;
        }

        MCRDELETEDITEMSPK castother = (MCRDELETEDITEMSPK) other;

        return new EqualsBuilder().append(this.getIdentifier(), castother.getIdentifier()).append(this.getDateDeleted(),
                castother.getDateDeleted()).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getIdentifier()).append(getDateDeleted()).toHashCode();
    }
}

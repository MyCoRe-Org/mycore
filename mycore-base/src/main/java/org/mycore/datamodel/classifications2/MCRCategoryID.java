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

package org.mycore.datamodel.classifications2;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import org.mycore.common.MCRException;

/**
 * The composite identifier of a MCRCategory. If <code>rootID == ID</code> the
 * associated MCRCategory instance is a root category (a classification).
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date: 2008-04-11 09:14:19 +0000 (Fr, 11 Apr
 *          2008) $
 * @since 2.0
 */
@Embeddable
@Access(AccessType.FIELD)
public class MCRCategoryID implements Serializable {

    private static final long serialVersionUID = -5672923571406252855L;

    private static final Pattern validID = Pattern.compile("[^:$\\{\\}]+");

    private static final int ROOT_ID_LENGTH = 32;

    private static final int CATEG_ID_LENGTH = 128;

    @Basic
    @Column(name = "ClassID", length = ROOT_ID_LENGTH, nullable = false, updatable = false)
    private String rootID;

    @Basic
    @Column(name = "CategID", length = CATEG_ID_LENGTH, updatable = false)
    private String ID;

    private MCRCategoryID() {
        super();
    }

    /**
     * @param rootID
     *            aka Classification ID
     * @param id
     *            aka Category ID
     */
    public MCRCategoryID(String rootID, String id) {
        this();
        setID(id);
        setRootID(rootID);
    }

    public static MCRCategoryID rootID(String rootID) {
        return new MCRCategoryID(rootID, "");
    }

    /**
     * @param categoryId must be in format classificationId:categoryId
     * @return the {@link MCRCategoryID} if any
     */
    public static MCRCategoryID fromString(String categoryId) {
        StringTokenizer tok = new StringTokenizer(categoryId, ":");
        String rootId = tok.nextToken();
        if (!tok.hasMoreTokens()) {
            return rootID(rootId);
        }
        String categId = tok.nextToken();
        if (tok.hasMoreTokens()) {
            throw new IllegalArgumentException("CategoryId is ambiguous: " + categoryId);
        }
        return new MCRCategoryID(rootId, categId);
    }

    @Transient
    public boolean isRootID() {
        return ID == null || ID.equals("");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (ID == null || ID.isEmpty() ? 0 : ID.hashCode());
        result = PRIME * result + (rootID == null ? 0 : rootID.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
        final MCRCategoryID other = (MCRCategoryID) obj;
        if (ID == null) {
            if (other.ID != null && other.ID.length() > 0) {
                return false;
            }
        } else if (!ID.equals(other.ID) && (ID.length() > 0 || other.ID != null && other.ID.length() >= 0)) {
            return false;
        }
        if (rootID == null) {
            if (other.rootID != null) {
                return false;
            }
        } else if (!rootID.equals(other.rootID)) {
            return false;
        }
        return true;
    }

    /**
     * @return the ID
     */
    public String getID() {
        return ID == null ? "" : ID;
    }

    /**
     * @param id
     *            the ID to set
     */
    private void setID(String id) {
        if (id != null && id.length() > 0) {
            if (!validID.matcher(id).matches()) {
                throw new MCRException("category ID '" + id + "' is invalid and does not match: " + validID);
            }
            if (id.length() > CATEG_ID_LENGTH) {
                throw new MCRException(MessageFormat.format("category ID ''{0}'' is more than {1} characters long: {2}",
                    id, CATEG_ID_LENGTH, id.length()));
            }
        }
        ID = id;
    }

    /**
     * @return the rootID
     */
    public String getRootID() {
        return rootID;
    }

    /**
     * @param rootID
     *            the rootID to set
     */
    private void setRootID(String rootID) {
        if (!validID.matcher(rootID).matches()) {
            throw new MCRException(
                MessageFormat.format("classification ID ''{0}'' is invalid and does not match: {1}", rootID, validID));
        }
        if (rootID.length() > ROOT_ID_LENGTH) {
            throw new MCRException(MessageFormat.format(
                "classification ID ''{0}'' is more than {1} chracters long: {2}", rootID, ROOT_ID_LENGTH,
                rootID.length()));
        }
        this.rootID = rootID.intern();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (ID == null || ID.length() == 0) {
            return rootID;
        }
        return rootID + ':' + ID;
    }

}

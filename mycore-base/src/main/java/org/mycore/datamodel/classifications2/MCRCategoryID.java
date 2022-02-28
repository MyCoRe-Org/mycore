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
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.mycore.common.MCRException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

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
@JsonFormat(shape = JsonFormat.Shape.STRING)
public class MCRCategoryID implements Serializable {

    private static final long serialVersionUID = -5672923571406252855L;

    private static final Pattern VALID_ID = Pattern.compile("[^:$\\{\\}]+");

    private static final int ROOT_ID_LENGTH = 32;

    private static final int CATEG_ID_LENGTH = 128;

    @Basic
    @Column(name = "ClassID", length = ROOT_ID_LENGTH, nullable = false, updatable = false)
    private String rootID;

    @Basic
    @Column(name = "CategID", length = CATEG_ID_LENGTH, updatable = false)
    private String id;

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
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
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
        return id == null || id.equals("");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (id == null || id.isEmpty() ? 0 : id.hashCode());
        result = prime * result + (rootID == null ? 0 : rootID.hashCode());
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
        if (id == null) {
            if (other.id != null && other.id.length() > 0) {
                return false;
            }
        } else if (!id.equals(other.id) && (id.length() > 0 || other.id != null && other.id.length() >= 0)) {
            return false;
        }
        if (rootID == null) {
            return other.rootID == null;
        } else {
            return rootID.equals(other.rootID);
        }
    }

    /**
     * @return the ID
     */
    public String getID() {
        return id == null ? "" : id;
    }

    /**
     * @param id
     *            the ID to set
     */
    private void setID(String id) {
        if (id != null && id.length() > 0) {
            if (!VALID_ID.matcher(id).matches()) {
                throw new MCRException("category ID '" + id + "' is invalid and does not match: " + VALID_ID);
            }
            if (id.length() > CATEG_ID_LENGTH) {
                throw new MCRException(
                    new MessageFormat("category ID ''{0}'' is more than {1} characters long: {2}", Locale.ROOT)
                        .format(new Object[] { id, CATEG_ID_LENGTH, id.length() }));
            }
        }
        this.id = id;
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
        if (!VALID_ID.matcher(rootID).matches()) {
            throw new MCRException(
                new MessageFormat("classification ID ''{0}'' is invalid and does not match: {1}", Locale.ROOT)
                    .format(new Object[] { rootID, VALID_ID }));
        }
        if (rootID.length() > ROOT_ID_LENGTH) {
            throw new MCRException(String.format(Locale.ENGLISH,
                "classification ID ''%s'' is more than %d chracters long: %d", rootID, ROOT_ID_LENGTH,
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
    @JsonValue
    public String toString() {
        if (id == null || id.length() == 0) {
            return rootID;
        }
        return rootID + ':' + id;
    }

}

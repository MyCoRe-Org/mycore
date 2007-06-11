/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.classifications2;

import java.io.Serializable;

/**
 * The composite identifier of a MCRCategory. If <code>rootID == ID</code> the
 * associated MCRCategory instance is a root category (a classification).
 * 
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 * @since 2.0
 */
public class MCRCategoryID implements Serializable {

    private static final long serialVersionUID = -5672923571406252855L;

    private String rootID;

    private String ID;

    public MCRCategoryID() {
        super();
    }

    /**
     * @param rootID aka Classification ID
     * @param id aka Category ID
     */
    public MCRCategoryID(String rootID, String id) {
        super();
        this.rootID = rootID;
        ID = id;
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
        result = PRIME * result + ((ID == null) ? 0 : ID.hashCode());
        result = PRIME * result + ((rootID == null) ? 0 : rootID.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MCRCategoryID other = (MCRCategoryID) obj;
        if (ID == null) {
            if (other.ID != null)
                return false;
        } else if (!ID.equals(other.ID))
            return false;
        if (rootID == null) {
            if (other.rootID != null)
                return false;
        } else if (!rootID.equals(other.rootID))
            return false;
        return true;
    }

    /**
     * @return the ID
     */
    public String getID() {
        return ID;
    }

    /**
     * @param id
     *            the ID to set
     */
    public void setID(String id) {
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
    public void setRootID(String rootID) {
        this.rootID = rootID;
    }

}

/*
 * $RCSfile$
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

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * This class hold all primary keys of MCRURN
 * 
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRURNPK implements Serializable {
    private String mcrid;

    private String mcrurn;

    /**
     * Get the data field document ID.
     * 
     * @return Returns the document ID.
     */
    public String getMcrid() {
        return mcrid;
    }

    /**
     * Set the data field document ID.
     * 
     * @param mcrid
     *            The document ID to set.
     */
    public void setMcrid(String id) {
        this.mcrid = id;
    }

    /**
     * Get the data filed URN.
     * 
     * @return Returns the URN.
     */
    public String getMcrurn() {
        return mcrurn;
    }

    /**
     * Set the data filed URN.
     * 
     * @param mcrurn
     *            The URN to set.
     */
    public void setMcrurn(String urn) {
        this.mcrurn = urn;
    }

    /**
     * This method check the equalance of the given Object with this class. The
     * Object must be an instance of the class MCRURNPK.
     * 
     * @return Returns true if the object is equal.
     */
    public boolean equals(Object other) {
        if (!(other instanceof MCRURNPK)) {
            return false;
        }

        MCRURNPK castother = (MCRURNPK) other;

        return new EqualsBuilder().append(this.getMcrid(), castother.getMcrid()).append(this.getMcrurn(), castother.getMcrurn()).isEquals();
    }

    /**
     * This method return the hash code of this class as append of MCRFROM +
     * MCRTO + MCRTYPE
     * 
     * @return Returns the hash code.
     */
    public int hashCode() {
        return new HashCodeBuilder().append(getMcrid()).append(getMcrurn()).toHashCode();
    }
}

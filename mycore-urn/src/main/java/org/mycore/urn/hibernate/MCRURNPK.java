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

package org.mycore.urn.hibernate;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * This class hold all primary keys of MCRURN
 * 
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
@Embeddable
public class MCRURNPK implements Serializable {

    private static final long serialVersionUID = 8252257587972286981L;

    @Column(name = "MCRID", length = 64, nullable = false)
    private String mcrid;

    @Column(name = "MCRURN", length = 194, nullable = false)
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
     * @param id
     *            The document ID to set.
     */
    public void setMcrid(String id) {
        mcrid = id;
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
     * @param urn
     *            The URN to set.
     */
    public void setMcrurn(String urn) {
        mcrurn = urn;
    }

    /**
     * This method check the equalance of the given Object with this class. The
     * Object must be an instance of the class MCRURNPK.
     * 
     * @return Returns true if the object is equal.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MCRURNPK)) {
            return false;
        }

        MCRURNPK castother = (MCRURNPK) other;

        return new EqualsBuilder().append(getMcrid(), castother.getMcrid()).append(getMcrurn(), castother.getMcrurn())
            .isEquals();
    }

    /**
     * This method return the hash code of this class as append of MCRFROM +
     * MCRTO + MCRTYPE
     * 
     * @return Returns the hash code.
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getMcrid()).append(getMcrurn()).toHashCode();
    }
}

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
 * This class hold all primary keys of MCRLINKHREF
 * 
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRLINKHREFPK implements Serializable {

    private static final long serialVersionUID = -5838803852559721772L;

    private String mcrfrom;

    private String mcrto;

    private String mcrtype;

    /**
     * @param from
     *            the source ID of the link
     * @param to
     *            the target ID of the link
     * @param type
     *            the type of the link
     */
    public MCRLINKHREFPK(String from, String to, String type) {
        this.mcrfrom = from;
        this.mcrto = to;
        this.mcrtype = type;
    }

    /**
     * The constructor
     */
    public MCRLINKHREFPK() {
    }

    /**
     * Get the data field from.
     * 
     * @return Returns the mcrfrom.
     */
    public String getMcrfrom() {
        return mcrfrom;
    }

    /**
     * Set the data field from.
     * 
     * @param mcrfrom
     *            The mcrfrom to set.
     */
    public void setMcrfrom(String mcrfrom) {
        this.mcrfrom = mcrfrom;
    }

    /**
     * Get the data filed to.
     * 
     * @return Returns the mcrto.
     */
    public String getMcrto() {
        return mcrto;
    }

    /**
     * Set the data filed to.
     * 
     * @param mcrto
     *            The mcrto to set.
     */
    public void setMcrto(String mcrto) {
        this.mcrto = mcrto;
    }

    /**
     * Get the data filed type.
     * 
     * @return Returns the mcrtype.
     */
    public String getMcrtype() {
        return mcrtype;
    }

    /**
     * Set the data filed type.
     * 
     * @param mcrtype
     *            The mcrtype to set.
     */
    public void setMcrtype(String mcrtype) {
        this.mcrtype = mcrtype;
    }

    /**
     * This method check the equalance of the given Object with this class. The
     * Object must be an instance of the class MCRLINKHREFPK.
     * 
     * @return Returns true if the object is equal.
     */
    public boolean equals(Object other) {
        if (!(other instanceof MCRLINKHREFPK)) {
            return false;
        }

        MCRLINKHREFPK castother = (MCRLINKHREFPK) other;

        return new EqualsBuilder().append(this.getMcrfrom(), castother.getMcrfrom()).append(this.getMcrto(), castother.getMcrto()).append(this.getMcrtype(),
                castother.getMcrtype()).isEquals();
    }

    /**
     * This method return the hash code of this class as append of MCRFROM +
     * MCRTO + MCRTYPE
     * 
     * @return Returns the hash code.
     */
    public int hashCode() {
        return new HashCodeBuilder().append(getMcrfrom()).append(getMcrto()).append(getMcrtype()).toHashCode();
    }
}

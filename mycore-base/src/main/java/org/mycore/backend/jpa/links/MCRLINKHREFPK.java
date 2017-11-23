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

package org.mycore.backend.jpa.links;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * This class hold all primary keys of MCRLINKHREF
 * 
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
@Embeddable
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
        mcrfrom = from;
        mcrto = to;
        mcrtype = type;
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
    @Basic
    @Column(length = 64, name = "MCRFROM")
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
    @Basic
    @Column(length = 194, name = "MCRTO")
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
    @Basic
    @Column(length = 75, name = "MCRTYPE")
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mcrfrom == null) ? 0 : mcrfrom.hashCode());
        result = prime * result + ((mcrto == null) ? 0 : mcrto.hashCode());
        result = prime * result + ((mcrtype == null) ? 0 : mcrtype.hashCode());
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
        MCRLINKHREFPK other = (MCRLINKHREFPK) obj;
        if (mcrfrom == null) {
            if (other.mcrfrom != null) {
                return false;
            }
        } else if (!mcrfrom.equals(other.mcrfrom)) {
            return false;
        }
        if (mcrto == null) {
            if (other.mcrto != null) {
                return false;
            }
        } else if (!mcrto.equals(other.mcrto)) {
            return false;
        }
        if (mcrtype == null) {
            if (other.mcrtype != null) {
                return false;
            }
        } else if (!mcrtype.equals(other.mcrtype)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MCRLINKHREFPK [mcrfrom=" + mcrfrom + ", mcrto=" + mcrto + ", mcrtype=" + mcrtype + "]";
    }

}

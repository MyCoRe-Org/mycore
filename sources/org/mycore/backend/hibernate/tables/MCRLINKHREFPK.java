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

public class MCRLINKHREFPK implements Serializable {
    private String mcrfrom;

    private String mcrto;

    /**
     * @return Returns the mcrfrom.
     */
    public String getMcrfrom() {
        return mcrfrom;
    }

    /**
     * @param mcrfrom
     *            The mcrfrom to set.
     */
    public void setMcrfrom(String mcrfrom) {
        this.mcrfrom = mcrfrom;
    }

    /**
     * @return Returns the mcrto.
     */
    public String getMcrto() {
        return mcrto;
    }

    /**
     * @param mcrto
     *            The mcrto to set.
     */
    public void setMcrto(String mcrto) {
        this.mcrto = mcrto;
    }

    public boolean equals(Object other) {
        if (!(other instanceof MCRLINKHREFPK)) {
            return false;
        }

        MCRLINKHREFPK castother = (MCRLINKHREFPK) other;

        return new EqualsBuilder().append(this.getMcrfrom(), castother.getMcrfrom()).append(this.getMcrto(), castother.getMcrto()).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getMcrfrom()).append(getMcrto()).toHashCode();
    }
}

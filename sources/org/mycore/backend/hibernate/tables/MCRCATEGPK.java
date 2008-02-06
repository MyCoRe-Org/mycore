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

package org.mycore.backend.hibernate.tables;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class MCRCATEGPK implements Serializable {

    private static final long serialVersionUID = -2883097610006670647L;

    public String id;

    public String clid;

    public MCRCATEGPK() {
    }

    public MCRCATEGPK(String id, String clid) {
        this.id = id;
        this.clid = clid;
    }

    /**
     * @return Returns the cLID.
     */
    public String getClid() {
        return clid;
    }

    /**
     * @param clid
     *            The cLID to set.
     */
    public void setClid(String clid) {
        this.clid = clid;
    }

    /**
     * @return Returns the iD.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *            The iD to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    public boolean equals(Object other) {
        if (!(other instanceof MCRCATEGPK)) {
            return false;
        }

        MCRCATEGPK castother = (MCRCATEGPK) other;

        return new EqualsBuilder().append(this.getClid(), castother.getClid()).append(this.getId(), castother.getId()).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getClid()).append(getId()).toHashCode();
    }
}

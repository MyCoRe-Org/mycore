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

public class MCRGROUPMEMBERSPK implements Serializable {
    private static final long serialVersionUID = 4308033318842963890L;

    public long id;

    private MCRGROUPS gid;

    private MCRUSERS userid;

    public MCRGROUPMEMBERSPK() {
    }

    public MCRGROUPMEMBERSPK(MCRGROUPS gid, MCRUSERS userid) {
        this.gid = gid;
        this.userid = userid;
    }

    public MCRGROUPMEMBERSPK(long id) {
        this.id = id;
    }

    /**
     * @return Returns the gid.
     */
    public MCRGROUPS getGid() {
        return gid;
    }

    /**
     * @param gid
     *            The gid to set.
     */
    public void setGid(MCRGROUPS gid) {
        this.gid = gid;
    }

    /**
     * @return Returns the userid.
     */
    public MCRUSERS getUserid() {
        return userid;
    }

    /**
     * @param userid
     *            The userid to set.
     */
    public void setUserid(MCRUSERS userid) {
        this.userid = userid;
    }

    public boolean equals(Object other) {
        if (!(other instanceof MCRGROUPMEMBERSPK)) {
            return false;
        }

        MCRGROUPMEMBERSPK castother = (MCRGROUPMEMBERSPK) other;

        return new EqualsBuilder().append(this.getGid(), castother.getGid()).append(this.getUserid(), castother.getUserid()).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getGid()).append(getUserid()).toHashCode();
    }
}

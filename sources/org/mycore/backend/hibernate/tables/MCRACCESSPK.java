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

public class MCRACCESSPK implements Serializable {

    private static final long serialVersionUID = 1177905976922683366L;

    private String acpool;

    private String objid;

    public MCRACCESSPK() {
    }

    public MCRACCESSPK(String acpool, String objid) {
        this.acpool = acpool;
        this.objid = objid;
    }

    public String getAcpool() {
        return acpool;
    }

    public void setAcpool(String acpool) {
        this.acpool = acpool;
    }

    public String getObjid() {
        return objid;
    }

    public void setObjid(String objid) {
        this.objid = objid;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((acpool == null) ? 0 : acpool.hashCode());
        result = PRIME * result + ((objid == null) ? 0 : objid.hashCode());
        return result;
    }

    /* (non-Javadoc)
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
        final MCRACCESSPK other = (MCRACCESSPK) obj;
        if (acpool == null) {
            if (other.acpool != null)
                return false;
        } else if (!acpool.equals(other.acpool))
            return false;
        if (objid == null) {
            if (other.objid != null)
                return false;
        } else if (!objid.equals(other.objid))
            return false;
        return true;
    }

}

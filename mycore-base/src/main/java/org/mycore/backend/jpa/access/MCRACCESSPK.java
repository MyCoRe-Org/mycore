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

package org.mycore.backend.jpa.access;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class MCRACCESSPK implements Serializable {

    private static final long serialVersionUID = 1177905976922683366L;

    @Column(name = "ACPOOL")
    private String acpool;

    @Column(name = "OBJID")
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
        result = PRIME * result + (acpool == null ? 0 : acpool.hashCode());
        result = PRIME * result + (objid == null ? 0 : objid.hashCode());
        return result;
    }

    /* (non-Javadoc)
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
        final MCRACCESSPK other = (MCRACCESSPK) obj;
        if (acpool == null) {
            if (other.acpool != null) {
                return false;
            }
        } else if (!acpool.equals(other.acpool)) {
            return false;
        }
        if (objid == null) {
            if (other.objid != null) {
                return false;
            }
        } else if (!objid.equals(other.objid)) {
            return false;
        }
        return true;
    }

}

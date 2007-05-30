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

import java.sql.Timestamp;

public class MCRACCESS {
    private MCRACCESSPK key;

    private String rid;
    
    private String creator;

    private Timestamp creationdate;

    public MCRACCESS() {
    }

    public MCRACCESS(MCRACCESSPK key) {
        this.key = new MCRACCESSPK(key.getAcpool(), key.getObjid());
    }

    public MCRACCESS(String rid, String acpool, String objid, String creator, Timestamp creationdate) {
        this.key = new MCRACCESSPK(acpool, objid);
        this.rid = rid;
        this.creator = creator;
        this.creationdate = creationdate;
    }

    public MCRACCESSPK getKey() {
        return this.key;
    }

    public void setKey(MCRACCESSPK key) {
        this.key = key;
    }

    public Timestamp getCreationdate() {
        return creationdate;
    }

    public void setCreationdate(Timestamp creationdate) {
        this.creationdate = creationdate;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }
}

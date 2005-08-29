/*
 * This file is part of ** M y C o R e ** Visit our homepage at
 * http://www.mycore.de/ for details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, normally in the file license.txt. If not, write to the Free
 * Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307
 * USA
 */
package org.mycore.backend.hibernate.tables;

import java.sql.Timestamp;

public class MCRACCESS {
    
    private String rid;
    private String creator;
    private Timestamp creationdate;
    private String objid;
    private String acpool;
    
    
    public String getAcpool() {
        return acpool;
    }
    public void setAcpool(String acpool) {
        this.acpool = acpool;
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
    public String getObjid() {
        return objid;
    }
    public void setObjid(String objid) {
        this.objid = objid;
    }
    public String getRid() {
        return rid;
    }
    public void setRid(String rid) {
        this.rid = rid;
    }
    
    

}

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

package org.mycore.access.mcrimpl;

import java.util.Date;

public class MCRRuleMapping {
    private String ruleid;

    private String objid;

    private String pool;

    private String creator;

    private Date creationdate;

    public MCRRuleMapping() {
    }

    /**
     * returns MCRObjectID as string
     * 
     * @return MCRObjectID as string
     */
    public String getObjId() {
        return objid;
    }

    public void setObjId(String objid) {
        this.objid = objid;
    }

    public String getPool() {
        return pool;
    }

    public void setPool(String pool) {
        this.pool = pool;
    }

    public String getRuleId() {
        return ruleid;
    }

    public void setRuleId(String ruleid) {
        this.ruleid = ruleid;
    }

    public Date getCreationdate() {
        return new Date(creationdate.getTime());
    }

    public void setCreationdate(Date creationdate) {
        this.creationdate = new Date(creationdate.getTime());
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
}

package org.mycore.backend.hibernate.tables;
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
import java.sql.Timestamp;

public class MCRACCESSRULE {
    
    private String rid;
    private String creator;
    private Timestamp creationdate;
    private String rule;
    private String description;
    
    
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
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getRid() {
        return rid;
    }
    public void setRid(String rid) {
        this.rid = rid;
    }
    public String getRule() {
        return rule;
    }
    public void setRule(String rule) {
        this.rule = rule;
    }
    

}

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((creationdate == null) ? 0 : creationdate.hashCode());
        result = prime * result + ((creator == null) ? 0 : creator.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((rid == null) ? 0 : rid.hashCode());
        result = prime * result + ((rule == null) ? 0 : rule.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        System.out.println("EQUALS");
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof MCRACCESSRULE))
            return false;
        final MCRACCESSRULE other = (MCRACCESSRULE) obj;
        if (creationdate == null) {
            if (other.getCreationdate() != null)
                return false;
        } else {
            if (other.getCreationdate() == null)
                return false;
            // We will remove milliseconds as they don't need to be saved
            long thisLong = (long) Math.floor(creationdate.getTime() / 1000);
            long otherLong = (long) Math.floor(other.getCreationdate().getTime() / 1000);
            if (thisLong != otherLong)
                return false;
        }
        if (creator == null) {
            if (other.getCreator() != null)
                return false;
        } else if (!creator.equals(other.getCreator()))
            return false;
        if (description == null) {
            if (other.getDescription() != null)
                return false;
        } else if (!description.equals(other.getDescription()))
            return false;
        if (rid == null) {
            if (other.getRid() != null)
                return false;
        } else if (!rid.equals(other.getRid()))
            return false;
        if (rule == null) {
            if (other.getRule() != null)
                return false;
        } else if (!rule.equals(other.getRule()))
            return false;
        return true;
    }
}

/*
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.hibernate.tables;

import java.util.Date;

class MCRCATEG
{
    private String id;
    private String clid;
    private String pid;
    private String url;

    /**
    * @hibernate.property
    * column="ID"
    * not-null="true"
    * update="true"
    */
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    /**
    * @hibernate.property
    * column="CLID"
    * not-null="true"
    * update="true"
    */
    public String getClid() {
        return clid;
    }
    public void setClid(String clid) {
        this.clid = clid;
    }

    /**
    * @hibernate.property
    * column="PID"
    * not-null="true"
    * update="true"
    */
    public String getPid() {
        return pid;
    }
    public void setPid(String pid) {
        this.pid = pid;
    }

    /**
    * @hibernate.property
    * column="URL"
    * not-null="true"
    * update="true"
    */
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
}

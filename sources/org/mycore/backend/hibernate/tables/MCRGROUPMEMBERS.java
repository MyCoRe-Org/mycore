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

public class MCRGROUPMEMBERS {
    private MCRGROUPMEMBERSPK key;

    public MCRGROUPMEMBERS() {
        this.key = new MCRGROUPMEMBERSPK();
    }

    public MCRGROUPMEMBERS(MCRGROUPS gid, MCRUSERS userid, String groupid) {
        this.key = new MCRGROUPMEMBERSPK(gid, userid);
    }

    public MCRGROUPMEMBERS(long id) {
        this.key = new MCRGROUPMEMBERSPK(id);
    }

    /**
     * @hibernate.property column="Primary Key" not-null="true" update="true"
     */
    public MCRGROUPMEMBERSPK getKey() {
        return key;
    }

    public void setKey(MCRGROUPMEMBERSPK key) {
        this.key = key;
    }

    /**
     * @hibernate.property column="GID" not-null="true" update="true"
     */
    public MCRGROUPS getGid() {
        return key.getGid();
    }

    public void setGid(MCRGROUPS gid) {
        key.setGid(gid);
    }

    /**
     * @hibernate.property column="USERID" not-null="true" update="true"
     */
    public MCRUSERS getUserid() {
        return key.getUserid();
    }

    public void setUserid(MCRUSERS userid) {
        key.setUserid(userid);
    }

}

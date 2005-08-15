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

public class MCRGROUPADMINS {

	private MCRGROUPADMINSPK key;

	public MCRGROUPADMINS() {
		this.key = new MCRGROUPADMINSPK();
	}

	public MCRGROUPADMINS(String gid, String userid, String groupid) {
		this.key = new MCRGROUPADMINSPK(gid, userid, groupid);
	}

	/**
	 * @hibernate.property column="Primary Key" not-null="true" update="true"
	 */
	public MCRGROUPADMINSPK getKey() {
		return key;
	}

	public void setKey(MCRGROUPADMINSPK key) {
		this.key = key;
	}

	/**
	 * @hibernate.property column="GID" not-null="true" update="true"
	 */
	public String getGid() {
		return key.getGid();
	}

	public void setGid(String gid) {
		key.setGid(gid);
	}

	/**
	 * @hibernate.property column="USERID" not-null="true" update="true"
	 */
	public String getUserid() {
		return key.getUserid();
	}

	public void setUserid(String userid) {
		key.setUserid(userid);
	}

	/**
	 * @hibernate.property column="GROUPID" not-null="true" update="true"
	 */
	public String getGroupid() {
		return key.getGroupid();
	}

	public void setGroupid(String groupid) {
		key.setGroupid(groupid);
	}
}

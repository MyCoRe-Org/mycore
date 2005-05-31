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

public class MCRFSNODES
{
    private String id;
    private String pid;
    private String type;
    private String owner;
    private String name;
    private String label;
    private long size;
    private Date date;
    private String storeid;
    private String storageid;
    private String fctid;
    private String md5;
    private int numchdd;
    private int numchdf;
    private int numchtd;
    private int numchtf;

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
    * column="TYPE"
    * not-null="true"
    * update="true"
    */
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    /**
    * @hibernate.property
    * column="OWNER"
    * not-null="true"
    * update="true"
    */
    public String getOwner() {
        return owner;
    }
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
    * @hibernate.property
    * column="NAME"
    * not-null="true"
    * update="true"
    */
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    /**
    * @hibernate.property
    * column="LABEL"
    * not-null="true"
    * update="true"
    */
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }

    /**
    * @hibernate.property
    * column="SIZE"
    * not-null="true"
    * update="true"
    */
    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }

    /**
    * @hibernate.property
    * column="DATE"
    * not-null="true"
    * update="true"
    */
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }

    /**
    * @hibernate.property
    * column="STOREID"
    * not-null="true"
    * update="true"
    */
    public String getStoreid() {
        return storeid;
    }
    public void setStoreid(String storeid) {
        this.storeid = storeid;
    }

    /**
    * @hibernate.property
    * column="STORAGEID"
    * not-null="true"
    * update="true"
    */
    public String getStorageid() {
        return storageid;
    }
    public void setStorageid(String storageid) {
        this.storageid = storageid;
    }

    /**
    * @hibernate.property
    * column="FCTID"
    * not-null="true"
    * update="true"
    */
    public String getFctid() {
        return fctid;
    }
    public void setFctid(String fctid) {
        this.fctid = fctid;
    }

    /**
    * @hibernate.property
    * column="MD5"
    * not-null="true"
    * update="true"
    */
    public String getMd5() {
        return md5;
    }
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    /**
    * @hibernate.property
    * column="NUMCHDD"
    * not-null="true"
    * update="true"
    */
    public int getNumchdd() {
        return numchdd;
    }
    public void setNumchdd(int numchdd) {
        this.numchdd = numchdd;
    }

    /**
    * @hibernate.property
    * column="NUMCHDF"
    * not-null="true"
    * update="true"
    */
    public int getNumchdf() {
        return numchdf;
    }
    public void setNumchdf(int numchdf) {
        this.numchdf = numchdf;
    }

    /**
    * @hibernate.property
    * column="NUMCHTD"
    * not-null="true"
    * update="true"
    */
    public int getNumchtd() {
        return numchtd;
    }
    public void setNumchtd(int numchtd) {
        this.numchtd = numchtd;
    }

    /**
    * @hibernate.property
    * column="NUMCHTF"
    * not-null="true"
    * update="true"
    */
    public int getNumchtf() {
        return numchtf;
    }
    public void setNumchtf(int numchtf) {
        this.numchtf = numchtf;
    }
}

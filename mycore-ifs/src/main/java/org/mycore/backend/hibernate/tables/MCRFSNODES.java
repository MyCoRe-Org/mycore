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

import java.util.Date;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
    uniqueConstraints = { @UniqueConstraint(columnNames = { "pid", "name" }, name = "mcrfsnodes_pid_name_idx") },
    indexes = { @Index(columnList = "pid, owner", name = "mcrfsnodes_pid_owner_idx") })
public class MCRFSNODES {
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

    @Id
    @Column(name = "ID", length = 16)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Column(name = "PID", length = 16)
    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    @Column(name = "TYPE", length = 1, nullable = false)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "OWNER", length = 64, nullable = false)
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Column(name = "NAME", length = 250, nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "LABEL", length = 250)
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Column(name = "SIZE", nullable = false)
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Column(name = "DATE")
    public Date getDate() {
        return Optional
            .ofNullable(date)
            .map(Date::getTime)
            .map(Date::new)
            .orElse(null);
    }

    public void setDate(Date date) {
        this.date = new Date(date.getTime());
    }

    @Column(name = "STOREID", length = 32)
    public String getStoreid() {
        return storeid;
    }

    public void setStoreid(String storeid) {
        this.storeid = storeid;
    }

    @Column(name = "STORAGEID", length = 250)
    public String getStorageid() {
        return storageid;
    }

    public void setStorageid(String storageid) {
        this.storageid = storageid;
    }

    @Column(name = "FCTID", length = 32)
    public String getFctid() {
        return fctid;
    }

    public void setFctid(String fctid) {
        this.fctid = fctid;
    }

    @Column(name = "MD5", length = 32)
    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @Column(name = "NUMCHDD")
    public int getNumchdd() {
        return numchdd;
    }

    public void setNumchdd(int numchdd) {
        this.numchdd = numchdd;
    }

    @Column(name = "NUMCHDF")
    public int getNumchdf() {
        return numchdf;
    }

    public void setNumchdf(int numchdf) {
        this.numchdf = numchdf;
    }

    @Column(name = "NUMCHTD")
    public int getNumchtd() {
        return numchtd;
    }

    public void setNumchtd(int numchtd) {
        this.numchtd = numchtd;
    }

    @Column(name = "NUMCHTF")
    public int getNumchtf() {
        return numchtf;
    }

    public void setNumchtf(int numchtf) {
        this.numchtf = numchtf;
    }
}

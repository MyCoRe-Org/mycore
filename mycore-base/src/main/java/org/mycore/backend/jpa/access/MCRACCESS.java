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

package org.mycore.backend.jpa.access;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "MCRACCESS")
public class MCRACCESS {
    @EmbeddedId
    private MCRACCESSPK key;

    @ManyToOne
    @JoinColumn(name = "rid", nullable = false)
    private MCRACCESSRULE rule;

    @Column(name = "CREATOR", length = 64, nullable = false)
    private String creator;

    @Column(name = "CREATIONDATE", length = 64, nullable = false)
    private Timestamp creationdate;

    public MCRACCESS() {
    }

    public MCRACCESS(MCRACCESSPK key) {
        this.key = new MCRACCESSPK(key.getAcpool(), key.getObjid());
    }

    public MCRACCESS(MCRACCESSRULE rule, String acpool, String objid, String creator, Timestamp creationdate) {
        key = new MCRACCESSPK(acpool, objid);
        this.rule = rule;
        this.creator = creator;
        this.creationdate = creationdate;
    }

    public MCRACCESSPK getKey() {
        return key;
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

    public MCRACCESSRULE getRule() {
        return rule;
    }

    public void setRule(MCRACCESSRULE rule) {
        this.rule = rule;
    }
}

/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "MCRACCESSRULE")
public class MCRACCESSRULE {

    @Id
    @Column(name = "RID")
    private String rid;

    @Column(name = "CREATOR", length = 64, nullable = false)
    private String creator;

    @Column(name = "CREATIONDATE", length = 64, nullable = false)
    private Timestamp creationdate;

    @Column(name = "RULE", length = 2048000, nullable = false)
    private String rule;

    @Column(name = "DESCRIPTION", nullable = false)
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
        result = prime * result + (creationdate == null ? 0 : creationdate.hashCode());
        result = prime * result + (creator == null ? 0 : creator.hashCode());
        result = prime * result + (description == null ? 0 : description.hashCode());
        result = prime * result + (rid == null ? 0 : rid.hashCode());
        result = prime * result + (rule == null ? 0 : rule.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        System.out.println("EQUALS");
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MCRACCESSRULE other)) {
            return false;
        }
        return timestampsAreEqual(creationdate, other.getCreationdate()) &&
            stringsAreEqual(creator, other.getCreator()) &&
            stringsAreEqual(description, other.getDescription()) &&
            stringsAreEqual(rid, other.getRid()) &&
            stringsAreEqual(rule, other.getRule());
    }

    private boolean timestampsAreEqual(Timestamp timestamp, Timestamp otherTimestamp) {
        return (!((timestamp == null && otherTimestamp != null)
            || !Objects.equals(timestamp, otherTimestamp) &&
                !Objects.equals(timestamp.getTime() / 1000, otherTimestamp.getTime() / 1000)));
    }

    private boolean stringsAreEqual(String string, String otherString) {
        return Objects.equals(string, otherString);
    }
}

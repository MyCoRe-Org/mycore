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

import java.io.Serializable;

import org.apache.commons.lang.builder.HashCodeBuilder;

public class MCRXMLTABLEPK implements Serializable {
    private String id;

    private int version;

    private String type;

    public MCRXMLTABLEPK(String id, int version, String type) {
        this.id = id;
        this.version = version;
        this.type = type;
    }

    public MCRXMLTABLEPK() {
    }

    public String getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean equals(Object _other) {
        if (!(_other instanceof MCRXMLTABLEPK)) {
            return false;
        }

        MCRXMLTABLEPK other = (MCRXMLTABLEPK) _other;

        return this.id.equals(other.id) && (this.version == other.version) && this.type.equals(other.type);
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getId()).append(getVersion()).append(getType()).toHashCode();
    }
}

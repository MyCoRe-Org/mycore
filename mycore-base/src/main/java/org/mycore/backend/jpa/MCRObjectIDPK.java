/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  MyCoRe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MyCoRe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.backend.jpa;

import java.io.Serializable;
import java.util.Objects;

import org.mycore.backend.jpa.objectinfo.MCRObjectInfoEntity;
import org.mycore.datamodel.metadata.MCRObjectID;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;

/**
 * Use this wrapper if you want a {@link MCRObjectID} as a primary key with name <code>id</code>
 * in your JPA mapping.
 *
 * <pre>{@code
 * @Entity
 * @IdClass(MCRObjectIDPK.class)
 * public class EntityClass {
 *
 *     private MCRObjectID id;
 *
 *     […]
 *
 *     @Id
 *     public MCRObjectID getId() {
 *         return id;
 *     }
 *
 *     public void setId(MCRObjectID id) {
 *         this.id = id;
 *     }
 *
 *     […]
 *
 * }
 * }</pre>
 *
 * @see MCRObjectInfoEntity
 */
@Access(AccessType.FIELD)
public class MCRObjectIDPK implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Convert(converter = MCRObjectIDConverter.class)
    @Basic
    @Column(length = MCRObjectID.MAX_LENGTH)
    public MCRObjectID id;

    /**
     * Use this constructor for quick queries.
     *
     * Sample-Code:<br>
     * <pre>{@code
     *     EntityManager em = […];
     *     em.find(EntityClass.class, new MCRObjectIDPK(MCRObjectID.getInstance('mir_mods_00004711')));
     * }</pre>
     * @see jakarta.persistence.EntityManager#find(Class, Object)
     */
    public MCRObjectIDPK(MCRObjectID id) {
        this.id = id;
    }

    public MCRObjectIDPK() {
        //empty for JPA implementations
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MCRObjectIDPK that = (MCRObjectIDPK) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

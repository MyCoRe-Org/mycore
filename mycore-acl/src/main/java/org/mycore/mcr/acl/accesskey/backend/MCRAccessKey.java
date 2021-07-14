/*
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

package org.mycore.mcr.acl.accesskey.backend;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.mycore.backend.jpa.MCRObjectIDConverter;
import org.mycore.datamodel.metadata.MCRObjectID;

@NamedQueries({
    @NamedQuery(name = "MCRAccessKey.getById",
        query = "SELECT k"
            + "  FROM MCRAccessKey k"
            + "  WHERE k.objectId = :objId"),
    @NamedQuery(name = "MCRAccessKey.getByValue",
        query = "SELECT k"
            + "  FROM MCRAccessKey k"
            + "  WHERE k.value = :value AND k.objectId = :objId"),
    @NamedQuery(name = "MCRAccessKey.getByType",
        query = "SELECT k"
            + "  FROM MCRAccessKey k"
            + "  WHERE k.type = :type AND k.objectId = :objId"),
    @NamedQuery(name = "MCRAccessKey.clearById",
        query = "DELETE"
            + "  FROM MCRAccessKey k"
            + "  WHERE k.objectId = :objId"),
    @NamedQuery(name = "MCRAccessKey.clear",
        query = "DELETE"
            + "  FROM MCRAccessKey k"),
})
 
/**
 * Access keys for a {@link MCRObject}.
 * An access keys contains a value and a type.
 * Value is the key value of the key and type the permission.
 */
@Entity
@Table(name = "MCRAccessKey")
public class MCRAccessKey {

    private static final long serialVersionUID = 1L;

    /** The unique and internal information id */
    private int id;

    /** The access key information*/
    private MCRObjectID mcrObjectId; 

    /** The key value*/
    private String value;

    /** The permission type*/
    private String type;

    private MCRAccessKey() {
    }

    /**
     * Creates a new access key with value and type.
     *
     * @param value the value the user must know to acquire permission.
     * @param type the type of permission.
     */
    public MCRAccessKey(final String value, final String type) {
        setValue(value);
        setType(type);
    }

    /**
     * Creates a new access key with value and type.
     *
     * @param objectId the assigned {@link MCRObjectID}.
     * @param value the value the user must know to acquire permission.
     * @param type the type of permission.
     */
    public MCRAccessKey(final MCRObjectID objectId, final String value, final String type) {
        setObjectId(objectId);
        setValue(value);
        setType(type);
    }

    /**
     * @return the linked mcrObjectId
     */
    @JsonIgnore
    @Column(name = "object_id",
        length = MCRObjectID.MAX_LENGTH,
        nullable = false)
    @Convert(converter = MCRObjectIDConverter.class)
    public MCRObjectID getObjectId() {
        return mcrObjectId;
    }

    /**
     * @param mcrObjectId the {@link MCRObjectID} to set
     */
    public void setObjectId(final MCRObjectID mcrObjectId) {
        this.mcrObjectId = mcrObjectId;
    }

    /**
     * @return internal id
     */
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accesskey_id",
        nullable = false)
    public int getId() {
        return id;
    }

    /**
     * @param id internal id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the key value
     */
    @Column(name = "value",
        nullable = false)
    public String getValue() {
        return value;
    }

    /**
     * @param value key value
    */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * @return permission type 
     */
    @Column(name = "type",
        nullable = false)
    public String getType() {
        return type;
    }

    /**
     * @param type permission type
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MCRAccessKey)) {
            return false;
        }
        MCRAccessKey other = (MCRAccessKey) o;
        return this.id == other.getId() && this.type.equals(other.getType())
            && this.value.equals(other.getValue());
    }
}

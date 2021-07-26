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

package org.mycore.mcr.acl.accesskey.model;

import java.util.Date;

import org.mycore.backend.jpa.MCRObjectIDConverter;
import org.mycore.datamodel.metadata.MCRObjectID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@NamedQueries({
    @NamedQuery(name = "MCRAccessKey.getWithObjectId",
        query = "SELECT k"
            + "  FROM MCRAccessKey k"
            + "  WHERE k.objectId = :objectId"
            + "  ORDER BY k.lastModified ASC"),
    @NamedQuery(name = "MCRAccessKey.getWithSecret",
        query = "SELECT k"
            + "  FROM MCRAccessKey k"
            + "  WHERE k.secret = :secret AND k.objectId = :objectId"),
    @NamedQuery(name = "MCRAccessKey.getWithType",
        query = "SELECT k"
            + "  FROM MCRAccessKey k"
            + "  WHERE k.type = :type AND k.objectId = :objectId"),
    @NamedQuery(name = "MCRAccessKey.clearWithObjectId",
        query = "DELETE"
            + "  FROM MCRAccessKey k"
            + "  WHERE k.objectId = :objectId"),
    @NamedQuery(name = "MCRAccessKey.clear",
        query = "DELETE"
            + "  FROM MCRAccessKey k"),
})
 
/**
 * Access keys for a {@link MCRObject}.
 * An access keys contains a secret and a type.
 * Value is the key secret of the key and type the permission.
 */
@Entity
@Table(name = "MCRAccessKey")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MCRAccessKey {

    private static final long serialVersionUID = 1L;

    /** The unique and internal information id */
    private int id;

    /** The access key information */
    private MCRObjectID objectId; 

    /** The secret */
    private String secret;

    /** The permission type */
    private String type;

    /** The status */
    private Boolean isActive;

    /** The expiration date* */
    private Date expiration;

    /** The comment */
    private String comment;

    /** The date of creation */
    private Date created;

    /** The name of creator */
    private String createdBy;

    /** The date of last modification */
    private Date lastModified;

    /** The name of the last modifier */
    private String lastModifiedBy;

    protected MCRAccessKey() {
    }

    /**
     * Creates a new access key with secret and type.
     *
     * @param secret the secret the user must know to acquire permission.
     * @param type the type of permission.
     */
    public MCRAccessKey(final String secret, final String type) {
        this();
        setSecret(secret);
        setType(type);
    }

    /**
     * @return the linked objectId
     */
    @JsonIgnore
    @Column(name = "object_id",
        length = MCRObjectID.MAX_LENGTH,
        nullable = false)
    @Convert(converter = MCRObjectIDConverter.class)
    public MCRObjectID getObjectId() {
        return objectId;
    }

    /**
     * @param objectId the {@link MCRObjectID} to set
     */
    public void setObjectId(final MCRObjectID objectId) {
        this.objectId = objectId;
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
     * @return the key secret
     */
    @Column(name = "secret",
        nullable = false)
    public String getSecret() {
        return secret;
    }

    /**
     * @param secret key secret
     */
    public void setSecret(final String secret) {
        this.secret = secret;
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

    /**
     * @return active or not
     */
    @Column(name = "isActive",
        nullable = false)
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * @param isActive the state
     */
    public void setIsActive(final Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * @return expiration date
     */
    @Column(name = "expiration")
    public Date getExpiration() {
        return expiration;
    }

    /**
     * @param expiration the expiration date
     */
    public void setExpiration(final Date expiration) {

        this.expiration = expiration;
    }

    /**
     * @return comment
     */
    @Column(name = "comment")
    public String getComment() {
        return comment;
    }

    /**
     * @param comment the comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return date of creation
     */
    @Column(name = "created")
    public Date getCreated() {
        return created;
    }

    /**
     * @param created date of creation
     */
    public void setCreated(final Date created) {
        this.created = created;
    }

    /**
     * @return name of creator
     */
    @Column(name = "createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * @param createdBy name of creator
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * @return date of last modification
     */
    @Column(name = "lastModified")
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * @param lastModified date of last modification
     */
    @Column(name = "lastModified")
    public void setLastModified(final Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @return name of last modifier
     */
    @Column(name = "lastModifiedBy")
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * @param lastModifiedBy name of last modifier
     */
    public void setLastModifiedBy(final String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
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
            && this.secret.equals(other.getSecret());
    }
}

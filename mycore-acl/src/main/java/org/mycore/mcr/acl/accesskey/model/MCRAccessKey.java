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

/**
 * Model for access key.
 * 
 * An access keys contains a secret and a type.
 * Value is the key secret of the key and type the permission.
 */
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

    /**
     * Creates blank MCRAccessKey.
     */
    protected MCRAccessKey() {
    }

    /**
     * Creates a MCRAccessKey with secret and type.
     *
     * @param secret the secret the user must know to acquire permission.
     * @param type the type of permission.
     */
    public MCRAccessKey(String secret, String type) {
        this();
        setSecret(secret);
        setType(type);
    }

    /**
     * Returns the linked {@link MCRObjectID}.
     * 
     * @return the MCRObjectID
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
     * Sets {@link MCRObjectID}.
     * 
     * @param objectId the MCRObjectID
     */
    public void setObjectId(MCRObjectID objectId) {
        this.objectId = objectId;
    }

    /**
     * Returns the intenal id.
     * 
     * @return the internal id
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
     * Sets the internal id.
     * 
     * @param id the internal id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the secret.
     * 
     * @return the secret
     */
    @Column(name = "secret",
        nullable = false)
    public String getSecret() {
        return secret;
    }

    /**
     * Sets the secret.
     * 
     * @param secret the secret
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Returns the type.
     * 
     * @return permission type 
     */
    @Column(name = "type",
        nullable = false)
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     * 
     * @param type permission type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns if active.
     * 
     * @return true if active
     */
    @Column(name = "isActive",
        nullable = false)
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Sets the state.
     * 
     * @param isActive the state
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Returns the expiration date.
     * 
     * @return the expiration date
     */
    @Column(name = "expiration")
    public Date getExpiration() {
        return expiration;
    }

    /**
     * Sets the expiration date.
     * 
     * @param expiration the expiration date
     */
    public void setExpiration(Date expiration) {

        this.expiration = expiration;
    }

    /**
     * Returns the comment.
     * 
     * @return the comment
     */
    @Column(name = "comment")
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment.
     * 
     * @param comment the comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns date of creation.
     * 
     * @return date of creation
     */
    @Column(name = "created")
    public Date getCreated() {
        return created;
    }

    /**
     * Sets date of creation.
     * 
     * @param created date of creation
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * Returns name of creator.
     * 
     * @return the name
     */
    @Column(name = "createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets name of creator.
     * 
     * @param createdBy the name
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Returns date of last modification.
     * 
     * @return the date
     */
    @Column(name = "lastModified")
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets data of last modification.
     * 
     * @param lastModified the date
     */
    @Column(name = "lastModified")
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Returns name of last modifier.
     * 
     * @return the name
     */
    @Column(name = "lastModifiedBy")
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets name of last modifier.
     * 
     * @param lastModifiedBy the name
     */
    public void setLastModifiedBy(String lastModifiedBy) {
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

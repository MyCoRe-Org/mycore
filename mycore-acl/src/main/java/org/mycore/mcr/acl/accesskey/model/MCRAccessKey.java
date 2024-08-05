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
import java.util.Objects;
import java.util.UUID;

import org.mycore.datamodel.metadata.MCRObjectID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@NamedQueries({
    @NamedQuery(name = MCRAccessKeyNamedQueries.NAME_FIND_BY_REFERENCE,
        query = "SELECT k FROM MCRAccessKey k WHERE k.reference = :" + MCRAccessKeyNamedQueries.PARAM_REFERENCE
            + " ORDER BY k.lastModified ASC"),
    @NamedQuery(name = MCRAccessKeyNamedQueries.NAME_FIND_BY_UUID,
        query = "SELECT k FROM MCRAccessKey k WHERE k.uuid = :" + MCRAccessKeyNamedQueries.PARAM_UUID),
    @NamedQuery(name = MCRAccessKeyNamedQueries.NAME_FIND_BY_REFERENCE_AND_VALUE,
        query = "SELECT k FROM MCRAccessKey k WHERE k.value = :" + MCRAccessKeyNamedQueries.PARAM_VALUE
            + " AND k.reference = :" + MCRAccessKeyNamedQueries.PARAM_REFERENCE),
    @NamedQuery(name = MCRAccessKeyNamedQueries.NAME_FIND_BY_REFERENCE_AND_PERMISSION,
        query = "SELECT k FROM MCRAccessKey k  WHERE k.permission = :" + MCRAccessKeyNamedQueries.PARAM_PERMISSION
            + " AND k.reference = :" + MCRAccessKeyNamedQueries.PARAM_REFERENCE),
    @NamedQuery(name = MCRAccessKeyNamedQueries.NAME_FIND_BY_PERMISSION,
        query = "SELECT k FROM MCRAccessKey k  WHERE k.permission = :" + MCRAccessKeyNamedQueries.PARAM_PERMISSION),
    @NamedQuery(name = MCRAccessKeyNamedQueries.NAME_DELETE_BY_REFERENCE,
        query = "DELETE FROM MCRAccessKey k WHERE k.reference = :" + MCRAccessKeyNamedQueries.PARAM_REFERENCE),
    @NamedQuery(name = MCRAccessKeyNamedQueries.NAME_DELETE_ALL,
        query = "DELETE FROM MCRAccessKey k"),
    @NamedQuery(name = MCRAccessKeyNamedQueries.NAME_FIND_ALL,
        query = "SELECT k FROM MCRAccessKey k"),
})

/**
 * Access keys for a reference.
 * An access keys contains a secret and a type.
 * Value is the key secret of the key and type the permission.
 */
@Entity
@Table(name = "MCRAccessKey")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MCRAccessKey {

    /** The unique and internal information id */
    private Long id;

    /** The access key information */
    private String reference;

    /** The secret */
    private String value;

    /** The permission type */
    private String permission;

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

    private UUID uuid;

    public MCRAccessKey() {
    }

    /**
     * Creates a new access key with secret and type.
     *
     * @param secret the secret the user must know to acquire permission.
     * @param type the type of permission.
     */
    public MCRAccessKey(final String secret, final String type) {
        this();
        setValue(secret);
        setPermission(type);
    }

    /**
     * Constructs an access key with reference, permission and value.
     *
     * @param reference the reference
     * @param permission the permission
     * @param value the value
     */
    public MCRAccessKey(String reference, String permission, String value) {
        setReference(reference);
        setPermission(permission);
        setValue(value);
    }

    /**
     * @return the linked objectId
     */
    @JsonIgnore
    @Transient
    public MCRObjectID getObjectId() {
        if (reference != null) {
            return MCRObjectID.getInstance(reference);
        }
        return null;
    }

    /**
     * Returns the UUID.
     *
     * @return the UUID or null
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Sets the UUID.
     *
     * @param uuid the uuid to set
     */
    @Column(name = "uuid", nullable = false)
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Sets UUID if null.
     */
    @PrePersist
    @PreUpdate
    public void autofill() {
        if (getUuid() == null) {
            setUuid(UUID.randomUUID());
        }
    }

    /**
     * @param objectId the {@link MCRObjectID} to set
     */
    public void setObjectId(final MCRObjectID objectId) {
        reference = objectId.toString();
    }

    /**
     * Returns the reference.
     *
     * @return the reference
     */
    @JsonIgnore
    @Column(name = "objectId", nullable = false)
    public String getReference() {
        return reference;
    }

    /**
     * Sets the reference.
     *
     * @param reference the reference
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * @return internal id
     */
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accesskey_id", nullable = false)
    public Long getId() {
        return id;
    }

    /**
     * @param id internal id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the key secret
     */
    @Transient
    public String getSecret() {
        return value;
    }

    /**
     * @param secret key secret
     */
    public void setSecret(String secret) {
        value = secret;
    }

    /**
     * Returns the value.
     *
     * @return the value
     */
    @Column(name = "secret", nullable = false)
    @JsonIgnore
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return permission type
     */

    @Transient
    public String getType() {
        return permission;
    }

    /**
     * @param type permission type
     */
    public void setType(String type) {
        permission = type;
    }

    /**
     * Returns the permission.
     *
     * @return the permission
     */
    @Column(name = "type", nullable = false)
    @JsonIgnore
    public String getPermission() {
        return permission;
    }

    /**
     * Sets the permission.
     *
     * @param permission the permission
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * @return active or not
     */
    @Column(name = "isActive", nullable = false)
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
    public int hashCode() {
        return Objects.hash(id, value, reference);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MCRAccessKey other)) {
            return false;
        }
        return id == other.getId() && reference.equals(other.getReference())
            && value.equals(other.getValue());
    }

}

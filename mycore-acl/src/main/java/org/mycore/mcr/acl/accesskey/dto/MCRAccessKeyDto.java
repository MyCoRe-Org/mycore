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

package org.mycore.mcr.acl.accesskey.dto;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing an access key.
 *
 * The {@code AccessKeyDto} class represents the data structure used to transfer access key information.
 */
public class MCRAccessKeyDto {

    private UUID id;

    private String secret;

    private String reference;

    private String permission;

    private String comment;

    private Boolean active;

    private Date expiration;

    private Date created;

    private String createdBy;

    private Date lastModified;

    private String lastModifiedBy;

    /**
     * Returns the ID of the access key.
     *
     * @return the ID of the access access key
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_ID)
    public UUID getId() {
        return id;
    }

    /**
     * Sets the ID of the access key.
     *
     * @param id the ID to set for the access key
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Returns the secret associated with the access key.
     *
     * @return the secret of the access key
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_SECRET)
    public String getSecret() {
        return secret;
    }

    /**
     * Sets the secret associated with the access key.
     *
     * @param secret the secret to set for the access key
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Returns the reference associated with the access key.
     *
     * @return the reference associated with the access key
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_REFERENCE)
    public String getReference() {
        return reference;
    }

    /**
     * Sets the reference associated with the access key.
     *
     * @param reference the reference string to set for the access key
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Returns the permission associated with the access key.
     *
     * @return the permission of the access key
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_PERMISSION)
    public String getPermission() {
        return permission;
    }

    /**
     * Sets the permission associated with the access key.
     *
     * @param permission the permission to set for the access key
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * Returns the comment associated with the access key.
     *
     * @return the comment for the access key
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_COMMENT)
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment associated with the access key.
     *
     * @param comment the comment to set for the access key
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the active status of the access key.
     *
     * @return true if the access key is active, false otherwise
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_ACTIVE)
    public Boolean getActive() {
        return active;
    }

    /**
     * Sets the active status of the access key.
     *
     * @param active true to set the access key as active, false to deactivate it
     */
    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
     * Gets the expiration date of the access key.
     *
     * @return the expiration date of the access key, or null if it does not expire
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_EXPIRATION)
    public Date getExpiration() {
        return expiration;
    }

    /**
     * Sets the expiration date of the access key.
     *
     * @param expiration the expiration date to set for the access key
     */
    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    /**
     * Returns the creation date of the access key.
     *
     * @return the date when the access key was created
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_CREATED)
    public Date getCreated() {
        return created;
    }

    /**
     * Sets the creation date of the access key.
     *
     * @param created the creation date to set for the access key
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * Returns the user ID of the creator of the access key.
     *
     * @return the user ID of the creator
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_CREATED_BY)
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the user ID of the creator of the access key.
     *
     * @param createdBy the user ID to set for the creator of the access key
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Returns the last modification date of the access key.
     *
     * @return the date when the access key was last modified
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_LAST_MODIFIED)
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets the last modification date of the access key.
     *
     * @param lastModified the last modification date to set for the access key
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Returns the user ID of the last person who modified the access key.
     *
     * @return the user ID of the last modifier
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_LAST_MODIFIED_BY)
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the user ID of the last person who modified the access key.
     *
     * @param lastModifiedBy the user ID to set for the last modifier of the access key
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, comment, created, createdBy, expiration, lastModified, lastModifiedBy, reference,
            secret, permission);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MCRAccessKeyDto other = (MCRAccessKeyDto) obj;
        return Objects.equals(active, other.active) && Objects.equals(comment, other.comment)
            && Objects.equals(created, other.created) && Objects.equals(createdBy, other.createdBy)
            && Objects.equals(expiration, other.expiration) && Objects.equals(lastModified, other.lastModified)
            && Objects.equals(lastModifiedBy, other.lastModifiedBy) && Objects.equals(reference, other.reference)
            && Objects.equals(secret, other.secret) && Objects.equals(permission, other.permission);
    }

}

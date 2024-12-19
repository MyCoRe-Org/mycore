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

import org.mycore.mcr.acl.accesskey.dto.util.MCRNullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for partial updates of access keys.
 *
 * This class allows for the partial updating of various fields of an access key,
 * with each field wrapped in a {@link MCRNullable} to indicate if the field should be updated.
 */
public class MCRAccessKeyPartialUpdateDto {

    private MCRNullable<String> secret = new MCRNullable<>();

    private MCRNullable<String> permission = new MCRNullable<>();

    private MCRNullable<String> reference = new MCRNullable<>();

    private MCRNullable<Boolean> active = new MCRNullable<>();

    private MCRNullable<String> comment = new MCRNullable<>();

    private MCRNullable<Date> expiration = new MCRNullable<>();

    /**
     * Returns the secret associated with the access key.
     *
     * @return a Nullable containing the secret of the access key, or an empty Nullable if not set
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_SECRET)
    public MCRNullable<String> getSecret() {
        return secret;
    }

    /**
     * Sets the secret associated with the access key.
     *
     * @param secret a Nullable containing the new secret of the access key
     */
    public void setSecret(MCRNullable<String> secret) {
        this.secret = secret;
    }

    /**
     * Returns the permission associated with the access key.
     *
     * @return a Nullable containing the permission of the access key
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_PERMISSION)
    public MCRNullable<String> getPermission() {
        return permission;
    }

    /**
     * Sets the permission with the access key.
     *
     * @param permission a Nullable containing the permission of the access key
     */
    public void setPermission(MCRNullable<String> permission) {
        this.permission = permission;
    }

    /**
     * Returns the reference associated with the access key.
     *
     * @return a Nullable containing the reference associated with the access key
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_REFERENCE)
    public MCRNullable<String> getReference() {
        return reference;
    }

    /**
     * Sets the reference associated with the access key.
     *
     * @param reference a Nullable containing the new reference associated with the access key
     */
    public void setReference(MCRNullable<String> reference) {
        this.reference = reference;
    }

    /**
     * Returns the active status of the access key.
     *
     * @return a Nullable containing the active status of the access key
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_ACTIVE)
    public MCRNullable<Boolean> getActive() {
        return active;
    }

    /**
     * Sets the active status of the access key.
     *
     * @param active a Nullable containing the new active status of the access key
     */
    public void setActive(MCRNullable<Boolean> active) {
        this.active = active;
    }

    /**
     * Returns the comment associated with the access key.
     *
     * @return a Nullable containing the comment associated with the access key
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_COMMENT)
    public MCRNullable<String> getComment() {
        return comment;
    }

    /**
     * Sets the comment associated with the access key.
     *
     * @param comment a Nullable containing the new comment associated with the access key
     */
    public void setComment(MCRNullable<String> comment) {
        this.comment = comment;
    }

    /**
     * Returns the expiration date of the access key.
     *
     * @return a Nullable containing the expiration date of the access key
     */
    @JsonProperty(MCRAccessKeyJsonConstants.NAME_EXPIRATION)
    public MCRNullable<Date> getExpiration() {
        return expiration;
    }

    /**
     * Sets the expiration date of the access key.
     *
     * @param expiration a Nullable containing the new expiration date of the access key
     */
    public void setExpiration(MCRNullable<Date> expiration) {
        this.expiration = expiration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(active, comment, expiration, reference, permission, secret);
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
        MCRAccessKeyPartialUpdateDto other = (MCRAccessKeyPartialUpdateDto) obj;
        return Objects.equals(active, other.active) && Objects.equals(comment, other.comment)
            && Objects.equals(expiration, other.expiration) && Objects.equals(reference, other.reference)
            && Objects.equals(permission, other.permission) && Objects.equals(secret, other.secret);
    }
}

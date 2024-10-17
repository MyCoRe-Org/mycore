package org.mycore.mcr.acl.accesskey.restapi.v2.dto;

import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * DTO representing an access key, used for compatibility with an outdated REST API.
 */
public class MCRRestAccessKeyDto {

    private String secret;

    private String type;

    private Boolean isActive;

    private Date expiration;

    private String comment;

    private Date created;

    private String createdBy;

    private Date lastModified;

    private String lastModifiedBy;

    /**
     * Returns the secret.
     *
     * @return the secret
     */
    @JsonProperty("secret")
    public String getSecret() {
        return secret;
    }

    /**
     * Sets the secret.
     *
     * @param secret the secret to set
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Returns the type.
     *
     * @return the type
     */
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the state.
     *
     * @return the isActive
     */
    @JsonProperty("isActive")
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Sets the state.
     *
     * @param isActive the isActive to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Return the date of expiration.
     *
     * @return the expiration
     */
    @JsonProperty("expiration")
    public Date getExpiration() {
        return expiration;
    }

    /**
     * Sets the date of expiration.
     *
     * @param expiration the expiration to set
     */
    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    /**
     * Returns the comment
     *
     * @return the comment
     */
    @JsonProperty("comment")
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment.
     *
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the creation date.
     *
     * @return the created
     */
    @JsonProperty("created")
    public Date getCreated() {
        return created;
    }

    /**
     * Sets the creation date.
     *
     * @param created the created to set
     */
    public void setCreated(Date created) {
        this.created = created;
    }

    /**
     * Returns the user who created this entity.
     *
     * @return the createdBy
     */
    @JsonProperty("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Sets the user who created this entity.
     *
     * @param createdBy the createdBy to set
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Returns the last modification date.
     *
     * @return the lastModified
     */
    @JsonProperty("lastModified")
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Sets the last modification date.
     *
     * @param lastModified the lastModified to set
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Returns the user who last modified this entity.
     *
     * @return the lastModifiedBy
     */
    @JsonProperty("lastModifiedBy")
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * Sets the user who last modified this entity.
     *
     * @param lastModifiedBy the lastModifiedBy to set
     */
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment, created, createdBy, expiration, isActive, lastModified, lastModifiedBy, secret,
            type);
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
        MCRRestAccessKeyDto other = (MCRRestAccessKeyDto) obj;
        return Objects.equals(comment, other.comment) && Objects.equals(created, other.created)
            && Objects.equals(createdBy, other.createdBy) && Objects.equals(expiration, other.expiration)
            && Objects.equals(isActive, other.isActive) && Objects.equals(lastModified, other.lastModified)
            && Objects.equals(lastModifiedBy, other.lastModifiedBy) && Objects.equals(secret, other.secret)
            && Objects.equals(type, other.type);
    }
}

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@NamedQueries({
    @NamedQuery(name = "MCRAccessKey.getWithObjectId",
        query = "SELECT k"
            + "  FROM MCRAccessKey k"
            + "  WHERE k.objectId = :objectId"
            + "  ORDER BY k.lastChange ASC"),
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

    /** The creator */
    private String creator;

    /** The creation date */
    private Date creation;

    /** Last modified by */
    private String lastChanger;

    /** Last modified date */
    private Date lastChange;

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
     * @return name of creator
     */
    @Column(name = "creator")
    public String getCreator() {
        return creator;
    }

    /**
     * @param creator name of creator
     */
    public void setCreator(final String creator) {
        this.creator = creator;
    }
    
    /**
     * @return date of creation
     */
    @Column(name = "creation")
    public Date getCreation() {
        return creation;
    }

    /**
     * @param creation date of creation
     */
    public void setCreation(final Date creation) {
        this.creation = creation;
    }

    /**
     * @return name of last changer
     */
    @Column(name = "lastChanger")
    public String getLastChanger() {
        return lastChanger;
    }

    /**
     * @param lastChanger name of modifier
     */
    public void setLastChanger(final String lastChanger) {
        this.lastChanger = lastChanger;
    }
    
    /**
     * @return last date of change
     */
    @Column(name = "lastChange")
    public Date getLastChange() {
        return lastChange;
    }

    /**
     * @param lastChange last date of change
     */
    public void setLastChange(final Date lastChange) {
        this.lastChange = lastChange;
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

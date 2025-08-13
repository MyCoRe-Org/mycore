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

package org.mycore.frontend.support;

import java.time.Instant;

import org.mycore.common.xml.adapters.MCRInstantXMLAdapter;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Represents a lock on a MyCoRe object.
 * <p>
 * This class serves as a data transfer object (DTO) for both internal lock management and for
 * serialization to XML/JSON in REST API responses. It holds all state information for a lock,
 * including who owns it, when it was acquired, and its duration.
 * <p>
 * The actual expiration time of the lock is calculated as {@code updated} + {@code timeout}.
 */
@XmlRootElement(name = "lock")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MCRObjectLock {

    /**
     * The internal identifier of the lock, often tied to a user session or another internal mechanism.
     * This field is for internal use only and is marked as {@link XmlTransient} to prevent it from
     * being serialized in API responses.
     */
    @XmlTransient
    protected String id;

    /**
     * Flag indicating whether the object is currently considered locked.
     */
    @XmlAttribute
    protected boolean locked;

    /**
     * The identifier of the user who acquired the lock.
     */
    @XmlAttribute
    protected String createdBy;

    /**
     * The timestamp when the lock was initially created.
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(MCRInstantXMLAdapter.class)
    protected Instant created;

    /**
     * The timestamp when the lock was last updated. Upon creation, this value is the same as
     * {@link #created}. It is refreshed when the lock's timeout is extended.
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(MCRInstantXMLAdapter.class)
    protected Instant updated;

    /**
     * The duration of the lock in milliseconds, starting from the {@link #updated} timestamp.
     * A value of {@code null} indicates an infinite timeout where the lock does not expire automatically.
     */
    @XmlAttribute
    protected Integer timeout;

    /**
     * Sets the internal identifier for the lock.
     *
     * @param id the internal lock identifier
     * @return this {@code MCRObjectLock} instance
     */
    public MCRObjectLock setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the locked status.
     *
     * @param locked true if the object is locked, false otherwise
     * @return this {@code MCRObjectLock} instance
     */
    public MCRObjectLock setLocked(boolean locked) {
        this.locked = locked;
        return this;
    }

    /**
     * Sets the user who created the lock.
     *
     * @param createdBy the user's identifier
     * @return this {@code MCRObjectLock} instance
     */
    public MCRObjectLock setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    /**
     * Sets the creation timestamp of the lock.
     *
     * @param created the creation timestamp
     * @return this {@code MCRObjectLock} instance
     */
    public MCRObjectLock setCreated(Instant created) {
        this.created = created;
        return this;
    }

    /**
     * Sets the last updated timestamp of the lock.
     *
     * @param updated the last updated timestamp
     * @return this {@code MCRObjectLock} instance
     */
    public MCRObjectLock setUpdated(Instant updated) {
        this.updated = updated;
        return this;
    }

    /**
     * Sets the lock timeout duration.
     *
     * @param timeout the duration in milliseconds; {@code null} for infinite
     * @return this {@code MCRObjectLock} instance
     */
    public MCRObjectLock setTimeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Gets the internal identifier of the lock.
     *
     * @return the internal lock ID
     */
    public String getId() {
        return id;
    }

    /**
     * Checks if the object is locked.
     *
     * @return true if locked, false otherwise
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Gets the identifier of the user who created the lock.
     *
     * @return the creator's user ID
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return the creation timestamp
     */
    public Instant getCreated() {
        return created;
    }

    /**
     * Gets the last updated timestamp.
     *
     * @return the last updated timestamp
     */
    public Instant getUpdated() {
        return updated;
    }

    /**
     * Gets the timeout duration in milliseconds.
     *
     * @return the timeout in milliseconds, or {@code null} for infinite
     */
    public Integer getTimeout() {
        return timeout;
    }

    /**
     * Returns the expiration timestamp.
     *
     * @return the expiration timestamp.
     */
    @XmlAttribute(name = "expires")
    @XmlJavaTypeAdapter(MCRInstantXMLAdapter.class)
    public Instant getExpirationTime() {
        if (timeout == null) {
            return null;
        }
        return updated.plusMillis(timeout);
    }

    /**
     * A factory method to create a new, fully populated lock object.
     * The token is set to the same value as the internal id.
     *
     * @param id      The internal identifier for the lock, often a session ID.
     * @param user    The user ID of the person acquiring the lock.
     * @param timeout The lock timeout in milliseconds.
     * @return A new {@code MCRObjectLock} instance, initialized with the current time for created/updated timestamps.
     */
    public static MCRObjectLock createLock(String id, String user, Integer timeout) {
        Instant now = Instant.now();
        return new MCRObjectLock()
            .setId(id)
            .setLocked(true)
            .setCreatedBy(user)
            .setCreated(now)
            .setUpdated(now)
            .setTimeout(timeout);
    }

}

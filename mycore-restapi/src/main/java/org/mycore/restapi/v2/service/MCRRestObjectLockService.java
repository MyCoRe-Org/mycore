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

package org.mycore.restapi.v2.service;

import static org.mycore.restapi.v2.MCRRestAuthorizationFilter.PARAM_MCRID;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.support.MCRObjectLock;

import jakarta.ws.rs.PathParam;

/**
 * Service providing lock operations for MyCoRe objects.
 */
public interface MCRRestObjectLockService {

    /**
     * Returns the current lock state of the object.
     *
     * @param id the object ID
     * @return an unlocked placeholder if no lock exists; otherwise the lock metadata
     * @throws jakarta.ws.rs.WebApplicationException 404 if the object does not exist
     */
    MCRObjectLock getLock(@PathParam(PARAM_MCRID) MCRObjectID id);

    /**
     * Creates a new lock for the current user.
     *
     * @param id          the object ID
     * @param requestBody lock request; the timeout is taken from {@code requestBody.getTimeout()},
     *                    defaulting to 60 seconds if not provided
     * @return the created lock
     * @throws jakarta.ws.rs.WebApplicationException 404 if the object does not exist;
     *                                               409 if the object is already locked
     */
    MCRObjectLock setLock(@PathParam(PARAM_MCRID) MCRObjectID id, MCRObjectLock requestBody);

    /**
     * Updates the existing lock's timeout or creates a new lock if none exists.
     *
     * @param id          the object ID
     * @param requestBody lock request containing the new timeout (must not be {@code null})
     * @return the updated or newly created lock
     * @throws jakarta.ws.rs.WebApplicationException 404 if the object does not exist;
     *                                               409 if a lock exists but is owned by another user
     */
    MCRObjectLock updateLock(@PathParam(PARAM_MCRID) MCRObjectID id, MCRObjectLock requestBody);

    /**
     * Removes the lock if it is owned by the current user.
     *
     * @param id the object ID
     * @return an unlocked placeholder
     * @throws jakarta.ws.rs.WebApplicationException 404 if the object or lock does not exist;
     *                                               409 if the lock is owned by another user
     */
    MCRObjectLock deleteLock(@PathParam(PARAM_MCRID) MCRObjectID id);

}

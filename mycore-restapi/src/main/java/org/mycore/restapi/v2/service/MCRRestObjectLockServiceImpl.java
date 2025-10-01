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

import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.support.MCRObjectIDLockTable;
import org.mycore.frontend.support.MCRObjectLock;
import org.mycore.restapi.v2.MCRErrorCodeConstants;
import org.mycore.restapi.v2.MCRErrorResponse;
import org.mycore.restapi.v2.MCRRestUtils;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/**
 * Default service implementation providing lock operations for MyCoRe objects.
 * <p>
 * This is a pure service (no JAX-RS endpoints). It throws {@code jakarta.ws.rs.WebApplicationException} instances
 * for error cases.
 * <p>
 * Ownership is determined by the current user from {@link MCRSessionMgr#getCurrentSession()}.
 */
public class MCRRestObjectLockServiceImpl implements MCRRestObjectLockService {

    /**
     * {@inheritDoc}
     */
    @Override
    public MCRObjectLock getLock(@PathParam(PARAM_MCRID) MCRObjectID id) {
        MCRRestUtils.checkExists(id);
        MCRObjectLock lock = MCRObjectIDLockTable.getLock(id);
        if (lock == null) {
            return new MCRObjectLock().setLocked(false);
        }
        return new MCRObjectLock()
            .setLocked(true)
            .setTimeout(lock.getTimeout())
            .setCreatedBy(lock.getCreatedBy())
            .setCreated(lock.getCreated())
            .setUpdated(lock.getUpdated());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCRObjectLock setLock(@PathParam(PARAM_MCRID) MCRObjectID id, MCRObjectLock requestBody) {
        MCRRestUtils.checkExists(id);
        if (MCRObjectIDLockTable.isLocked(id)) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.CONFLICT.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_ALREADY_LOCKED)
                .withMessage("The object " + id + " is already locked.")
                .toException();
        }
        return createLock(id, requestBody);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCRObjectLock updateLock(@PathParam(PARAM_MCRID) MCRObjectID id, MCRObjectLock requestBody) {
        MCRRestUtils.checkExists(id);
        MCRObjectLock lock = MCRObjectIDLockTable.getLock(id);
        // RFC5789: If the Request-URI does not point to an existing resource, the server MAY create a new resource.
        if (lock == null) {
            return createLock(id, requestBody);
        }
        // check id
        String lockId = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        checkLockIdsAreEqual(lockId, lock.getId());
        // update the lock object
        return MCRObjectIDLockTable.updateLock(id, requestBody != null ? requestBody.getTimeout() : null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCRObjectLock deleteLock(@PathParam(PARAM_MCRID) MCRObjectID id) {
        MCRRestUtils.checkExists(id);
        MCRObjectLock lock = MCRObjectIDLockTable.getLock(id);
        // lock not found
        if (lock == null) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.NOT_FOUND.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_INVALID_STATE)
                .withMessage("No lock for object " + id + " found.")
                .toException();
        }
        // check ids
        String lockId = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        checkLockIdsAreEqual(lockId, lock.getId());
        // delete
        MCRObjectIDLockTable.unlock(id);
        return new MCRObjectLock().setLocked(false);
    }

    /**
     * Creates a lock for the current user using the given timeout.
     * If the timeout is not provided, 60 seconds are used.
     *
     * @param id          the object ID
     * @param requestBody lock request (maybe {@code null})
     * @return the created lock
     */
    private static MCRObjectLock createLock(MCRObjectID id, MCRObjectLock requestBody) {
        String userId = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        int timeout = (requestBody != null && requestBody.getTimeout() != null) ? requestBody.getTimeout() : 1000 * 60;
        return MCRObjectIDLockTable.lock(id, userId, userId, timeout);
    }

    /**
     * Ensures the provided user id matches the lock owner.
     *
     * @param id1 the current user's ID
     * @param id2 the lock owner's ID
     * @throws jakarta.ws.rs.WebApplicationException 409 if the IDs do not match
     */
    private static void checkLockIdsAreEqual(String id1, String id2) {
        if (!id1.equals(id2)) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.CONFLICT.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_LOCKED_BY_DIFFERENT_USER)
                .withMessage("The provided id does not match the active lock.")
                .toException();
        }
    }

}

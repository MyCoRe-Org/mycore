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
import org.mycore.common.config.MCRConfiguration2;
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
 * <p>
 * Configuration:
 * <ul>
 *     <li>{@code MCR.RestAPI.V2.ObjectLock.Timeout.Default}: timeout in milliseconds of a new lock, if the request
 *         does not provide one</li>
 *     <li>{@code MCR.RestAPI.V2.ObjectLock.Timeout.Max}: maximum timeout in milliseconds, larger timeouts are
 *         reduced to it</li>
 * </ul>
 */
public class MCRRestObjectLockServiceImpl implements MCRRestObjectLockService {

    private static final int TIMEOUT_DEFAULT = MCRConfiguration2
        .getInt("MCR.RestAPI.V2.ObjectLock.Timeout.Default")
        .orElseThrow();

    private static final int TIMEOUT_MAX = MCRConfiguration2.getInt("MCR.RestAPI.V2.ObjectLock.Timeout.Max")
        .orElseThrow();

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
    public MCRObjectLock putLock(@PathParam(PARAM_MCRID) MCRObjectID id, MCRObjectLock requestBody) {
        MCRRestUtils.checkExists(id);
        Integer timeout = getRequestedTimeout(requestBody);
        MCRObjectLock lock = MCRObjectIDLockTable.getLock(id);
        if (lock == null) {
            return createLock(id, timeout);
        }
        // check id
        String lockId = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        checkLockIdsAreEqual(lockId, lock.getId());
        // update the lock object
        return MCRObjectIDLockTable.updateLock(id, timeout);
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
     * If the timeout is not provided, {@code MCR.RestAPI.V2.ObjectLock.Timeout.Default} is used.
     *
     * @param id      the object ID
     * @param timeout the timeout in milliseconds (maybe {@code null})
     * @return the created lock
     */
    private static MCRObjectLock createLock(MCRObjectID id, Integer timeout) {
        String userId = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        int lockTimeout = timeout != null ? timeout : Math.min(TIMEOUT_DEFAULT, TIMEOUT_MAX);
        return MCRObjectIDLockTable.lock(id, userId, userId, lockTimeout);
    }

    /**
     * Returns the timeout of the lock request, reduced to {@code MCR.RestAPI.V2.ObjectLock.Timeout.Max}.
     *
     * @param requestBody lock request (maybe {@code null})
     * @return the timeout in milliseconds, or {@code null} if the request does not provide one
     * @throws jakarta.ws.rs.WebApplicationException 400 if the timeout is not positive
     */
    static Integer getRequestedTimeout(MCRObjectLock requestBody) {
        Integer timeout = requestBody != null ? requestBody.getTimeout() : null;
        if (timeout == null) {
            return null;
        }
        if (timeout <= 0) {
            throw MCRErrorResponse.ofStatusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .withErrorCode(MCRErrorCodeConstants.MCROBJECT_INVALID_LOCK_TIMEOUT)
                .withMessage("The timeout of a lock must be positive.")
                .toException();
        }
        return Math.min(timeout, TIMEOUT_MAX);
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

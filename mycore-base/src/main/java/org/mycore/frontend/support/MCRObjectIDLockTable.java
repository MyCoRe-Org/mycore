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

package org.mycore.frontend.support;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * A thread-safe, in-memory service for managing exclusive locks on MyCoRe objects ({@link MCRObjectID}).
 * <p>
 * This class uses a singleton pattern to provide a central, application-wide lock table.
 * It is designed to be robust against common locking issues:
 * <ul>
 *     <li><b>Automatic Timeouts:</b> Locks expire automatically after a specified duration, preventing
 *         indefinite locks from crashed or misbehaving clients.</li>
 *     <li><b>Automatic Session Cleanup:</b> As a {@link MCRSessionListener}, it automatically
 *         clears all locks held by a user when their session is destroyed.</li>
 *     <li><b>Thread-Safety:</b> Uses a {@link ConcurrentMap} to ensure safe concurrent access
 *         from multiple threads.</li>
 * </ul>
 */
public final class MCRObjectIDLockTable implements MCRSessionListener {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ConcurrentMap<MCRObjectID, MCRObjectLock> lockMap;

    /**
     * Private constructor to enforce the singleton pattern.
     * Initializes the underlying map and registers this instance as a session listener
     * to handle lock cleanup on session destruction.
     */
    private MCRObjectIDLockTable() {
        this.lockMap = new ConcurrentHashMap<>();
        MCRSessionMgr.addSessionListener(this);
    }

    private static MCRObjectIDLockTable getInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    /**
     * Removes all locks associated with a given session. This is typically called
     * automatically when a session is destroyed.
     *
     * @param session The session whose locks should be cleared.
     */
    public void clearTable(MCRSession session) {
        String sessionId = session.getID();
        List<MCRObjectID> toRemoveList = lockMap.entrySet().stream()
            .filter(entry -> entry.getValue().id.equals(sessionId))
            .map(ConcurrentMap.Entry::getKey)
            .toList();
        for (MCRObjectID objectId : toRemoveList) {
            lockMap.remove(objectId);
        }
    }

    /**
     * Removes all locks.
     */
    public static void clear() {
        getInstance().lockMap.clear();
    }

    /**
     * Immediately removes the lock for a given object ID, regardless of who owns it.
     *
     * @param objectId The ID of the object to unlock.
     * @return The lock object that was removed, or {@code null} if no lock existed.
     */
    public static MCRObjectLock unlock(MCRObjectID objectId) {
        return getInstance().lockMap.remove(objectId);
    }

    /**
     * Attempts to acquire a lock for the given object ID using the current session's information.
     *
     * @param objectId The ID of the object to lock.
     * @return The new {@link MCRObjectLock} if the lock was acquired successfully, or the
     * existing lock if the object was already locked.
     */
    public static MCRObjectLock lock(MCRObjectID objectId) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String sessionId = session.getID();
        String userName = session.getUserInformation().getUserID();
        return lock(objectId, sessionId, userName, null);
    }

    /**
     * Attempts to acquire a lock for the given object ID with explicit owner and timeout details.
     * <p>
     * This is an atomic "compute-if-absent" operation. If the object is not locked, a new lock is created
     * and a delayed unlock task is scheduled.
     *
     * @param objectId The ID of the object to lock.
     * @param lockId   The internal ID for the lock (e.g., a session ID).
     * @param userName The user acquiring the lock.
     * @param timeout  The lock duration in milliseconds. If null, the lock may be infinite.
     * @return         The new {@link MCRObjectLock} if the lock was acquired, or the existing lock if the object was
     *                 already locked.
     */
    public static MCRObjectLock lock(MCRObjectID objectId, String lockId, String userName, Integer timeout) {
        return getInstance().lockMap.computeIfAbsent(objectId, (id) -> {
            MCRObjectLock objectLock = MCRObjectLock.createLock(lockId, userName, timeout);
            delayedUnlock(objectId, objectLock);
            return objectLock;
        });
    }

    /**
     * Same as {@link #updateLock(MCRObjectID, Integer)}. Only refreshes the lock. The timeout remains the same.
     *
     * @param objectId The ID of the locked object.
     * @return The updated lock object, or {@code null} if no lock exists for the given ID.
     */
    public static MCRObjectLock updateLock(MCRObjectID objectId) {
        return updateLock(objectId, null);
    }

    /**
     * Updates an existing lock, typically to extend its duration.
     * <p>
     * This method updates the lock's 'updated' timestamp to the current time. If a non-null
     * {@code timeout} parameter is provided, the lock's duration is changed. If the {@code timeout}
     * parameter is null, the lock's existing duration is simply refreshed from the new 'updated' time.
     * <p>
     * After updating, if the lock has a finite timeout (i.e., its timeout field is not null),
     * a new delayed unlock task is scheduled to ensure it expires correctly. This correctly
     * handles both changing the duration and refreshing the existing duration.
     *
     * @param objectId The ID of the locked object.
     * @param timeout  The new timeout duration in milliseconds. If null, the existing timeout duration is used
     *                 for the refresh.
     * @return The updated lock object, or {@code null} if no lock exists for the given ID.
     */
    public static MCRObjectLock updateLock(MCRObjectID objectId, Integer timeout) {
        MCRObjectLock objectLock = getLock(objectId);
        if (objectLock == null) {
            return null;
        }
        objectLock.setUpdated(LocalDateTime.now());
        if (timeout != null) {
            objectLock.setTimeout(timeout);
        }
        if (objectLock.timeout != null) {
            delayedUnlock(objectId, objectLock);
        }
        return objectLock;
    }

    /**
     * Checks if a lock exists for the given object ID.
     *
     * @param objectId The ID of the object to check.
     * @return {@code true} if the object is locked, {@code false} otherwise.
     */
    public static boolean isLocked(MCRObjectID objectId) {
        return getInstance().lockMap.containsKey(objectId);
    }

    /**
     * Checks if the given object is locked by the current user's session.
     *
     * @param objectId The ID of the object to check.
     * @return {@code true} if the object is locked and the lock's ID matches the current session ID.
     */
    public static boolean isLockedByCurrentSession(MCRObjectID objectId) {
        String sessionId = MCRSessionMgr.getCurrentSession().getID();
        MCRObjectLock objectLock = getInstance().lockMap.get(objectId);
        return objectLock != null && sessionId.equals(objectLock.id);
    }

    /**
     * Retrieves the lock information for a given object ID.
     *
     * @param objectId The ID of the object.
     * @return The {@link MCRObjectLock} object, or {@code null} if no lock exists.
     */
    public static MCRObjectLock getLock(MCRObjectID objectId) {
        return getInstance().lockMap.get(objectId);
    }

    /**
     * Schedules a task to automatically unlock an object after its timeout has passed.
     * <p>
     * This method contains a re-check. If the lock is updated before the timeout expires,
     * this original scheduled task will execute but will see that the lock's expiration time is now
     * in the future, and it will do nothing. This prevents premature unlocking.
     *
     * @param objectId   The ID of the object to unlock.
     * @param objectLock The lock object containing timeout information.
     */
    private static void delayedUnlock(final MCRObjectID objectId, final MCRObjectLock objectLock) {
        if (objectLock == null || objectLock.timeout == null) {
            return;
        }
        CompletableFuture.delayedExecutor(objectLock.timeout, TimeUnit.MILLISECONDS).execute(() -> {
            // Re-fetch the lock from the map to ensure we have the most current state.
            MCRObjectLock currentLock = getLock(objectId);
            if (currentLock == null) {
                // Lock was already removed by some other means.
                return;
            }
            // A lock is only unlocked if its original token matches the one we are checking.
            // This prevents a new, unrelated lock on the same objectId from being prematurely removed.
            if (!currentLock.getToken().equals(objectLock.getToken())) {
                return;
            }
            // Check if expiration time is reached
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime unlockTime = currentLock.getExpirationTime();
            if (now.isBefore(unlockTime)) {
                return;
            }
            // Atomically remove the lock only if it is the exact same lock object.
            getInstance().lockMap.remove(objectId, currentLock);
        });
    }

    /**
     * Retrieves the session of the user who is holding the lock on the object.
     *
     * @param objectId The ID of the locked object.
     * @return The {@link MCRSession} of the lock owner, or {@code null} if the object is not locked
     * or the session is no longer active.
     */
    public static MCRSession getLocker(MCRObjectID objectId) {
        MCRObjectLock objectLock = getInstance().lockMap.get(objectId);
        if (objectLock == null) {
            return null;
        }
        return MCRSessionMgr.getSession(objectLock.id);
    }

    /**
     * Handles session events. When a session is destroyed, it triggers {@link #clearTable(MCRSession)}
     * to release all locks held by that session.
     *
     * @param event The session event.
     */
    @Override
    public void sessionEvent(MCRSessionEvent event) {
        switch (event.getType()) {
            case DESTROYED -> clearTable(event.getSession());
            default -> LOGGER.debug("Skipping event: {}", event.getType());
        }
    }

    /**
     * Convenience method to check if an object is locked by the current session, using a String object ID.
     *
     * @param objectId The string representation of the object ID.
     * @return {@code true} if the object is locked by the current session.
     * @see #isLockedByCurrentSession(MCRObjectID)
     */
    public static boolean isLockedByCurrentSession(String objectId) {
        MCRObjectID objId = MCRObjectID.getInstance(objectId);
        return isLockedByCurrentSession(objId);
    }

    /**
     * Convenience method to get the username of the user locking an object.
     *
     * @param objectId The string representation of the object ID.
     * @return The username of the lock owner, or {@code null} if the object is not locked.
     */
    public static String getLockingUserName(String objectId) {
        MCRObjectID objId = MCRObjectID.getInstance(objectId);
        MCRObjectLock objectLock = getLock(objId);
        return objectLock != null ? objectLock.createdBy : null;
    }

    private static final class LazyInstanceHolder {
        public static final MCRObjectIDLockTable SINGLETON_INSTANCE = new MCRObjectIDLockTable();
    }

}

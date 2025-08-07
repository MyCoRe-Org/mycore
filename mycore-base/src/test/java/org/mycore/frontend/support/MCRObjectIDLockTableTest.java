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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRUserInformation;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@DisplayName("MCRObjectIDLockTable Core Logic Test")
@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "MCR.Metadata.Type.object", string = "true")
    })
public class MCRObjectIDLockTableTest {

    private static final String USER_A = "userA";

    private static final String USER_B = "userB";

    private MCRObjectID testId;

    @BeforeEach
    public void setUp() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.setUserInformation(new MCRUserInformation() {
            @Override
            public String getUserID() {
                return USER_A;
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public String getUserAttribute(String attribute) {
                return "";
            }
        });
        testId = MCRObjectID.getInstance("junit_object_00000001");
    }

    @AfterEach
    public void tearDown() {
        MCRObjectIDLockTable.clear();
    }

    @Test
    void testLockAndGetLock() {
        // when
        MCRObjectIDLockTable.lock(testId);

        // then
        assertTrue(MCRObjectIDLockTable.isLocked(testId), "Object should be locked");
        MCRObjectLock lock = MCRObjectIDLockTable.getLock(testId);
        assertNotNull(lock, "getLock should return a non-null object");
        assertTrue(lock.isLocked(), "Lock object should report as locked");
        assertEquals(USER_A, lock.getCreatedBy(), "Lock should be created by the mocked user");
    }

    @Test
    void testUnlock() {
        // given
        MCRObjectIDLockTable.lock(testId);
        assertTrue(MCRObjectIDLockTable.isLocked(testId), "Precondition: Object must be locked");

        // when
        MCRObjectIDLockTable.unlock(testId);

        // then
        assertFalse(MCRObjectIDLockTable.isLocked(testId), "Object should be unlocked");
        assertNull(MCRObjectIDLockTable.getLock(testId), "getLock should return null for an unlocked object");
    }

    @Test
    void testLockAlreadyLockedReturnsExistingLock() {
        // given
        MCRObjectLock firstLock = MCRObjectIDLockTable.lock(testId, "id_A", USER_A, 10);

        // when
        // A different user attempts to lock the same object
        MCRObjectLock secondAttempt = MCRObjectIDLockTable.lock(testId, "id_B", USER_B, 10);

        // then
        assertNotNull(secondAttempt, "Second lock attempt should not return null");
        assertSame(firstLock, secondAttempt, "Should return the instance of the first lock");
        assertEquals(USER_A, secondAttempt.getCreatedBy(), "The lock should still be owned by USER_A");
    }

    @Test
    @DisplayName("Automatic unlock after timeout should release the lock")
    void delayedUnlockLockExpires() throws InterruptedException {
        // given
        final int timeoutMs = 10;

        // when
        MCRObjectIDLockTable.lock(testId, "id", USER_A, timeoutMs);
        assertTrue(MCRObjectIDLockTable.isLocked(testId), "Object should be locked immediately after locking");

        // then
        Thread.sleep(20);
        assertFalse(MCRObjectIDLockTable.isLocked(testId), "Object should be unlocked after timeout");
    }

    @Test
    @DisplayName("updateLock should refresh the timeout")
    void updateLockRefreshesTimeout() throws InterruptedException {
        // given
        final int timeoutMs = 40;
        MCRObjectIDLockTable.lock(testId, "id", USER_A, timeoutMs);

        // when
        // Wait for a bit, but less than the timeout
        Thread.sleep(20);

        // Refresh the lock for another 40 ms
        MCRObjectLock updatedLock = MCRObjectIDLockTable.updateLock(testId);
        assertNotNull(updatedLock);

        // then
        // Wait past the *original* expiry time
        Thread.sleep(25);
        assertTrue(MCRObjectIDLockTable.isLocked(testId), "Lock should still be held after original expiry time");

        // Now wait for the refreshed lock to expire
        Thread.sleep(25);
        assertFalse(MCRObjectIDLockTable.isLocked(testId),
            "Lock should be released after the refreshed timeout expires");
    }

    @Test
    @DisplayName("A stale delayedUnlock task from a previous lock must not remove a new lock")
    void delayedUnlockStaleTaskDoesNotRemoveNewLock() throws InterruptedException {
        // given: Create a "zombie" task
        final int shortTimeoutMs = 20;
        MCRObjectIDLockTable.lock(testId, "tokenA", USER_A, shortTimeoutMs);

        // when: Manually unlock and then immediately re-lock by another user
        MCRObjectIDLockTable.unlock(testId);
        MCRObjectIDLockTable.lock(testId, "tokenB", USER_B, 40);

        // then: Wait for the first "zombie" task to fire
        Thread.sleep(30); // Wait past the first timeout

        // CRITICAL ASSERTION: The lock should NOT be removed
        assertTrue(MCRObjectIDLockTable.isLocked(testId), "The new lock must not be removed by the stale task");
        MCRObjectLock currentLock = MCRObjectIDLockTable.getLock(testId);
        assertEquals(USER_B, currentLock.getCreatedBy(), "The lock should still be owned by USER_B");
    }

}

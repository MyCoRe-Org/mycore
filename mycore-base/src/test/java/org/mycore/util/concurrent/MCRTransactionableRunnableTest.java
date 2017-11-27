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

package org.mycore.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;

public class MCRTransactionableRunnableTest extends MCRTestCase {

    private static Logger LOGGER = LogManager.getLogger();

    @Test
    public void run() {
        // with session
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String sessionID = session.getID();
        TestRunnable testRunnable = new TestRunnable();
        MCRTransactionableRunnable transactionableRunnable = new MCRTransactionableRunnable(testRunnable, session);
        transactionableRunnable.run();
        assertTrue(testRunnable.isExecuted());
        assertEquals(sessionID, testRunnable.getSessionContextID());
        session.close();

        // without session
        transactionableRunnable = new MCRTransactionableRunnable(testRunnable);
        transactionableRunnable.run();
        assertTrue(testRunnable.isExecuted());
        assertNotEquals(sessionID, testRunnable.getSessionContextID());
        assertFalse(MCRSessionMgr.hasCurrentSession());
    }

    private class TestRunnable implements Runnable {

        private boolean executed;

        private String sessionContextID;

        @Override
        public void run() {
            try {
                Thread.sleep(100);
                sessionContextID = MCRSessionMgr.getCurrentSession().getID();
                executed = true;
                LOGGER.info("thread executed");
            } catch (InterruptedException e) {
                LOGGER.error("thread interrupted", e);
            }
        }

        public String getSessionContextID() {
            return sessionContextID;
        }

        public boolean isExecuted() {
            return executed;
        }

    }

}

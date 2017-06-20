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

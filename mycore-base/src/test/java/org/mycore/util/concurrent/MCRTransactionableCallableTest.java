package org.mycore.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;

public class MCRTransactionableCallableTest extends MCRTestCase {

    private static Logger LOGGER = LogManager.getLogger();

    @Test
    public void run() {
        // with session
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String sessionID = session.getID();
        TestCallable testCallable = new TestCallable();
        MCRTransactionableCallable<Boolean> transactionableRunnable = new MCRTransactionableCallable<Boolean>(
            testCallable, session);
        try {
            assertTrue(transactionableRunnable.call());
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals(sessionID, testCallable.getSessionContextID());
        session.close();

        // without session
        transactionableRunnable = new MCRTransactionableCallable<Boolean>(testCallable);
        try {
            assertTrue(transactionableRunnable.call());
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotEquals(sessionID, testCallable.getSessionContextID());
        assertFalse(MCRSessionMgr.hasCurrentSession());
    }

    private class TestCallable implements Callable<Boolean> {

        private String sessionContextID;

        @Override
        public Boolean call() throws Exception {
            Thread.sleep(100);
            sessionContextID = MCRSessionMgr.getCurrentSession().getID();
            LOGGER.info("thread executed");
            return true;
        }

        public String getSessionContextID() {
            return sessionContextID;
        }

    }

}

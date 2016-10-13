package org.mycore.util.concurrent;

import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestCase;

public class MCRTransactionableRunnableTest extends MCRTestCase {

    private static Logger LOGGER = LogManager.getLogger();

    private static boolean EXECUTED = false;

    @Test
    public void run() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRTransactionableRunnable transactionableRunnable = new MCRTransactionableRunnable(new TestRunnable(),
            session);
        transactionableRunnable.run();
        assertTrue(EXECUTED);
    }

    private class TestRunnable implements Runnable {

        @Override
        public void run() {
            try {
                Thread.sleep(100);
                EXECUTED = true;
                LOGGER.info("thread executed");
            } catch (InterruptedException e) {
                LOGGER.error("thread interrupted", e);
            }
        }

    }

}

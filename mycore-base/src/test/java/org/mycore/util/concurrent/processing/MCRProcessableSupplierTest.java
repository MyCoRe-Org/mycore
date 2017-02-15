package org.mycore.util.concurrent.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.processing.MCRAbstractProgressable;
import org.mycore.common.processing.MCRProcessable;
import org.mycore.common.processing.MCRProcessableStatus;
import org.mycore.common.processing.MCRProcessableStatusListener;
import org.mycore.common.processing.MCRProgressable;
import org.mycore.common.processing.MCRProgressableListener;

public class MCRProcessableSupplierTest extends MCRTestCase {

    private static Logger LOGGER = LogManager.getLogger();

    private static int PROGRESS_LISTENER_COUNTER = 0;

    private static int STATUS_LISTENER_COUNTER = 0;

    @Test
    public void submit() throws Exception {
        ExecutorService es = Executors.newSingleThreadExecutor();
        MCRProcessableExecutor pes = MCRProcessableFactory.newPool(es);

        MCRProcessableSupplier<?> supplier = pes.submit(new TestRunnable(), 0);

        // STATUS LISTENER
        STATUS_LISTENER_COUNTER = 0;
        supplier.addStatusListener(new MCRProcessableStatusListener() {
            @Override
            public void onStatusChange(MCRProcessable source, MCRProcessableStatus oldStatus,
                MCRProcessableStatus newStatus) {
                STATUS_LISTENER_COUNTER++;
            }
        });

        // PROGRESS LISTENER
        PROGRESS_LISTENER_COUNTER = 0;
        supplier.addProgressListener(new MCRProgressableListener() {
            @Override
            public void onProgressTextChange(MCRProgressable source, String oldProgressText, String newProgressText) {
                PROGRESS_LISTENER_COUNTER++;
            }

            @Override
            public void onProgressChange(MCRProgressable source, Integer oldProgress, Integer newProgress) {
                PROGRESS_LISTENER_COUNTER++;
            }
        });

        // await the task is executed
        supplier.getFuture().get();

        // TESTS
        assertEquals("the processable should be finished successful", MCRProcessableStatus.successful,
            supplier.getStatus());
        assertEquals("the progressable should be at 100", Integer.valueOf(100), supplier.getProgress());
        assertEquals("end", supplier.getProgressText());
        assertEquals("there shouldn't be any error", null, supplier.getError());
        assertNotEquals("there should be a start time", null, supplier.getStartTime());
        assertNotEquals("there should be an end time", null, supplier.getEndTime());
        assertNotEquals(null, supplier.took());

        assertNotEquals("the status listener wasn't called", 0, STATUS_LISTENER_COUNTER);
        assertNotEquals("the progressable listener wasn't called", 0, PROGRESS_LISTENER_COUNTER);

        es.shutdown();
        es.awaitTermination(10, TimeUnit.SECONDS);
    }

    private static class TestRunnable extends MCRAbstractProgressable implements Runnable {

        @Override
        public void run() {
            setProgress(0);
            setProgressText("start");
            try {
                Thread.sleep(100);
                setProgress(100);
                setProgressText("end");
            } catch (InterruptedException e) {
                LOGGER.warn("test thread interrupted", e);
            }
        }

    }

}

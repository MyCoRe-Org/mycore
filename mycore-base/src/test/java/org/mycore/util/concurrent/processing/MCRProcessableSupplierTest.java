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
import org.mycore.common.processing.MCRProcessableStatus;
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
        supplier.addStatusListener((source, oldStatus, newStatus) -> STATUS_LISTENER_COUNTER++);

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

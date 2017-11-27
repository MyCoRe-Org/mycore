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
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableDefaultCollection;
import org.mycore.common.processing.MCRProcessableRegistry;
import org.mycore.common.processing.MCRProcessableStatus;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;

public class MCRProcessableFactoryTest extends MCRTestCase {

    private static Logger LOGGER = LogManager.getLogger();

    @Test
    public void newPool() throws Exception {
        MCRProcessableRegistry registry = new MCRCentralProcessableRegistry();
        MCRProcessableCollection collection = new MCRProcessableDefaultCollection("test");
        registry.register(collection);

        int nThreads = 3;
        ExecutorService es = Executors.newFixedThreadPool(nThreads);
        MCRProcessableExecutor pes = MCRProcessableFactory.newPool(es, collection);

        assertEquals("No runnables should be queued right now.", 0, collection.stream().count());
        assertEquals("Only the 'test' collection should be registered.", 1, registry.stream().count());

        Semaphore semaphore = new Semaphore(nThreads);
        semaphore.acquire(nThreads); //lock threads until ready

        MCRProcessableSupplier<?> sup1 = pes.submit(sleepyThread(semaphore));
        MCRProcessableSupplier<?> sup2 = pes.submit(sleepyThread(semaphore));
        MCRProcessableSupplier<?> sup3 = pes.submit(sleepyThread(semaphore));

        MCRProcessableStatus s1 = sup1.getStatus();
        MCRProcessableStatus s2 = sup2.getStatus();
        MCRProcessableStatus s3 = sup3.getStatus();
        String msgPrefix = "Job should be created or in processing: ";
        assertTrue(msgPrefix + s1, MCRProcessableStatus.processing == s1 || MCRProcessableStatus.created == s1);
        assertTrue(msgPrefix + s2, MCRProcessableStatus.processing == s2 || MCRProcessableStatus.created == s2);
        assertTrue(msgPrefix + s3, MCRProcessableStatus.processing == s3 || MCRProcessableStatus.created == s3);

        assertEquals(3, collection.stream().count());
        semaphore.release(nThreads); //go

        CompletableFuture.allOf(sup1.getFuture(), sup2.getFuture(), sup3.getFuture()).get();

        assertEquals(MCRProcessableStatus.successful, sup1.getStatus());
        assertEquals(MCRProcessableStatus.successful, sup2.getStatus());
        assertEquals(MCRProcessableStatus.successful, sup3.getStatus());

        es.shutdown();
        es.awaitTermination(10, TimeUnit.SECONDS);
    }

    private Runnable sleepyThread(Semaphore semaphore) {
        return () -> {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                LOGGER.warn("test thread interrupted", e);
            } finally {
                semaphore.release();
            }

        };
    }

}

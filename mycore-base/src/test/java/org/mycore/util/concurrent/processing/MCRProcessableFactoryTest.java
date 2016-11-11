package org.mycore.util.concurrent.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableDefaultCollection;
import org.mycore.common.processing.MCRProcessableRegistry;
import org.mycore.common.processing.MCRProcessableStatus;

public class MCRProcessableFactoryTest {

    private static Logger LOGGER = LogManager.getLogger();

    @Test
    public void newPool() throws Exception {
        MCRProcessableRegistry registry = new MCRProcessableRegistry();
        MCRProcessableCollection collection = new MCRProcessableDefaultCollection("test");
        registry.register(collection);

        ExecutorService es = Executors.newFixedThreadPool(3);
        MCRProcessableExecutor pes = MCRProcessableFactory.newPool(es, collection);

        assertEquals(0, collection.stream().count());
        assertEquals(1, registry.stream().count());

        MCRProcessableSupplier<?> sup1 = pes.submit(sleepyThread());
        MCRProcessableSupplier<?> sup2 = pes.submit(sleepyThread());
        MCRProcessableSupplier<?> sup3 = pes.submit(sleepyThread());

        MCRProcessableStatus s1 = sup1.getStatus();
        MCRProcessableStatus s2 = sup2.getStatus();
        MCRProcessableStatus s3 = sup3.getStatus();
        assertTrue(MCRProcessableStatus.processing == s1 || MCRProcessableStatus.created == s1);
        assertTrue(MCRProcessableStatus.processing == s2 || MCRProcessableStatus.created == s2);
        assertTrue(MCRProcessableStatus.processing == s3 || MCRProcessableStatus.created == s3);

        assertEquals(3, collection.stream().count());

        CompletableFuture.allOf(sup1.getFuture(), sup2.getFuture(), sup3.getFuture()).get();

        assertEquals(MCRProcessableStatus.successful, sup1.getStatus());
        assertEquals(MCRProcessableStatus.successful, sup2.getStatus());
        assertEquals(MCRProcessableStatus.successful, sup3.getStatus());

        es.shutdown();
        es.awaitTermination(10, TimeUnit.SECONDS);
    }

    private Runnable sleepyThread() {
        return new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    LOGGER.warn("test thread interrupted", e);
                }

            }
        };
    }

}

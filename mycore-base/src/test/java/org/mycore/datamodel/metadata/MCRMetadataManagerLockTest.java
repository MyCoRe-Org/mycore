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

package org.mycore.datamodel.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRMetadataManager.MCRObjectLock;
import org.mycore.test.MyCoReTest;

/**
 * Tests for the per-object lock primitive on {@link MCRMetadataManager}.
 * No metadata store is touched; only lock acquisition/release semantics are exercised.
 */
@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRMetadataManagerLockTest {

    private static MCRObjectID idA() {
        return MCRObjectID.getInstance("MyCoRe_test_00000001");
    }

    private static MCRObjectID idB() {
        return MCRObjectID.getInstance("MyCoRe_test_00000002");
    }

    @Test
    public void reentrantSameThread() {
        MCRObjectID id = idA();
        try (MCRObjectLock outer = MCRMetadataManager.lock(id)) {
            try (MCRObjectLock inner = MCRMetadataManager.lock(id)) {
                assertTrue(true);
            }
        }
    }

    @Test
    public void differentIdsRunInParallel() throws Exception {
        MCRObjectID a = idA();
        MCRObjectID b = idB();
        CountDownLatch bothInside = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            pool.submit(() -> {
                try (MCRObjectLock l = MCRMetadataManager.lock(a)) {
                    bothInside.countDown();
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
            pool.submit(() -> {
                try (MCRObjectLock l = MCRMetadataManager.lock(b)) {
                    bothInside.countDown();
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
            assertTrue(bothInside.await(5, TimeUnit.SECONDS),
                "both threads should hold their lock simultaneously");
            release.countDown();
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
        @MCRTestProperty(key = "MCR.Metadata.Manager.LockTimeoutSeconds", string = "1")
    })
    public void timeoutThrows() throws Exception {
        MCRObjectID id = idA();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            pool.submit(() -> {
                try (MCRObjectLock l = MCRMetadataManager.lock(id)) {
                    holding.countDown();
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
            assertTrue(holding.await(5, TimeUnit.SECONDS));
            assertThrows(MCRPersistenceException.class, () -> MCRMetadataManager.lock(id));
            release.countDown();
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void refcountCleansUpAfterRelease() throws Exception {
        for (int i = 0; i < 100; i++) {
            MCRObjectID id = MCRObjectID.getInstance(String.format("MyCoRe_test_%08d", 100 + i));
            try (MCRObjectLock l = MCRMetadataManager.lock(id)) {
                // nothing
            }
        }
        Field f = MCRMetadataManager.class.getDeclaredField("LOCKS");
        f.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) f.get(null);
        assertEquals(0, map.size(), "LOCKS map should be empty after all releases");
    }

    @Test
    public void mutualExclusionSerialisesAccess() throws Exception {
        MCRObjectID id = idA();
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxObserved = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            for (int i = 0; i < 8; i++) {
                pool.submit(() -> {
                    try (MCRObjectLock l = MCRMetadataManager.lock(id)) {
                        int now = inside.incrementAndGet();
                        maxObserved.accumulateAndGet(now, Math::max);
                        Thread.sleep(20);
                        inside.decrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
            }
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(1, maxObserved.get(), "only one thread may hold the lock at a time");
    }
}

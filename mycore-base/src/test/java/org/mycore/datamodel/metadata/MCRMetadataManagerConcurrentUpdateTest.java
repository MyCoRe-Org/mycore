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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.events.MCREvent.ObjectType;
import org.mycore.common.events.MCREventManager;
import org.mycore.datamodel.common.MCRXMLMetadataEventHandler;
import org.mycore.datamodel.metadata.MCRMetadataManager.MCRObjectLock;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

/**
 * Integration tests for the per-object lock wired into {@link MCRMetadataManager}'s
 * write methods. Both tests would fail without the automatic lock.
 */
@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessBaseImpl.class),
    @MCRTestProperty(key = "MCR.Metadata.Type.document", string = "true")
})
public class MCRMetadataManagerConcurrentUpdateTest {

    private MCRObject obj;

    @BeforeEach
    public void setUp() throws Exception {
        MCREventManager.getInstance().clear();
        MCREventManager.getInstance().addEventHandler(ObjectType.OBJECT, new MCRXMLMetadataEventHandler());
        obj = newObject("test_document_00000001");
        MCRMetadataManager.create(obj);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (MCRMetadataManager.exists(obj.getId())) {
            MCRMetadataManager.delete(MCRMetadataManager.retrieveMCRObject(obj.getId()));
        }
    }

    private static MCRObject newObject(String id) {
        MCRObject object = new MCRObject();
        object.setId(MCRObjectID.getInstance(id));
        object.setSchema("noSchema");
        return object;
    }

    /**
     * Holds {@code lock(id)} on a separate thread; main thread tries an
     * {@code update(id)} which must block until the holder releases.
     * Without the automatic lock inside {@code update}, the call returns
     * immediately and the assertion fails.
     */
    @Test
    public void updateBlocksWhileExternalLockHeld() throws Exception {
        MCRObjectID id = obj.getId();
        MCRSession parentSession = MCRSessionMgr.getCurrentSession();

        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseHolder = new CountDownLatch(1);
        AtomicBoolean updateReturned = new AtomicBoolean();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<Void> holder = CompletableFuture.runAsync(() -> {
                MCRSessionMgr.setCurrentSession(parentSession);
                try (MCRObjectLock l = MCRMetadataManager.lock(id)) {
                    lockHeld.countDown();
                    releaseHolder.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    MCRSessionMgr.releaseCurrentSession();
                }
            }, pool);

            assertTrue(lockHeld.await(5, TimeUnit.SECONDS), "holder must acquire the lock");

            CompletableFuture<Void> updater = CompletableFuture.runAsync(() -> {
                MCRSessionMgr.setCurrentSession(parentSession);
                try {
                    MCRObject loaded = MCRMetadataManager.retrieveMCRObject(id);
                    loaded.getService().addFlag("updated", "yes");
                    MCRMetadataManager.update(loaded);
                    updateReturned.set(true);
                } catch (Exception e) {
                    throw new MCRException("update failed", e);
                } finally {
                    MCRSessionMgr.releaseCurrentSession();
                }
            }, pool);

            // Give the updater enough time to enter update() and block on the lock.
            // Without the automatic lock, update() would complete in well under 1s.
            Thread.sleep(1000);
            assertFalse(updateReturned.get(),
                "update must block while another thread holds lock(id)");

            releaseHolder.countDown();
            holder.get(5, TimeUnit.SECONDS);
            updater.get(5, TimeUnit.SECONDS);

            assertTrue(updateReturned.get(), "update must complete after holder releases lock");

            MCRObject finalState = MCRMetadataManager.retrieveMCRObject(id);
            assertEquals(List.of("yes"), finalState.getService().getFlags("updated"));
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    /**
     * Forces both threads to retrieve the same {@code modifyDate} snapshot before
     * either persists, then both call {@code update}. With the lock in place,
     * one wins and the other's stale {@code modifyDate} is rejected by
     * {@code checkModificationDates} (which now sits inside the locked critical
     * section). Without the lock the two writes interleave and silently lose one
     * caller's flag.
     */
    @Test
    public void forcedRaceProducesExactlyOneWinner() throws Exception {
        MCRObjectID id = obj.getId();
        MCRSession parentSession = MCRSessionMgr.getCurrentSession();

        CyclicBarrier afterRetrieve = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<Boolean> fA = racingUpdate(pool, parentSession, id, afterRetrieve, "A");
            CompletableFuture<Boolean> fB = racingUpdate(pool, parentSession, id, afterRetrieve, "B");

            int succeeded = 0;
            int rejected = 0;
            for (CompletableFuture<Boolean> f : List.of(fA, fB)) {
                try {
                    if (f.get(15, TimeUnit.SECONDS)) {
                        succeeded++;
                    } else {
                        rejected++;
                    }
                } catch (ExecutionException ee) {
                    Throwable root = rootCause(ee);
                    if (root instanceof MCRPersistenceException) {
                        rejected++;
                    } else {
                        throw ee;
                    }
                }
            }

            assertEquals(1, succeeded,
                "exactly one update must succeed; the other must be rejected as stale");
            assertEquals(1, rejected, "exactly one update must be rejected");

            MCRObject finalState = MCRMetadataManager.retrieveMCRObject(id);
            long winnerFlags = finalState.getService().getFlags("race").stream()
                .filter(s -> s.equals("A") || s.equals("B")).count();
            assertEquals(1, winnerFlags, "persisted state must contain exactly the winner's flag");
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static CompletableFuture<Boolean> racingUpdate(ExecutorService pool, MCRSession session,
        MCRObjectID id, CyclicBarrier afterRetrieve, String value) {
        return CompletableFuture.supplyAsync(() -> {
            MCRSessionMgr.setCurrentSession(session);
            try {
                MCRObject loaded = MCRMetadataManager.retrieveMCRObject(id);
                loaded.getService().addFlag("race", value);
                // wait until the other thread has also retrieved its (stale) snapshot
                afterRetrieve.await(15, TimeUnit.SECONDS);
                try {
                    MCRMetadataManager.update(loaded);
                    return true;
                } catch (MCRPersistenceException e) {
                    return false;
                } catch (Exception e) {
                    throw new MCRException("unexpected update failure", e);
                }
            } catch (Exception e) {
                throw new MCRException("racing update failed", e);
            } finally {
                MCRSessionMgr.releaseCurrentSession();
            }
        }, pool);
    }

    private static Throwable rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c;
    }
}

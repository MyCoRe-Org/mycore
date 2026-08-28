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

package org.mycore.services.queuedjob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.processing.impl.MCRCentralProcessableRegistry;
import org.mycore.services.queuedjob.action.MCRTestJobAction1;
import org.mycore.services.queuedjob.config2.MCRConfiguration2JobConfig;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRJPATestHelper;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Processable.Registry.Class", classNameOf = MCRCentralProcessableRegistry.class),
    @MCRTestProperty(key = "MCR.QueuedJob.MCRTestJobAction1.JobThreads", string = "2"),
    @MCRTestProperty(key = "MCR.QueuedJob.MCRTestJobAction1.Listeners", classNameOf =
        MCRJobThreadStarterTest.SchedulingProbe.class)
})
public class MCRJobThreadStarterTest {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int JOB_THREADS = 2;

    /**
     * Time a job thread is kept inside {@link MCRJobRunnable#run()} after it notified the job manager about the
     * completed job. Only has to outlast one scheduling round trip of the manager thread; a longer hold can never
     * turn a failing run into a passing one, it only widens the window in which the manager is able to notice the
     * freed job thread.
     */
    private static final long COMPLETION_HOLD_MS = 200;

    private static final AtomicInteger HANDED_OUT_JOBS = new AtomicInteger();

    private static CountDownLatch allJobThreadsBusy;

    private static CountDownLatch pendingJobIsPollable;

    private static CountDownLatch managerIsScheduling;

    private static CountDownLatch signalDelivered;

    private static List<MCRJob> getAllJobs(MCRJobDAOJPAImpl dao, Class<? extends MCRJobAction> action) {
        return dao.getJobs(action, null, null, null, null);
    }

    @BeforeEach
    public void setUp() {
        HANDED_OUT_JOBS.set(0);
        allJobThreadsBusy = new CountDownLatch(JOB_THREADS);
        pendingJobIsPollable = new CountDownLatch(1);
        managerIsScheduling = new CountDownLatch(1);
        signalDelivered = new CountDownLatch(1);
    }

    @Test
    public void testRun() {
        MCRConfiguration2JobConfig config = new MCRConfiguration2JobConfig();
        MCRJobDAOJPAImpl dao = new GatedJobDAO();
        MCRJobQueue queue = new MCRJobQueue(MCRTestJobAction1.class, config, dao);
        MCRJobThreadStarter starter = new MCRJobThreadStarter(MCRTestJobAction1.class, config, queue);

        Date baseTime = new Date();

        MCRJob job1 = new MCRJob(MCRTestJobAction1.class);
        job1.setParameter("count", "1");
        job1.setParameter("error", "false");
        job1.setStatus(MCRJobStatus.NEW);
        job1.setAdded(new Date(baseTime.getTime() + 20));
        queue.offer(job1);

        MCRJob job2 = new MCRJob(MCRTestJobAction1.class);
        job2.setParameter("count", "2");
        job2.setParameter("error", "false");
        job2.setStatus(MCRJobStatus.NEW);
        job2.setAdded(new Date(baseTime.getTime() + 40));
        queue.offer(job2);

        MCRJob job3 = new MCRJob(MCRTestJobAction1.class);
        job3.setParameter("count", "3");
        job3.setParameter("error", "true");
        job3.setStatus(MCRJobStatus.NEW);
        job3.setAdded(new Date(baseTime.getTime() + 60));
        queue.offer(job3);

        MCRJPATestHelper.startNewTransaction();

        Thread thread = new Thread(starter);
        thread.start();

        try {
            // The manager may not hand out more jobs than it has threads, so job3 is still NEW and the manager is
            // waiting for a job thread to become free. Release job3 before any running job can complete.
            assertTrue(allJobThreadsBusy.await(30, TimeUnit.SECONDS), "job threads did not start");
            pendingJobIsPollable.countDown();

            final int maxWait = 5000; //lower than MCRJobThreadStarter.ONE_MINUTE_IN_MS to show slow throughput
            long startTime = System.currentTimeMillis();
            int stepTime = 100;
            while (getAllJobs(dao, job1.getAction()).stream()
                .filter(j -> j.getStatus() == MCRJobStatus.FINISHED || j.getStatus() == MCRJobStatus.ERROR)
                .count() < 3 && startTime + maxWait >= System.currentTimeMillis()) {
                Thread.yield();
                Thread.sleep(stepTime);
                LOGGER.info("Waiting for jobs to finish. Time left: {}",
                    maxWait - System.currentTimeMillis() + startTime);
                MCRJPATestHelper.startNewTransaction();
            }
        } catch (InterruptedException e) {
            throw new MCRException(e);
        } finally {
            // fixes Thread leak: the manager thread and its job threads outlived this test otherwise
            pendingJobIsPollable.countDown();
            starter.prepareClose();
            starter.close();
        }

        long allJobCount = getAllJobs(dao, job1.getAction()).stream().count();
        long pendingJobCount =
            getAllJobs(dao, job1.getAction()).stream().filter(j -> j.getStatus() == MCRJobStatus.NEW).count();
        long finishedJobCount =
            getAllJobs(dao, job1.getAction()).stream().filter(j -> j.getStatus() == MCRJobStatus.FINISHED).count();
        long errorJobCount =
            getAllJobs(dao, job1.getAction()).stream().filter(j -> j.getStatus() == MCRJobStatus.ERROR).count();

        getAllJobs(dao, job1.getAction())
            .forEach(j -> LOGGER.info("Job in queue: {}", j));
        assertEquals(3, allJobCount, "There should be 3 jobs");
        assertEquals(0, pendingJobCount,
            "Job manager must not leave a job pending while its job threads are idle");
        assertEquals(2, finishedJobCount, "Finished Job count should be 2");
        assertEquals(1, errorJobCount, "Error Job count should be 1");
    }

    /**
     * A job offered while the job manager is scheduling must not have to wait for the fallback poll.
     */
    @Test
    public void testSignalWhileScheduling() throws InterruptedException {
        pendingJobIsPollable.countDown(); // no job thread has to be held back in this test
        MCRConfiguration2JobConfig config = new MCRConfiguration2JobConfig();
        MCRJobDAOJPAImpl dao = new BlockingJobDAO();
        MCRJobQueue queue = new MCRJobQueue(MCRTestJobAction1.class, config, dao);
        MCRJobThreadStarter starter = new MCRJobThreadStarter(MCRTestJobAction1.class, config, queue);

        MCRJPATestHelper.startNewTransaction();

        Thread thread = new Thread(starter);
        thread.start();
        try {
            // the manager is inside its poll now: past its last check for free job threads, but not yet waiting
            assertTrue(managerIsScheduling.await(30, TimeUnit.SECONDS), "manager did not start");

            MCRJob job = new MCRJob(MCRTestJobAction1.class);
            job.setParameter("count", "1");
            job.setParameter("error", "false");
            job.setStatus(MCRJobStatus.NEW);
            job.setAdded(new Date());
            queue.offer(job);
            MCRJPATestHelper.startNewTransaction();
            MCRTransactionManager.commitTransactions(); // runs the on-commit task that notifies the manager
            signalDelivered.countDown();

            final int maxWait = 5000; //lower than MCRJobThreadStarter.ONE_MINUTE_IN_MS to show slow throughput
            long startTime = System.currentTimeMillis();
            while (getAllJobs(dao, MCRTestJobAction1.class).stream()
                .noneMatch(j -> j.getStatus() == MCRJobStatus.FINISHED)
                && startTime + maxWait >= System.currentTimeMillis()) {
                Thread.sleep(100);
                MCRJPATestHelper.startNewTransaction();
            }
        } finally {
            // fixes Thread leak: the manager thread and its job threads outlived this test otherwise
            signalDelivered.countDown();
            starter.prepareClose();
            starter.close();
        }

        long finishedJobCount =
            getAllJobs(dao, MCRTestJobAction1.class).stream().filter(j -> j.getStatus() == MCRJobStatus.FINISHED)
                .count();
        assertEquals(1, finishedJobCount, "Job offered while the manager was scheduling should be run");
    }

    /**
     * Hands out no more jobs than the manager has threads until the test opened the gate. Without this the manager
     * pre-fetches all three jobs while its threads are still starting up, which hides the scheduling defect.
     */
    private static final class GatedJobDAO extends MCRJobDAOJPAImpl {

        @Override
        public List<MCRJob> getNextJobs(Class<? extends MCRJobAction> action, Integer amount) {
            if (HANDED_OUT_JOBS.get() >= JOB_THREADS && pendingJobIsPollable.getCount() > 0) {
                return List.of();
            }
            List<MCRJob> jobs = super.getNextJobs(action, amount);
            HANDED_OUT_JOBS.addAndGet(jobs.size());
            return jobs;
        }
    }

    /**
     * Blocks the job manager inside its poll, so that the test can deliver a signal while the manager is
     * scheduling rather than waiting.
     */
    private static final class BlockingJobDAO extends MCRJobDAOJPAImpl {

        @Override
        public List<MCRJob> getNextJobs(Class<? extends MCRJobAction> action, Integer amount) {
            List<MCRJob> jobs = super.getNextJobs(action, amount);
            managerIsScheduling.countDown();
            SchedulingProbe.await(signalDelivered, 30000);
            return jobs;
        }
    }

    /**
     * Registered via <code>MCR.QueuedJob.MCRTestJobAction1.Listeners</code> and therefore called by
     * {@link MCRJobRunnable} right after the job manager's own listener, on the job thread and still inside
     * {@link MCRJobRunnable#run()}.
     */
    public static final class SchedulingProbe implements MCRJobStatusListener {

        @Override
        public void onProcessing(MCRJob job) {
            // ThreadPoolExecutor#beforeExecute() already counted this thread as active
            allJobThreadsBusy.countDown();
            await(pendingJobIsPollable, 30000);
        }

        @Override
        public void onSuccess(MCRJob job) {
            holdJobThread();
        }

        @Override
        public void onError(MCRJob job, Exception e) {
            holdJobThread();
        }

        /**
         * The manager has just been notified about this job. Stay inside {@link MCRJobRunnable#run()} so that
         * {@link java.util.concurrent.ThreadPoolExecutor#afterExecute} cannot release this job thread yet: the
         * manager has to decide about the pending job while the completed job still occupies its thread.
         */
        private static void holdJobThread() {
            try {
                Thread.sleep(COMPLETION_HOLD_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MCRException(e);
            }
        }

        static void await(CountDownLatch latch, long millis) {
            try {
                latch.await(millis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MCRException(e);
            }
        }
    }
}

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

package org.mycore.services.queuedjob;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableDefaultCollection;
import org.mycore.common.processing.MCRProcessableRegistry;
import org.mycore.util.concurrent.processing.MCRProcessableExecutor;
import org.mycore.util.concurrent.processing.MCRProcessableFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;

/**
 * Pulls {@link MCRJob}s from Database and starts {@link MCRJobRunnable}s.
 * 
 * @author René Adler
 * @author Sebastian Hofmann
 */
public class MCRJobThreadStarter implements Runnable, Closeable {

    private static final long ONE_MINUTE_IN_MS = TimeUnit.MINUTES.toMillis(1);

    private static final Logger LOGGER = LogManager.getLogger(MCRJobThreadStarter.class);

    private final MCRJobQueue jobQueue;

    private final ThreadPoolExecutor jobExecutor;

    private final Class<? extends MCRJobAction> action;

    private volatile boolean running = true;

    private final MCRProcessableDefaultCollection processableCollection;

    private final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();

    private final ReentrantLock runLock;

    private final ListenerNotifier listener;

    private MCRProcessableExecutor processableExecutor;

    private final AtomicInteger activeThreads = new AtomicInteger();

    private final int maxJobThreadCount;

    private final MCRJobConfig config;

    MCRJobThreadStarter(Class<? extends MCRJobAction> action, MCRJobConfig config, MCRJobQueue jobQueue) {
        this.config = config;
        MCRShutdownHandler.getInstance().addCloseable(this);
        this.action = action;
        runLock = new ReentrantLock();

        this.jobQueue = jobQueue;

        // listen for new jobs
        listener = new ListenerNotifier();
        jobQueue.addListener(listener);

        maxJobThreadCount = config.maxJobThreadCount(action).orElseGet(config::maxJobThreadCount);

        jobExecutor = new ActiveCountingThreadPoolExecutor(maxJobThreadCount, workQueue,
            new JobThreadFactory(getSimpleActionName()), activeThreads);

        MCRProcessableRegistry registry = MCRProcessableRegistry.getSingleInstance();
        processableCollection = new MCRProcessableDefaultCollection(getName());
        registry.register(processableCollection);
    }

    /**
     * Starts local threads ({@link MCRJobRunnable}) and gives {@link MCRJob} instances to them.
     * Use property <code>"MCR.QueuedJob.JobThreads"</code> to specify how many concurrent threads should be running.
     * <code>"MCR.QueuedJob.activated"</code> can be used activate or deactivate general {@link MCRJob} running.
     */
    @Override
    public void run() {
        boolean activated = config.activated(action).orElseGet(config::activated);
        LOGGER.info("JobQueue {} is {}", action.getName(), activated ? "activated" : "deactivated");

        if (!activated) {
            return;
        }

        Thread.currentThread().setName(getName());
        //get this MCRSession a speaking name
        MCRSessionMgr.unlock();
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());

        running = true;
        processableExecutor = MCRProcessableFactory.newPool(jobExecutor, processableCollection);
        processableCollection.setProperty("running", running);

        LOGGER.info("JobManager for {} with {} thread(s) is started", action.getName(), getMaxJobThreadCount());
        while (running) {
            try {
                while (hasFreeJobThreads()) {
                    if (!scheduleNextJob()) {
                        break;
                    }
                }
                synchronized (listener) {
                    listener.wait(ONE_MINUTE_IN_MS);
                }
            } catch (PersistenceException e) {
                LOGGER.warn("We have an database error, sleep and run later.", e);
                try {
                    Thread.sleep(ONE_MINUTE_IN_MS);
                } catch (InterruptedException ie) {
                    LOGGER.error("Waiting for database was interrupted.", ie);
                }
            } catch (Exception e) {
                LOGGER.error("Keep running while catching exceptions.", e);
            }
        }
        processableCollection.setProperty("running", running);

        LOGGER.info("{} thread finished", getName());
        MCRSessionMgr.releaseCurrentSession();
    }

    /**
     * @return true if a job was scheduled and there could be more jobs
     */
    private boolean scheduleNextJob() {
        runLock.lock();
        try {
            if (!running) {
                return false;
            }

            EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
            EntityTransaction transaction = em.getTransaction();

            MCRJob job = null;
            MCRJobAction action = null;
            try {
                transaction.begin();

                job = jobQueue.poll();
                processableCollection.setProperty("queue size", jobQueue.size());

                if (job != null) {
                    action = toMCRJobAction(job);

                    if (action != null && !action.isActivated()) {
                        job.setStatus(MCRJobStatus.NEW);
                        job.setStart(null);
                    }
                }
                transaction.commit();
            } catch (RollbackException e) {
                LOGGER.error("Error while getting next job.", e);
                try {
                    transaction.rollback();
                } catch (RuntimeException re) {
                    LOGGER.warn("Could not rollback transaction.", re);
                }
            } finally {
                em.close();
            }
            if (job != null && action != null && action.isActivated() && !jobExecutor.isShutdown()) {
                LOGGER.info("Creating:{}", job);
                processableExecutor.submit(new MCRJobRunnable(job, config, List.of(listener), action));
                return true;
            }
        } finally {
            runLock.unlock();
        }
        return false;
    }

    private boolean hasFreeJobThreads() {
        return activeThreads.get() < getMaxJobThreadCount();
    }

    private int getMaxJobThreadCount() {
        return maxJobThreadCount;
    }

    /**
     * stops transmitting {@link MCRJob} to {@link MCRJobRunnable} and prepares shutdown.
     */
    public void prepareClose() {
        LOGGER.info("Closing manager thread");
        //signal manager thread to stop now
        running = false;
        //Wake up, Neo!
        synchronized (listener) {
            LOGGER.debug("Wake up queue");
            listener.notifyAll();
        }
        runLock.lock();
        try {
            if (processableExecutor != null) {
                LOGGER.debug("Shutdown executor jobs.");
                processableExecutor.getExecutor().shutdown();
                try {
                    LOGGER.debug("Await termination of executor jobs.");
                    processableExecutor.getExecutor().awaitTermination(60, TimeUnit.SECONDS);
                    LOGGER.debug("All jobs finished.");
                } catch (InterruptedException e) {
                    LOGGER.debug("Could not wait 60 seconds...", e);
                }
            }
        } finally {
            runLock.unlock();
        }
    }

    /**
     * Shuts down this thread and every local threads spawned by {@link #run()}.
     */
    @Override
    public void close() {
        if (processableExecutor != null && !processableExecutor.getExecutor().isShutdown()) {
            LOGGER.info("We are in a hurry, closing service right now");
            this.jobExecutor.shutdownNow();
            try {
                this.jobExecutor.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.debug("Could not wait  60 seconds...", e);
            }
        }
    }

    @Override
    public int getPriority() {
        return MCRShutdownHandler.Closeable.DEFAULT_PRIORITY - 1;
    }

    private String getSimpleActionName() {
        return action.getSimpleName();
    }

    /**
     * Returns the name of this job manager.
     * 
     * @return the name
     */
    public String getName() {
        return getSimpleActionName() + " Manager";
    }

    /**
     * Returns the processable collection assigned to this job manager.
     * 
     * @return the processable collection
     */
    public MCRProcessableCollection getProcessableCollection() {
        return processableCollection;
    }

    private static MCRJobAction toMCRJobAction(MCRJob job) {
        try {
            Constructor<? extends MCRJobAction> actionConstructor = job.getAction().getConstructor(MCRJob.class);
            return actionConstructor.newInstance(job);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Notifies waiting threads when a job is set to error, success or processing. When a job is added to the queue, the
     * Thread is notified after the session is committed.
     */
    private static final class ListenerNotifier implements MCRJobQueueEventListener, MCRJobStatusListener {

        @Override
        public void onJobAdded(MCRJob job) {
            // a job was added to the queue, but the transaction is not yet committed, so we have to wait for it.
            MCRSessionMgr.getCurrentSession().onCommit(() -> {
                synchronized (this) {
                    this.notifyAll();
                }
            });
        }

        @Override
        public void onError(MCRJob job, Exception e) {
            synchronized (this) {
                this.notifyAll();
            }
        }

        @Override
        public void onSuccess(MCRJob job) {
            synchronized (this) {
                this.notifyAll();
            }
        }

        @Override
        public void onProcessing(MCRJob job) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    /**
     * A {@link ThreadFactory} that creates threads with a given name prefix.
     */
    private static final class JobThreadFactory implements ThreadFactory {

        private final String actionName;

        private AtomicInteger tNum = new AtomicInteger(0);

        JobThreadFactory(String simpleActionName) {
            this.actionName = simpleActionName;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, actionName + "Worker#" + tNum.incrementAndGet());
        }
    }

    /**
     * A {@link ThreadPoolExecutor} that counts the number of active threads. (without iterating over the thread pool)
     */
    private static final class ActiveCountingThreadPoolExecutor extends ThreadPoolExecutor {

        private final AtomicInteger activeThreads;

        ActiveCountingThreadPoolExecutor(int maxJobThreadCount,
            LinkedBlockingQueue<Runnable> workQueue,
            JobThreadFactory jobThreadFactory,
            AtomicInteger activeThreads) {
            super(maxJobThreadCount, maxJobThreadCount, 1, TimeUnit.DAYS, workQueue, jobThreadFactory);
            this.activeThreads = activeThreads;
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            activeThreads.decrementAndGet();
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            activeThreads.incrementAndGet();
        }

    }

}

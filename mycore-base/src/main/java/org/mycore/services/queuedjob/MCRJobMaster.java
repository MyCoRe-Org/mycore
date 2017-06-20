/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.services.queuedjob;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.common.inject.MCRInjectorConfig;
import org.mycore.common.processing.MCRProcessableCollection;
import org.mycore.common.processing.MCRProcessableDefaultCollection;
import org.mycore.common.processing.MCRProcessableRegistry;
import org.mycore.util.concurrent.processing.MCRProcessableExecutor;
import org.mycore.util.concurrent.processing.MCRProcessableFactory;

/**
 * The master of all {@link MCRJobThread}s threads.
 * 
 * @author Ren\u00E9 Adler
 */
public class MCRJobMaster implements Runnable, Closeable {

    private static MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static Map<String, MCRJobMaster> INSTANCES = new HashMap<String, MCRJobMaster>();

    private static Logger LOGGER = LogManager.getLogger(MCRJobMaster.class);

    private final MCRJobQueue JOB_QUEUE;

    private Class<? extends MCRJobAction> action;

    private MCRProcessableExecutor jobServe;

    private MCRProcessableDefaultCollection processableCollection;

    private volatile boolean running = true;

    private ReentrantLock runLock;

    private MCRJobMaster(Class<? extends MCRJobAction> action) {
        MCRShutdownHandler.getInstance().addCloseable(this);
        this.action = action;
        runLock = new ReentrantLock();
        JOB_QUEUE = MCRJobQueue.getInstance(action);

        MCRProcessableRegistry registry = MCRInjectorConfig.injector().getInstance(MCRProcessableRegistry.class);
        processableCollection = new MCRProcessableDefaultCollection(getName());
        registry.register(processableCollection);
    }

    /**
     * Returns an singleton instance of this class.
     * 
     * @param action the {@link MCRJobAction} or <code>null</code>
     * @return the instance of this class
     */
    public static MCRJobMaster getInstance(Class<? extends MCRJobAction> action) {
        String key = action != null && !MCRJobQueue.singleQueue ? action.getName() : "single";
        MCRJobMaster master = INSTANCES.get(key);
        if (master == null) {
            master = new MCRJobMaster(MCRJobQueue.singleQueue ? null : action);
            INSTANCES.put(key, master);
        }

        if (!master.running)
            return null;

        return master;
    }

    /**
     * Return if {@link MCRJobMaster} is running.
     * 
     * @return if is running
     */
    public static boolean isRunning(Class<? extends MCRJobAction> action) {
        String key = action != null && !MCRJobQueue.singleQueue ? action.getName() : "single";
        MCRJobMaster master = INSTANCES.get(key);

        return master != null && master.running;
    }

    /**
     * Starts the local {@link MCRJobMaster}.
     * Can be auto started if <code>"MCR.QueuedJob.{?MCRJobAction?.}autostart"</code> 
     * is set to <code>true</code>.
     */
    public static void startMasterThread(Class<? extends MCRJobAction> action) {
        if (!isRunning(action)) {
            LOGGER.info("Starting job master thread" + (action == null ? "" : " for action \"" + action.getName())
                + "\".");
            final Thread master = new Thread(getInstance(action));
            master.start();
        }
    }

    /**
     * Starts local threads ({@link MCRJobThread}) and gives {@link MCRJob} instances to them.
     * Use property <code>"MCR.QueuedJob.JobThreads"</code> to specify how many concurrent threads should be running.<br>
     * <code>"MCR.QueuedJob.activated"</code> can be used activate or deactivate general {@link MCRJob} running. 
     */
    @Override
    public void run() {
        Thread.currentThread().setName(getName());
        //get this MCRSession a speaking name
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());

        boolean activated = CONFIG.getBoolean(MCRJobQueue.CONFIG_PREFIX + "activated", true);
        activated = activated
            && CONFIG.getBoolean(MCRJobQueue.CONFIG_PREFIX + JOB_QUEUE.CONFIG_PREFIX_ADD + "activated", true);

        LOGGER.info("JobQueue" + (MCRJobQueue.singleQueue ? "" : " for \"" + action.getName() + "\"") + " is "
            + (activated ? "activated" : "deactivated"));
        if (activated) {
            running = true;
            int jobThreadCount = CONFIG.getInt(MCRJobQueue.CONFIG_PREFIX + "JobThreads", 2);
            jobThreadCount = CONFIG.getInt(MCRJobQueue.CONFIG_PREFIX + JOB_QUEUE.CONFIG_PREFIX_ADD + "JobThreads",
                jobThreadCount);

            ThreadFactory slaveFactory = new ThreadFactory() {
                AtomicInteger tNum = new AtomicInteger();

                ThreadGroup tg = new ThreadGroup("MCRJob slave job thread group");

                public Thread newThread(Runnable r) {
                    Thread t = new Thread(tg, r, getPreLabel() + "Slave#" + tNum.incrementAndGet());
                    return t;
                }
            };
            final AtomicInteger activeThreads = new AtomicInteger();
            final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
            ThreadPoolExecutor executor = new ThreadPoolExecutor(jobThreadCount, jobThreadCount, 1, TimeUnit.DAYS,
                workQueue,
                slaveFactory) {

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
            };

            jobServe = MCRProcessableFactory.newPool(executor, processableCollection);
            processableCollection.setProperty("running", running);

            LOGGER.info("JobMaster" + (MCRJobQueue.singleQueue ? "" : " for \"" + action.getName() + "\"") + " with "
                + jobThreadCount + " thread(s) is started");
            while (running) {
                try {
                    while (activeThreads.get() < jobThreadCount) {
                        runLock.lock();
                        try {
                            if (!running)
                                break;

                            EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
                            EntityTransaction transaction = em.getTransaction();

                            MCRJob job = null;
                            MCRJobAction action = null;
                            try {
                                transaction.begin();

                                job = JOB_QUEUE.poll();
                                processableCollection.setProperty("queue size", JOB_QUEUE.size());

                                if (job != null) {
                                    action = toMCRJobAction(job.getAction());

                                    if (action != null && !action.isActivated()) {
                                        job.setStatus(MCRJobStatus.NEW);
                                        job.setStart(null);
                                    }
                                }

                                transaction.commit();
                            } catch (RollbackException e) {
                                LOGGER.error("Error while getting next job.", e);
                                if (transaction != null) {
                                    try {
                                        transaction.rollback();
                                    } catch (RuntimeException re) {
                                        LOGGER.warn("Could not rollback transaction.", re);
                                    }
                                }
                            } finally {
                                em.close();
                            }
                            if (job != null && action != null && action.isActivated()
                                && !jobServe.getExecutor().isShutdown()) {
                                LOGGER.info("Creating:" + job);
                                jobServe.submit(new MCRJobThread(job));
                            } else {
                                try {
                                    synchronized (JOB_QUEUE) {
                                        if (running) {
                                            LOGGER.debug("No job in queue going to sleep");
                                            //fixes a race conditioned deadlock situation
                                            //do not wait longer than 60 sec. for a new MCRJob
                                            JOB_QUEUE.wait(60000);
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    LOGGER.error("Job thread was interrupted.", e);
                                }
                            }
                        } finally {
                            runLock.unlock();
                        }
                    } // while(activeThreads.get() < jobThreadCount)
                    if (activeThreads.get() < jobThreadCount)
                        try {
                            LOGGER.info("Waiting for a job to finish");
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            LOGGER.error("Job thread was interrupted.", e);
                        }
                } catch (PersistenceException e) {
                    LOGGER.warn("We have an database error, sleep and run later.", e);
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ie) {
                        LOGGER.error("Waiting for database was interrupted.", ie);
                    }
                } catch (Throwable e) {
                    LOGGER.error("Keep running while catching exceptions.", e);
                }
            } // while(running)
            processableCollection.setProperty("running", running);
        }
        LOGGER.info(getName() + " thread finished");
        MCRSessionMgr.releaseCurrentSession();
    }

    /**
     * stops transmitting {@link MCRJob} to {@link MCRJobThread} and prepares shutdown.
     */
    public void prepareClose() {
        LOGGER.info("Closing master thread");
        //signal master thread to stop now
        running = false;
        //Wake up, Neo!
        synchronized (JOB_QUEUE) {
            LOGGER.debug("Wake up queue");
            JOB_QUEUE.notifyAll();
        }
        runLock.lock();
        try {
            if (jobServe != null) {
                LOGGER.debug("Shutdown executor jobs.");
                jobServe.getExecutor().shutdown();
                try {
                    LOGGER.debug("Await termination of executor jobs.");
                    jobServe.getExecutor().awaitTermination(60, TimeUnit.SECONDS);
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
    public void close() {
        if (jobServe != null && !jobServe.getExecutor().isShutdown()) {
            LOGGER.info("We are in a hurry, closing service right now");
            jobServe.getExecutor().shutdownNow();
            try {
                jobServe.getExecutor().awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.debug("Could not wait  60 seconds...", e);
            }
        }
    }

    @Override
    public int getPriority() {
        return MCRShutdownHandler.Closeable.DEFAULT_PRIORITY - 1;
    }

    protected String getPreLabel() {
        return (MCRJobQueue.singleQueue ? "Job" : action.getSimpleName());
    }

    /**
     * Returns the name of this job master.
     * 
     * @return
     */
    public String getName() {
        return getPreLabel() + " Master";
    }

    /**
     * Returns the processable collection assigned to this job master.
     * 
     * @return the processable collection
     */
    public MCRProcessableCollection getProcessableCollection() {
        return processableCollection;
    }

    private static MCRJobAction toMCRJobAction(Class<? extends MCRJobAction> actionClass) {
        try {
            Constructor<? extends MCRJobAction> actionConstructor = actionClass.getConstructor();
            MCRJobAction action = actionConstructor.newInstance();

            return action;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return null;
    }
}

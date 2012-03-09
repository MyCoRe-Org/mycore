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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;

/**
 * The master of all {@link MCRJobThread}s threads.
 * 
 * @author Ren\u00E9 Adler
 */
public class MCRJobMaster implements Runnable, Closeable {
    private static final SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static MCRJobMaster INSTANCE = null;

    private static Logger LOGGER = Logger.getLogger(MCRJobMaster.class);

    private static MCRJobQueue JOB_QUEUE = MCRJobQueue.getInstance();

    private ThreadPoolExecutor jobServe;

    private volatile boolean running = true;

    private ReentrantLock runLock;

    private MCRJobMaster() {
        MCRShutdownHandler.getInstance().addCloseable(this);
        runLock = new ReentrantLock();
    }

    /**
     * Returns an instance of this class.
     * 
     * @return the instance of the class
     */
    public static MCRJobMaster getInstance() {
        if (INSTANCE == null)
            INSTANCE = new MCRJobMaster();

        return INSTANCE;
    }

    /**
     * Return if {@link MCRJobMaster} is running.
     * 
     * @return if is running
     */
    public static boolean isRunning() {
        return INSTANCE != null;
    }

    /**
     * Starts the local {@link MCRJobMaster}. Can be auto started if <code>"MCR.QueuedJob.autostart"</code> 
     * is set to </code>true</code>.
     */
    public static void startMasterThread() {
        if (!isRunning()) {
            LOGGER.info("Starting job master thread.");
            final Thread master = new Thread(getInstance());
            master.start();
        }
    }

    /**
     * Starts local threads ({@link MCRJobThread}) and gives {@link MCRJob} instances to them.
     * Use property <code>"MCR.QueuedJob.JobThreads"</code> to specify how many concurrent threads should be running.<br />
     * <code>"MCR.QueuedJob.activated"</code> can be used activate or deactivate general {@link MCRJob} running. 
     */
    @Override
    public void run() {
        Thread.currentThread().setName("JobMaster");
        //get this MCRSession a speaking name
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());

        boolean activated = CONFIG.getBoolean(MCRJobQueue.CONFIG_PREFIX + "activated", true);
        LOGGER.info("JobQueue is " + (activated ? "activated" : "deactivated"));
        if (activated) {
            running = true;

            int jobThreadCount = CONFIG.getInt(MCRJobQueue.CONFIG_PREFIX + "JobThreads", 1);
            ThreadFactory slaveFactory = new ThreadFactory() {
                AtomicInteger tNum = new AtomicInteger();

                ThreadGroup tg = new ThreadGroup("MCRJob slave job thread group");

                public Thread newThread(Runnable r) {
                    Thread t = new Thread(tg, r, "JobSlave#" + tNum.incrementAndGet());
                    return t;
                }
            };
            final AtomicInteger activeThreads = new AtomicInteger();
            final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
            jobServe = new ThreadPoolExecutor(jobThreadCount, jobThreadCount, 1, TimeUnit.DAYS, workQueue, slaveFactory) {

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
            LOGGER.info("JobMaster is started");
            while (running) {
                while (activeThreads.get() < jobThreadCount) {
                    runLock.lock();
                    try {
                        if (!running)
                            break;

                        Session session = sessionFactory.getCurrentSession();
                        Transaction transaction = session.beginTransaction();
                        MCRJob job = null;
                        MCRJobAction action = null;
                        try {
                            job = JOB_QUEUE.poll();

                            if (job != null) {
                                action = toMCRJobAction(job.getAction());

                                if (action != null && !action.isActivated()) {
                                    job.setStatus(MCRJob.Status.NEW);
                                    job.setStart(null);
                                }
                            }

                            transaction.commit();
                        } catch (HibernateException e) {
                            LOGGER.error("Error while getting next job.", e);
                            if (transaction != null) {
                                transaction.rollback();
                            }
                        } finally {
                            session.close();
                        }
                        if (job != null && action != null && action.isActivated() && !jobServe.isShutdown()) {
                            LOGGER.info("Creating:" + job);
                            jobServe.execute(new MCRJobThread(job));
                        } else {
                            try {
                                synchronized (JOB_QUEUE) {
                                    if (running) {
                                        LOGGER.debug("No Job in JobQueue going to sleep");
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
            } // while(running)
        }
        LOGGER.info("Master thread finished");
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
                jobServe.shutdown();
                try {
                    LOGGER.debug("Await termination of executor jobs.");
                    jobServe.awaitTermination(60, TimeUnit.SECONDS);
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
        if (jobServe != null && !jobServe.isShutdown()) {
            LOGGER.info("We are in a hurry, closing service right now");
            jobServe.shutdownNow();
            try {
                jobServe.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.debug("Could not wait  60 seconds...", e);
            }
        }
    }

    @Override
    public int getPriority() {
        return MCRShutdownHandler.Closeable.DEFAULT_PRIORITY - 1;
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

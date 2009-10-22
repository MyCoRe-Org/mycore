package org.mycore.services.iview2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;

public class MCRImageTiling implements Runnable, Closeable {
    private static final SessionFactory sessionFactory = MCRHIBConnection.instance().getSessionFactory();

    private static MCRImageTiling instance = null;

    private static Logger LOGGER = Logger.getLogger(MCRImageTiling.class);

    private static MCRTilingQueue tq = MCRTilingQueue.getInstance();

    private ExecutorService tilingServe;

    private volatile boolean running = true;

    private ReentrantLock runLock;

    private MCRImageTiling() {
        MCRShutdownHandler.getInstance().addCloseable(this);
        runLock = new ReentrantLock();
        Thread.currentThread().setName("TileMaster");
    }

    public static boolean isRunning() {
        if (instance == null) {
            return false;
        } else {
            return true;
        }
    }

    public static MCRImageTiling getInstance() {
        if (instance == null) {
            instance = new MCRImageTiling();
        }
        return instance;
    }

    public void run() {
        tilingServe = Executors.newFixedThreadPool(Integer.parseInt(MCRIview2Props.getProperty("TilingThreads")), new ThreadFactory() {
            AtomicInteger tNum = new AtomicInteger();

            ThreadGroup tg = new ThreadGroup("MCR slave tiling thread group");

            public Thread newThread(Runnable r) {
                Thread t = new Thread(tg, r, "TileSlave#" + tNum.incrementAndGet());
                return t;
            }
        });

        LOGGER.info("TilingMaster is started");
        while (true) {
            runLock.lock();
            try {
                if (!running)
                    break;
                Session session = sessionFactory.getCurrentSession();
                Transaction transaction = session.beginTransaction();
                MCRTileJob job = null;
                try {
                    LOGGER.info("transaction is active: " + transaction.isActive());
                    job = tq.poll();
                    transaction.commit();
                } catch (HibernateException e) {
                    LOGGER.error("Error while getting next tiling job.", e);
                    if (transaction != null) {
                        transaction.rollback();
                    }
                } finally {
                    session.close();
                }
                if (job != null && !tilingServe.isShutdown()) {
                    LOGGER.info("Creating:" + job.getPath());
                    tilingServe.execute(new MCRImageTileThread(job));
                } else {
                    try {
                        synchronized (tq) {
                            if (running) {
                                LOGGER.info("No Picture in TilingQueue going to sleep");
                                //fixes a race conditioned deadlock situation
                                //do not wait longer than 60 sec. for a new MCRTileJob
                                tq.wait(60000);
                            }
                        }
                    } catch (InterruptedException e) {
                        LOGGER.error("Image Tiling thread was interrupted.", e);
                    }
                }
            } finally {
                runLock.unlock();
            }
        } // while(running)
    }

    public void prepareClose() {
        LOGGER.info("Closing master image tiling thread");
        //signal master thread to stop now
        running = false;
        //Wake up, Neo!
        synchronized (tq) {
            tq.notifyAll();
        }
        runLock.lock();
        try {
            if (tilingServe != null) {
                tilingServe.shutdown();
                try {
                    tilingServe.awaitTermination(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LOGGER.debug("Could not wait 60 seconds...", e);
                }
            }
        } finally {
            runLock.unlock();
        }
    }

    public void close() {
        if (tilingServe != null && !tilingServe.isShutdown()) {
            LOGGER.info("We are in a hurry, closing tiling service right now");
            tilingServe.shutdownNow();
            try {
                tilingServe.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.debug("Could not wait  60 seconds...", e);
            }
        }
    }
}

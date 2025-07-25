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

package org.mycore.iview2.services;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.common.processing.MCRProcessableDefaultCollection;
import org.mycore.common.processing.MCRProcessableManager;
import org.mycore.util.concurrent.processing.MCRProcessableExecutor;
import org.mycore.util.concurrent.processing.MCRProcessableFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;

/**
 * Master image tiler thread.
 *
 * @author Thomas Scheffler (yagee)
 */
public final class MCRImageTiler implements Runnable, Closeable {

    private static final Logger LOGGER = LogManager.getLogger();

    @SuppressWarnings("PMD.LooseCoupling")
    private static final MCRTilingQueue TQ = MCRTilingQueue.getInstance();

    private MCRProcessableExecutor tilingServe;

    private volatile boolean running = true;

    private final ReentrantLock runLock;

    private final Constructor<? extends MCRTilingAction> tilingActionConstructor;

    private volatile Thread waiter;

    private MCRImageTiler() {
        MCRShutdownHandler.getInstance().addCloseable(this);
        runLock = new ReentrantLock();
        try {
            Class<? extends MCRTilingAction> tilingActionImpl = MCRConfiguration2.<MCRTilingAction>getClass(
                MCRIView2Tools.CONFIG_PREFIX + "MCRTilingActionImpl").orElse(MCRTilingAction.class);
            tilingActionConstructor = tilingActionImpl.getConstructor(MCRTileJob.class);
        } catch (Exception e) {
            LOGGER.error("Error while initializing", e);
            throw new MCRException(e);
        }
    }

    /**
     * @return an instance of this class.
     */
    public static MCRImageTiler getInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    /**
     * Starts local tiler threads ( {@link MCRTilingAction}) and gives {@link MCRTileJob} instances to them. Use
     * property <code>MCR.Module-iview2.TilingThreads</code> to specify how many concurrent threads should be running.
     */
    @Override
    public void run() {
        waiter = Thread.currentThread();
        Thread.currentThread().setName("TileMaster");
        //get this MCRSession a speaking name
        MCRSessionMgr.unlock();
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        mcrSession.setUserInformation(MCRSystemUserInformation.SYSTEM_USER);
        boolean activated = MCRConfiguration2.getBoolean(MCRIView2Tools.CONFIG_PREFIX + "LocalTiler.activated")
            .orElse(true) && MCRConfiguration2.getBoolean("MCR.Persistence.Database.Enable").orElse(true)
            && MCREntityManagerProvider.getEntityManagerFactory() != null;
        LOGGER.info("Local Tiling is {}", () -> activated ? "activated" : "deactivated");
        ImageIO.scanForPlugins();
        LOGGER.info("Supported image file types for reading: {}",
            () -> Arrays.toString(ImageIO.getReaderFormatNames()));

        MCRProcessableDefaultCollection imageTilerCollection = new MCRProcessableDefaultCollection("Image Tiler");
        MCRProcessableManager.getInstance().getRegistry().register(imageTilerCollection);

        if (activated) {
            startTilingThreads(imageTilerCollection);
        }
        LOGGER.info("Tiling thread finished");
        MCRSessionMgr.releaseCurrentSession();
        waiter = null;
    }

    private void startTilingThreads(MCRProcessableDefaultCollection imageTilerCollection) {
        final AtomicInteger activeThreads = new AtomicInteger();
        int tilingThreadCount = Integer.parseInt(MCRIView2Tools.getIView2Property("TilingThreads"));
        ThreadFactory slaveFactory = new ThreadFactory() {
            AtomicInteger tNum = new AtomicInteger();

            @SuppressWarnings("PMD.AvoidThreadGroup") //no method is called on tg
            ThreadGroup tg = new ThreadGroup("MCR slave tiling thread group");

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(tg, r, "TileSlave#" + tNum.incrementAndGet());
            }
        };
        this.tilingServe = getProcessableExecutor(imageTilerCollection, tilingThreadCount, slaveFactory, activeThreads);
        imageTilerCollection.setProperty("running", running);
        LOGGER.info("TilingMaster is started");
        while (running) {
            try {
                while (activeThreads.get() < tilingThreadCount) {
                    runLock.lock();
                    try {
                        if (!running) {
                            break;
                        }
                        processNextTileJob(imageTilerCollection);
                    } finally {
                        runLock.unlock();
                    }
                } // while(tilingServe.getActiveCount() < tilingServe.getCorePoolSize())
                if (activeThreads.get() < tilingThreadCount) {
                    try {
                        LOGGER.info("Waiting for a tiling job to finish");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        if (running) {
                            LOGGER.error("Image Tiling thread was interrupted.", e);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Keep running while catching exceptions.", e);
            }
        } // while(running)
        imageTilerCollection.setProperty("running", false);
    }

    private void processNextTileJob(MCRProcessableDefaultCollection imageTilerCollection) {
        EntityTransaction transaction = null;
        MCRTileJob job = null;
        try (EntityManager em = MCREntityManagerProvider.getCurrentEntityManager()) {
            transaction = em.getTransaction();
            transaction.begin();
            job = TQ.poll();
            imageTilerCollection.setProperty("queue",
                TQ.stream().map(MCRTileJob::getPath).collect(Collectors.toList()));
            transaction.commit();
        } catch (PersistenceException e) {
            LOGGER.error("Error while getting next tiling job.", e);
            if (transaction != null) {
                try {
                    transaction.rollback();
                } catch (RuntimeException re) {
                    LOGGER.warn("Could not rollback transaction.", re);
                }
            }
        }
        if (job != null && !tilingServe.getExecutor().isShutdown()) {
            LOGGER.info("Creating:{}", job::getPath);
            tilingServe.submit(getTilingAction(job));
        } else {
            try {
                synchronized (TQ) {
                    if (running) {
                        LOGGER.debug("No Picture in TilingQueue going to sleep");
                        //fixes a race conditioned deadlock situation
                        //do not wait longer than 60 sec. for a new MCRTileJob
                        TQ.wait(60_000);
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.error("Image Tiling thread was interrupted.", e);
            }
        }
    }

    private static MCRProcessableExecutor getProcessableExecutor(
        MCRProcessableDefaultCollection imageTilerCollection, int tilingThreadCount, ThreadFactory slaveFactory,
        AtomicInteger activeThreads) {
        final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        ThreadPoolExecutor baseExecutor = new ThreadPoolExecutor(tilingThreadCount, tilingThreadCount, 1,
            TimeUnit.DAYS, workQueue, slaveFactory) {

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
        return MCRProcessableFactory.newPool(baseExecutor, imageTilerCollection);
    }

    private MCRTilingAction getTilingAction(MCRTileJob job) {
        try {
            return tilingActionConstructor.newInstance(job);
        } catch (Exception e) {
            throw new MCRException(e);
        }
    }

    /**
     * stops transmitting {@link MCRTileJob} to {@link MCRTilingAction} and prepares shutdown.
     */
    @Override
    public void prepareClose() {
        LOGGER.info("Closing master image tiling thread");
        //signal master thread to stop now
        running = false;
        //Wake up, Neo!
        synchronized (TQ) {
            LOGGER.debug("Wake up tiling queue");
            TQ.notifyAll();
        }
        runLock.lock();
        try {
            if (tilingServe != null) {
                LOGGER.debug("Shutdown tiling executor jobs.");
                tilingServe.getExecutor().shutdown();
                try {
                    LOGGER.debug("Await termination of tiling executor jobs.");
                    tilingServe.getExecutor().awaitTermination(60, TimeUnit.SECONDS);
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
     * Shuts down this thread and every local tiling threads spawned by {@link #run()}.
     */
    @Override
    public void close() {
        if (tilingServe != null && !tilingServe.getExecutor().isShutdown()) {
            LOGGER.info("We are in a hurry, closing tiling service right now");
            tilingServe.getExecutor().shutdownNow();
            try {
                tilingServe.getExecutor().awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.debug("Could not wait  60 seconds...", e);
            }
        }
        if (waiter != null && waiter.isAlive()) {
            //thread still running
            LOGGER.info("{} is still running.", waiter::getName);
            Thread masterThread = waiter;
            waiter = null;
            masterThread.interrupt();
            try {
                masterThread.join();
                LOGGER.info("{} has died.", masterThread::getName);
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    @Override
    public int getPriority() {
        return MCRShutdownHandler.Closeable.DEFAULT_PRIORITY - 1;
    }

    private static final class LazyInstanceHolder {
        public static final MCRImageTiler SINGLETON_INSTANCE = new MCRImageTiler();
    }

}

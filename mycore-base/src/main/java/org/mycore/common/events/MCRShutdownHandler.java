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

package org.mycore.common.events;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;

import se.jiderhamn.classloader.leak.prevention.ClassLoaderLeakPreventor;

/**
 * Is a wrapper for shutdown hooks. When used inside a web application this shutdown hook is bound to the
 * ServletContext. If not this hook is bound to the Java Runtime. Every <code>Closeable</code> that is added via
 * <code>addCloseable()</code> will be closed at shutdown time. Do not forget to remove any closeable via
 * <code>removeCloseable()</code> to remove any instances. For registering this hook for a web application see
 * <code>MCRServletContextListener</code>
 *
 * @author Thomas Scheffler (yagee)
 * @see org.mycore.common.events.MCRServletContextListener
 * @since 1.3
 */
public final class MCRShutdownHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int ADD_CLOSEABLE_TIMEOUT = 10;

    private static final String PROPERTY_SYSTEM_NAME = "MCR.CommandLineInterface.SystemName";

    private static final MCRShutdownHandler SINGLETON_INSTANCE = new MCRShutdownHandler();

    final NavigableSet<Closeable> requests = new ConcurrentSkipListSet<>();

    final ReentrantReadWriteLock shutdownLock = new ReentrantReadWriteLock();

    volatile boolean shuttingDown;

    private volatile boolean shutDown;
    
    private final AtomicBoolean isShutdownHookRegistered = new AtomicBoolean(false);

    boolean isWebAppRunning;
    
    ClassLoaderLeakPreventor leakPreventor;

    private MCRShutdownHandler() {
        isWebAppRunning = false;
    }

    private void init() {
        if (!isWebAppRunning && isShutdownHookRegistered.compareAndSet(false,  true)) {
            LOGGER.info("adding MyCoRe ShutdownHook");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                MCRShutdownHandler sh = getInstance();
                if (sh != null) {
                    sh.shutDown();
                }
            }, "MCR-exit"));
        }
    }

    public static MCRShutdownHandler getInstance() {
        if (SINGLETON_INSTANCE.shutDown) {
            return null;
        }
        return SINGLETON_INSTANCE;
    }

    public void addCloseable(MCRShutdownHandler.Closeable c) {
        Objects.requireNonNull(c);
        init();
        boolean hasShutDownLock;
        try {
            hasShutDownLock = shutdownLock.readLock().tryLock(ADD_CLOSEABLE_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new MCRException("Could not aquire shutdown lock in time", e);
        }
        try {
            if (hasShutDownLock && !shuttingDown) {
                requests.add(c);
            } else {
                throw new MCRException("Cannot register Closeable while shutting down application.");
            }
        } finally {
            if (hasShutDownLock) {
                shutdownLock.readLock().unlock();
            }
        }
    }

    public void removeCloseable(MCRShutdownHandler.Closeable c) {
        Objects.requireNonNull(c);
        if (!shuttingDown) {
            requests.remove(c);
        }
    }

    void shutDown() {
        String cfgSystemName = "MyCoRe:";
        try {
            cfgSystemName = MCRConfiguration2.getStringOrThrow(PROPERTY_SYSTEM_NAME) + ":";
        } catch (MCRConfigurationException e) {
            //may occur early if there is an error starting mycore up or in JUnit tests
            LOGGER.warn(() -> "Error getting '" + PROPERTY_SYSTEM_NAME + "': " + e.getMessage());
        }
        final String system = cfgSystemName;
        LOGGER.info("{} Shutting down system, please wait...", system);
        runClosables();
        LOGGER.info("{} closing any remaining MCRSession instances, please wait...", system);
        MCRSessionMgr.close();
        LOGGER.info("{} Goodbye, and remember: \"Alles wird gut.\"", system);
        LogManager.shutdown();
        shutDown = true;
        // may be needed in webapp to release file handles correctly.
        if (leakPreventor != null) {
            ClassLoaderLeakPreventor myLeakPreventor = leakPreventor;
            leakPreventor = null;
            myLeakPreventor.runCleanUps();
        }
    }

    void runClosables() {
        Logger logger = LogManager.getLogger();
        logger.debug(() -> "requests: " + requests);
        Closeable[] closeables = requests.toArray(Closeable[]::new);
        Stream.of(closeables)
            .peek(c -> logger.debug("Prepare Closing (1): {}", c))
            .forEach(Closeable::prepareClose); //may add more Closeables MCR-1726
        shutdownLock.writeLock().lock();
        try {
            shuttingDown = true;
            //during shut down more request may come in MCR-1726
            final List<Closeable> alreadyPrepared = Arrays.asList(closeables);
            requests.stream()
                .filter(c -> !alreadyPrepared.contains(c))
                .peek(c -> logger.debug("Prepare Closing (2): {}", c))
                .forEach(Closeable::prepareClose);

            requests.stream()
                .peek(c -> logger.debug("Closing: {}", c))
                .forEach(Closeable::close);
        } finally {
            shutdownLock.writeLock().unlock();
        }
    }

    /**
     * Object is cleanly closeable via <code>close()</code>-call.
     *
     * @author Thomas Scheffler (yagee)
     */
    @FunctionalInterface
    public interface Closeable extends Comparable<Closeable> {
        /**
         * The default priority
         */
        int DEFAULT_PRIORITY = 5;

        /**
         * prepare for closing this object that implements <code>Closeable</code>. This is the first part of the closing
         * process. As a object may need database access to close cleanly this method can be used to be ahead of
         * database outtake.
         */
        default void prepareClose() {
            //should be overwritten if needed;
        }

        /**
         * cleanly closes this object that implements <code>Closeable</code>. You can provide some functionality to
         * close open files and sockets or so.
         */
        void close();

        /**
         * Returns the priority. A Closeable with a higher priority will be closed before a Closeable with a lower
         * priority. Default priority is 5.
         */
        default int getPriority() {
            return DEFAULT_PRIORITY;
        }

        @Override
        default int compareTo(Closeable other) {
            //MCR-1941: never return 0 if !this.equals(other)
            return Comparator.comparingInt(Closeable::getPriority)
                .thenComparingLong(Closeable::hashCode)
                .compare(other, this);
        }
    }

}

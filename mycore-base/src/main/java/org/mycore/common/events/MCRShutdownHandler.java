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

package org.mycore.common.events;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;

import se.jiderhamn.classloader.leak.prevention.ClassLoaderLeakPreventor;

/**
 * is a wrapper for shutdown hooks. When used inside a web application this shutdown hook is bound to the
 * ServletContext. If not this hook is bound to the Java Runtime. Every <code>Closeable</code> that is added via
 * <code>addCloseable()</code> will be closed at shutdown time. Do not forget to remove any closeable via
 * <code>removeCloseable()</code> to remove any instances. For registering this hook for a web application see
 * <code>MCRServletContextListener</code>
 *
 * @author Thomas Scheffler (yagee)
 * @see org.mycore.common.events.MCRShutdownThread
 * @see org.mycore.common.events.MCRServletContextListener
 * @since 1.3
 */
public class MCRShutdownHandler {

    private static final int ADD_CLOSEABLE_TIMEOUT = 10;

    private static final String PROPERTY_SYSTEM_NAME = "MCR.CommandLineInterface.SystemName";

    private static MCRShutdownHandler SINGLETON = new MCRShutdownHandler();

    private final ConcurrentSkipListSet<Closeable> requests = new ConcurrentSkipListSet<>();

    private final ReentrantReadWriteLock shutdownLock = new ReentrantReadWriteLock();

    private volatile boolean shuttingDown = false;

    boolean isWebAppRunning;

    ClassLoaderLeakPreventor leakPreventor;

    private MCRShutdownHandler() {
        isWebAppRunning = false;
    }

    private void init() {
        if (!isWebAppRunning) {
            MCRShutdownThread.getInstance();
        }
    }

    public static MCRShutdownHandler getInstance() {
        return SINGLETON;
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
        Logger logger = LogManager.getLogger(MCRShutdownHandler.class);
        String cfgSystemName = "MyCoRe:";
        try {
            cfgSystemName = MCRConfiguration.instance().getString(PROPERTY_SYSTEM_NAME) + ":";
        } catch (MCRConfigurationException e) {
            //may occur early if there is an error starting mycore up or in JUnit tests
            logger.warn("Error getting '" + PROPERTY_SYSTEM_NAME + "': {}", e.getMessage());
        }
        final String system = cfgSystemName;
        System.out.println(system + " Shutting down system, please wait...\n");
        logger.debug(() -> "requests: " + requests);
        Closeable[] closeables = requests.stream().toArray(Closeable[]::new);
        Stream.of(closeables)
            .peek(c -> logger.debug("Prepare Closing (1): {}", c))
            .forEach(Closeable::prepareClose);
        shutdownLock.writeLock().lock();
        try {
            shuttingDown = true;
            //during shut down more request may come in MCR-1726
            requests.stream()
                .filter(c -> !Arrays.asList(closeables).contains(c))
                .peek(c -> logger.debug("Prepare Closing (2): {}", c))
                .forEach(Closeable::prepareClose);

            requests.stream()
                .peek(c -> logger.debug("Closing: {}", c))
                .forEach(Closeable::close);

            System.out.println(system + " closing any remaining MCRSession instances, please wait...\n");
            MCRSessionMgr.close();
            System.out.println(system + " Goodbye, and remember: \"Alles wird gut.\"\n");
            LogManager.shutdown();
            SINGLETON = null;
        } finally {
            shutdownLock.writeLock().unlock();
        }
        // may be needed in webapp to release file handles correctly.
        if (leakPreventor != null) {
            ClassLoaderLeakPreventor myLeakPreventor = leakPreventor;
            leakPreventor = null;
            myLeakPreventor.contextDestroyed(null);
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
            return Integer.compare(other.getPriority(), getPriority());
        }
    }

}

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.NavigableSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MCRShutdownHandlerTest {
    private MCRShutdownHandlerState mark;

    /**
     * Marks the current state of the shutdown handler.
     * This method saves the current shuttingDown value and {@link MCRShutdownHandler.Closeable} requests fields.
     * <p>
     * <b>Warning:</b> It should be used only in unit tests to test this class while keeping a clean state.
     */
    @Before
    public void saveClosables() {
        this.mark = new MCRShutdownHandlerState(MCRShutdownHandler.getInstance().shuttingDown,
            MCRShutdownHandler.getInstance().requests);
    }

    /**
     * Resets the {@link MCRShutdownHandler.Closeable} requests in the shutdown handler.
     * This method checks if there is a previous state ({@link #saveClosables()}) and, if present, resets the shuttingDown
     * and requests fields to their values from the mark.
     * The operation is performed under a write lock to ensure thread safety.
     * <p>
     * <b>Warning:</b> It should be used only in unit tests to test this class while keeping a clean state.
     */
    @After
    public void resetCloseables() {
        Optional.ofNullable(mark)
            .ifPresent(state -> {
                MCRShutdownHandler shutdownHandler = MCRShutdownHandler.getInstance();
                shutdownHandler.shutdownLock.writeLock().lock();
                try {
                    shutdownHandler.shuttingDown = state.shuttingDown;
                    shutdownHandler.requests.clear();
                    shutdownHandler.requests.addAll(state.requests);
                } finally {
                    shutdownHandler.shutdownLock.writeLock().unlock();
                }
            });
    }

    @Test
    public void runCloseables() {
        final MCRShutdownHandler shutdownHandler = MCRShutdownHandler.getInstance();
        AtomicBoolean lowClosed = new AtomicBoolean(false);
        AtomicBoolean hiClosed = new AtomicBoolean(false);
        AtomicBoolean vHiClosed = new AtomicBoolean(false);
        MCRShutdownHandler.Closeable vHiPrio = new MCRShutdownHandler.Closeable() {
            @Override
            public int getPriority() {
                return Integer.MAX_VALUE;
            }

            @Override
            public void close() {
                System.out.println("Closing very hi prio");
                assertFalse("Low priority closeable was closed to early.", lowClosed.get());
                assertFalse("High priority closeable was closed to early.", hiClosed.get());
                vHiClosed.set(true);
            }

            @Override
            public String toString() {
                return "very high";
            }
        };
        MCRShutdownHandler.Closeable lowPrio = new MCRShutdownHandler.Closeable() {
            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 5;
            }

            @Override
            public void close() {
                System.out.println("Closing low prio");
                assertTrue("High priority closeable is not closed.", hiClosed.get());
                assertTrue("Very high priority closeable is not closed.", vHiClosed.get());
                lowClosed.set(true);
            }

            @Override
            public String toString() {
                return "low";
            }
        };
        MCRShutdownHandler.Closeable hiPrio = new MCRShutdownHandler.Closeable() {
            @Override
            public int getPriority() {
                return Integer.MIN_VALUE + 10;
            }

            @Override
            public void close() {
                System.out.println("Closing hi prio");
                assertTrue("Very high priority closeable is not closed.", vHiClosed.get());
                assertFalse("Low priority closeable was closed to early.", lowClosed.get());
                hiClosed.set(true);
            }

            @Override
            public String toString() {
                return "high";
            }
        };
        shutdownHandler.addCloseable(vHiPrio);
        shutdownHandler.addCloseable(lowPrio);
        shutdownHandler.addCloseable(hiPrio);
        shutdownHandler.runClosables();
        assertTrue(hiClosed.get() && lowClosed.get());
    }

    /**
     * The MCRShutdownHandlerState class represents the state of a shutdown handler.
     * It only used by {@link #mark} and {@link #resetCloseables()}.
     */
    private record MCRShutdownHandlerState(boolean shuttingDown, NavigableSet<MCRShutdownHandler.Closeable> requests) {

        /**
         * Constructs an instance of MCRShutdownHandlerState with the given parameters.
         * The requests field is initialized as a new ConcurrentSkipListSet containing the provided Closeables.
         * @param shuttingDown A boolean indicating whether the system is currently in the process of shutting down.
         * @param requests A set of Closeable objects representing the current shutdown requests.
         */
        MCRShutdownHandlerState {
            requests = new ConcurrentSkipListSet<>(requests);
        }
    }

}

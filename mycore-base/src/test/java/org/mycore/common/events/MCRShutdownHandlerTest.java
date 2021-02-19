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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class MCRShutdownHandlerTest {

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

}

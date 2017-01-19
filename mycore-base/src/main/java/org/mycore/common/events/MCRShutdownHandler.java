/**
 * $Revision$ $Date$ This
 * file is part of ** M y C o R e ** Visit our homepage at http://www.mycore.de/
 * for details. This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version. This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program, normally in the file
 * license.txt. If not, write to the Free Software Foundation Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307 USA
 **/
package org.mycore.common.events;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final String PROPERTY_SYSTEM_NAME = "MCR.CommandLineInterface.SystemName";

    /**
     * Object is cleanly closeable via <code>close()</code>-call.
     * 
     * @author Thomas Scheffler (yagee)
     */
    @FunctionalInterface
    public static interface Closeable {
        /**
         * prepare for closing this object that implements <code>Closeable</code>. This is the first part of the closing
         * process. As a object may need database access to close cleanly this method can be used to be ahead of
         * database outtake.
         */
        default void prepareClose(){
            //should be overwritten if needed;
        }

        /**
         * cleanly closes this object that implements <code>Closeable</code>. You can provide some functionality to
         * close open files and sockets or so.
         */
        public void close();

        /**
         * Returns the priority. A Closeable with a higher priority will be closed before a Closeable with a lower
         * priority. Default priority is 5.
         */
        default int getPriority(){
            return DEFAULT_PRIORITY;
        }

        /**
         * The default priority
         */
        public static int DEFAULT_PRIORITY = 5;
    }

    private static MCRShutdownHandler SINGLETON = new MCRShutdownHandler();

    private static final Set<Closeable> requests = Collections.synchronizedSet(new HashSet<Closeable>());

    private static boolean shuttingDown = false;

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
        init();
        requests.add(c);
    }

    public void removeCloseable(MCRShutdownHandler.Closeable c) {
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
            logger.warn("Error getting '" + PROPERTY_SYSTEM_NAME + "': " + e.getMessage());
        }
        final String system = cfgSystemName;
        System.out.println(system + " Shutting down system, please wait...\n");
        logger.debug("requests: " + requests.toString());
        synchronized (requests) {
            shuttingDown = true;
            Closeable[] closeables = requests.toArray(new Closeable[requests.size()]);
            Arrays.sort(closeables, new MCRCloseableComparator());
            for (Closeable c : closeables) {
                logger.debug("Prepare Closing: " + c.toString());
                c.prepareClose();
            }

            for (Closeable c : closeables) {
                logger.debug("Closing: " + c.toString());
                c.close();
                requests.remove(c);
            }
        }
        System.out.println(system + " closing any remaining MCRSession instances, please wait...\n");
        MCRSessionMgr.close();
        System.out.println(system + " Goodbye, and remember: \"Alles wird gut.\"\n");
        LogManager.shutdown();
        SINGLETON = null;
        // may be needed in webapp to release file handles correctly.
        if (leakPreventor != null) {
            ClassLoaderLeakPreventor myLeakPreventor = leakPreventor;
            leakPreventor = null;
            myLeakPreventor.contextDestroyed(null);
        }
    }

}

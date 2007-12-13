/**
 * $RCSfile$
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
package org.mycore.common.events;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;

/**
 * is a wrapper for shutdown hooks.
 * 
 * When used inside a web application this shutdown hook is bound to the
 * ServletContext. If not this hook is bound to the Java Runtime.
 * 
 * Every <code>Closeable</code> that is added via <code>addCloseable()</code>
 * will be closed at shutdown time. Do not forget to remove any closeable via
 * <code>removeCloseable()</code> to remove any instances.
 * 
 * For registering this hook for a web application see <code>MCRServletContextListener</code>
 * 
 * @author Thomas Scheffler (yagee)
 * 
 * @see org.mycore.common.events.MCRShutdownThread
 * @see org.mycore.common.events.MCRServletContextListener
 * @since 1.3
 */
public class MCRShutdownHandler {

    /**
     * Object is cleanly closeable via <code>close()</code>-call.
     * 
     * @author Thomas Scheffler (yagee)
     */
    public static interface Closeable {
        /**
         * cleanly closes this object that implements <code>Closeable</code>.
         * 
         * You can provide some functionality to close open files and sockets or
         * so.
         */
        public void close();
    }

    private static MCRShutdownHandler SINGLETON = new MCRShutdownHandler();

    private static Logger LOGGER = Logger.getLogger(MCRShutdownHandler.class);

    private static final Set requests = Collections.synchronizedSet(new HashSet());
    
    private static final String system = MCRConfiguration.instance().getString("MCR.CommandLineInterface.SystemName", "MyCoRe") + ":";

    private static boolean shuttingDown = false;

    boolean isWebAppRunning;

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
        if (!shuttingDown)
            requests.remove(c);
    }

    void shutDown() {
        System.out.println(system+" Shutting down system, please wait...\n");
        LOGGER.debug("requests: " + requests.toString());
        synchronized (requests) {
            shuttingDown = true;
            for (Iterator it = requests.iterator(); it.hasNext();) {
                MCRShutdownHandler.Closeable c = (MCRShutdownHandler.Closeable) it.next();
                LOGGER.debug("Closing: " + c.toString());
                c.close();
                it.remove();
            }
        }
        System.out.println(system+" closing any remaining MCRSession instances, please wait...\n");
        MCRSessionMgr.close();
        System.out.println(system + " Goodbye, and remember: \"Alles wird gut.\"\n");
    }

}
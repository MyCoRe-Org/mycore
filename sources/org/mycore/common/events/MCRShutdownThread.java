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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * is a shutdown hook for the current <code>Runtime</code>.
 * 
 * This class registers itself as a shutdown hook to the JVM. Every
 * <code>Closeable</code> that is added via <code>addCloseable()</code> will
 * be closed at shutdown time. Do not forget to remove any closeable via
 * <code>removeCloseable()</code> to remove any instances.
 * 
 * There is no way to instanciate this class somehow. This will be done on first access.
 * 
 * @author Thomas Scheffler (yagee)
 * @see java.lang.Runtime#addShutdownHook(java.lang.Thread)
 * @see MCRShutdownThread.Closeable
 */
public class MCRShutdownThread extends Thread {

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

    private static Set requests = new HashSet();

    private static Logger LOGGER = Logger.getLogger(MCRShutdownThread.class);

    static {
        new MCRShutdownThread();
    }

    private static boolean shuttingDown = false;

    private MCRShutdownThread() {
        this.setName("MCR-exit");
        LOGGER.info("adding MyCoRe ShutdownHook");
        Runtime.getRuntime().addShutdownHook(this);
    }

    public static void addCloseable(MCRShutdownThread.Closeable c) {
        requests.add(c);
    }

    public static void removeCloseable(MCRShutdownThread.Closeable c) {
        if (!shuttingDown)
            requests.remove(c);
    }

    public void run() {
        LOGGER.info(toString());
        synchronized (requests) {
            shuttingDown = true;
            for (Iterator it = requests.iterator(); it.hasNext();) {
                MCRShutdownThread.Closeable c = (MCRShutdownThread.Closeable) it.next();
                LOGGER.debug("Closing: " + c.toString());
                c.close();
                it.remove();
            }
        }
    }
    
    public String toString(){
        return "requests: "+requests; 
    }
}
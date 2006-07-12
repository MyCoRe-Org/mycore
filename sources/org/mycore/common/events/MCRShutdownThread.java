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

import org.apache.log4j.Logger;

/**
 * is a shutdown hook for the current <code>Runtime</code>.
 * 
 * This class registers itself as a shutdown hook to the JVM. 
 * 
 * There is no way to instanciate this class somehow. This will be done by MCRShutdownHandler.
 * 
 * @author Thomas Scheffler (yagee)
 * @see java.lang.Runtime#addShutdownHook(java.lang.Thread)
 * @see org.mycore.common.events.MCRShutdownHandler
 * @since 1.3
 */
public class MCRShutdownThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(MCRShutdownThread.class);

    private static final MCRShutdownThread SINGLETON = new MCRShutdownThread();

    private MCRShutdownThread() {
        this.setName("MCR-exit");
        LOGGER.info("adding MyCoRe ShutdownHook");
        Runtime.getRuntime().addShutdownHook(this);
    }

    static MCRShutdownThread getInstance() {
        return SINGLETON;
    }

    public void run() {
        MCRShutdownHandler.getInstance().shutDown();
    }
}
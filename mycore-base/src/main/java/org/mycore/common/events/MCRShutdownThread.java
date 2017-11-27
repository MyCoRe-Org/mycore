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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger LOGGER = LogManager.getLogger(MCRShutdownThread.class);

    private static final MCRShutdownThread SINGLETON = new MCRShutdownThread();

    private MCRShutdownThread() {
        setName("MCR-exit");
        LOGGER.info("adding MyCoRe ShutdownHook");
        Runtime.getRuntime().addShutdownHook(this);
    }

    static MCRShutdownThread getInstance() {
        return SINGLETON;
    }

    @Override
    public void run() {
        MCRShutdownHandler sh = MCRShutdownHandler.getInstance();
        if (sh != null) {
            sh.shutDown();
        }
    }
}

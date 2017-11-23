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

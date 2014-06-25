/**
 * 
 */
package org.mycore.handle;

import java.util.Timer;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRShutdownHandler.Closeable;
import org.mycore.common.events.MCRStartupHandler.AutoExecutable;

/**
 * @author shermann
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRHandleServiceStarter implements AutoExecutable, Closeable {
    private static final Logger LOGGER = Logger.getLogger(MCRHandleServiceStarter.class);

    private Timer handleAddTimer;

    private Timer handleGetTimer;

    @Override
    public String getName() {
        return "Handle Service";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext != null) {
            LOGGER.info("Starting " + MCRRequestHandleAdd.class.getSimpleName() + " service");
            handleAddTimer = new Timer("HandleAddRequester");
            handleAddTimer.scheduleAtFixedRate(MCRRequestHandleAdd.getInstance(), 30 * 1000, 60 * 1000);

            LOGGER.info("Starting " + MCRGetRemoteCreatedHandle.class.getSimpleName() + " service");
            handleGetTimer = new Timer("HandleGetRequester");
            handleGetTimer.scheduleAtFixedRate(MCRGetRemoteCreatedHandle.getInstance(), 30 * 1000, 60 * 1000);
            MCRShutdownHandler.getInstance().addCloseable(this);
        }
    }

    @Override
    public void prepareClose() {
        handleAddTimer.cancel();
        handleGetTimer.cancel();
    }

    @Override
    public void close() {
        handleAddTimer.purge();
        handleGetTimer.purge();
    }
}

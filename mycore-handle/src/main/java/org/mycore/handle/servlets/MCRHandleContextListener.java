/**
 * 
 */
package org.mycore.handle.servlets;

import java.util.Timer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.mycore.handle.MCRGetRemoteCreatedHandle;
import org.mycore.handle.MCRRequestHandleAdd;

/**
 * @author shermann
 *
 */
public class MCRHandleContextListener implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(MCRHandleContextListener.class);

    private Timer handleAddTimer;

    private Timer handleGetTimer;

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        handleAddTimer.cancel();
        handleGetTimer.cancel();
        handleAddTimer.purge();
        handleGetTimer.purge();
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Starting " + MCRRequestHandleAdd.class.getSimpleName() + " service");
        handleAddTimer = new Timer("HandleAddRequester");
        handleAddTimer.scheduleAtFixedRate(MCRRequestHandleAdd.getInstance(), 30 * 1000, 60 * 1000);

        LOGGER.info("Starting " + MCRGetRemoteCreatedHandle.class.getSimpleName() + " service");
        handleGetTimer = new Timer("HandleGetRequester");
        handleGetTimer.scheduleAtFixedRate(MCRGetRemoteCreatedHandle.getInstance(), 30 * 1000, 60 * 1000);
    }
}

/**
 * 
 */
package org.mycore.services.handle.servlets;

import java.util.Timer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.mycore.services.handle.MCRGetRemoteCreatedHandle;
import org.mycore.services.handle.MCRRequestHandleAdd;


/**
 * @author shermann
 *
 */
public class MCRHandleContextListener implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(MCRHandleContextListener.class);

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Starting " + MCRRequestHandleAdd.class.getSimpleName() + " service");
        new Timer("HandleAddRequester").scheduleAtFixedRate(MCRRequestHandleAdd.getInstance(), 30 * 1000, 60 * 1000);

        LOGGER.info("Starting " + MCRGetRemoteCreatedHandle.class.getSimpleName() + " service");
        new Timer("HandleGetRequester").scheduleAtFixedRate(MCRGetRemoteCreatedHandle.getInstance(), 30 * 1000, 60 * 1000);
    }
}

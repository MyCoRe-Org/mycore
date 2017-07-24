/**
 * 
 */
package org.mycore.urn.rest;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRStartupHandler;

/**
 * @author shermann
 *
 */
@Deprecated
public class URNRegistrationServiceStarter implements MCRStartupHandler.AutoExecutable, MCRShutdownHandler.Closeable {

    private static final Logger LOGGER = LogManager.getLogger(URNRegistrationServiceStarter.class);

    private ScheduledExecutorService scheduler;

    @Override
    public String getName() {
        return "URN Registration Service";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext != null) {
            MCRShutdownHandler.getInstance().addCloseable(this);
            String s1 = MCRConfiguration.instance().getString("MCR.URN.DNB.Credentials.Login", null);
            String s2 = MCRConfiguration.instance().getString("MCR.URN.DNB.Credentials.Password", null);

            if (s1 == null || s2 == null || s1.length() == 0 || s2.length() == 0) {
                LOGGER.warn("Could not instantiate " + URNRegistrationService.class.getName()
                    + " as required credentials are unset");
                LOGGER.warn("Please set MCR.URN.DNB.Credentials.Login and MCR.URN.DNB.Credentials.Password");
                return;
            }
            //urn service
            URNRegistrationService urnRegistrationService = null;
            DFGURNRegistrationService dfgURNRegistrationService = null;
            boolean supportDfgViewerURN = MCRConfiguration.instance().getBoolean("MCR.URN.Display.DFG.Viewer.URN",
                false);
            try {
                urnRegistrationService = new URNRegistrationService();
                if (supportDfgViewerURN) {
                    dfgURNRegistrationService = new DFGURNRegistrationService();
                }

            } catch (Exception e) {
                LOGGER.error("Could not instantiate " + URNRegistrationService.class.getName(), e);
                return;

            }
            LOGGER.info("Starting executor service...");
            scheduler = Executors.newSingleThreadScheduledExecutor();

            LOGGER.info("Starting " + URNRegistrationService.class.getSimpleName());
            // refresh every 60 seconds
            scheduler.scheduleAtFixedRate(urnRegistrationService, 0, 1, TimeUnit.MINUTES);

            if (dfgURNRegistrationService != null) {
                LOGGER.info("Starting " + DFGURNRegistrationService.class.getSimpleName());
                // refresh every 60 seconds
                scheduler.scheduleAtFixedRate(dfgURNRegistrationService, 0, 1, TimeUnit.MINUTES);
            }
        }
    }

    @Override
    public void prepareClose() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public void close() {
        if (scheduler != null && !scheduler.isShutdown()) {
            try {
                scheduler.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            scheduler.shutdownNow();
        }
    }
}

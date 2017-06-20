package org.mycore.webcli.resources;

import javax.ws.rs.container.ContainerRequestContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
public class MCRWebCLIPermission implements MCRResourceAccessChecker {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean isPermitted(ContainerRequestContext request) {
        if (!MCRAccessManager.getAccessImpl().checkPermission("use-webcli")) {
            LOGGER.info("Permission denied on MCRWebCLI");
            return false;
        }
        return true;
    }

}

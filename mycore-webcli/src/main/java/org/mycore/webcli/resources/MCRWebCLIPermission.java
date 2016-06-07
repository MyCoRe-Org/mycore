package org.mycore.webcli.resources;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

import com.sun.jersey.spi.container.ContainerRequest;

/**
 * @author Michel Buechner (mcrmibue)
 * 
 */
public class MCRWebCLIPermission implements MCRResourceAccessChecker {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    @Override
    public boolean isPermitted(ContainerRequest request) {
        if (!MCRAccessManager.getAccessImpl().checkPermission("use-webcli")) {
            LOGGER.info("Permission denied on MCRWebCLI");
            return false;
        }
        return true;
    }

}

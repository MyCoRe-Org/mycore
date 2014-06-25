package org.mycore.frontend.acl2.resources;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

import com.sun.jersey.spi.container.ContainerRequest;

public class MCRAclEditorPermission implements MCRResourceAccessChecker {

    private static Logger LOGGER = Logger.getLogger(MCRAclEditorPermission.class);

    @Override
    public boolean isPermitted(ContainerRequest request) {
        if (!MCRAccessManager.getAccessImpl().checkPermission("use-aclEditor")) {
            LOGGER.info("Permission denied on MCRAclEditor");
            return false;
        }
        return true;
    }
}

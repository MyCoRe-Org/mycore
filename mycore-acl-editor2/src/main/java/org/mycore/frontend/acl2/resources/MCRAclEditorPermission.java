package org.mycore.frontend.acl2.resources;

import javax.ws.rs.container.ContainerRequestContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

public class MCRAclEditorPermission implements MCRResourceAccessChecker {

    private static Logger LOGGER = LogManager.getLogger(MCRAclEditorPermission.class);

    @Override
    public boolean isPermitted(ContainerRequestContext request) {
        if (!MCRAccessManager.getAccessImpl().checkPermission("use-aclEditor")) {
            LOGGER.info("Permission denied on MCRAclEditor");
            return false;
        }
        return true;
    }
}

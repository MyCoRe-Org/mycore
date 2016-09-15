package org.mycore.wcms2.access;

import javax.ws.rs.container.ContainerRequestContext;

import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

public class MCRWCMSPermission implements MCRResourceAccessChecker {

    @Override
    public boolean isPermitted(ContainerRequestContext request) {
        return MCRAccessManager.checkPermission("use-wcms");
    }

}

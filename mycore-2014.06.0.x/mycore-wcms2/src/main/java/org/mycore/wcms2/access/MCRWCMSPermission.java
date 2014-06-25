package org.mycore.wcms2.access;

import org.mycore.access.MCRAccessManager;
import org.mycore.frontend.jersey.filter.access.MCRResourceAccessChecker;

import com.sun.jersey.spi.container.ContainerRequest;

public class MCRWCMSPermission implements MCRResourceAccessChecker {

    @Override
    public boolean isPermitted(ContainerRequest request) {
        return MCRAccessManager.checkPermission("use-wcms");
    }

}

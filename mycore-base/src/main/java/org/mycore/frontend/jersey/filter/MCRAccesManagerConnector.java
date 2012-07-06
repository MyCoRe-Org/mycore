package org.mycore.frontend.jersey.filter;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSession;
import org.mycore.frontend.jersey.filter.MCRSecurityFilterFactory.AccesManagerConnector;


public class MCRAccesManagerConnector implements AccesManagerConnector {
    @Override
    public boolean checkPermission(String resourceName, String resourceOperation, MCRSession session) {
        boolean hasPermission = MCRAccessManager.checkPermission(resourceName, resourceOperation);
        return hasPermission;
    }
}
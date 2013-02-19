package org.mycore.frontend.jersey.filter;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSession;

@Deprecated
public class MCRAccessManagerConnector {
    public boolean checkPermission(String resourceName, String resourceOperation, MCRSession session) {
        boolean hasPermission = MCRAccessManager.checkPermission(resourceName, resourceOperation);
        return hasPermission;
    }
}

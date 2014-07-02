package org.mycore.iview2.frontend;

import javax.servlet.http.HttpSession;

import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRIviewDefaultACLProvider implements MCRIviewACLProvider {

    @Override
    public boolean checkAccess(HttpSession session, MCRObjectID derivateID) {
        return MCRAccessManager.checkPermission(derivateID.toString(), "read");
    }

}

package org.mycore.viewer.configuration;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.mycore.access.MCRAccessManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRIviewDefaultACLProvider implements MCRIviewACLProvider {

    @Override
    public boolean checkAccess(HttpSession session, MCRObjectID derivateID) {
        if (MCRAccessManager.checkPermission(derivateID, "read")) {
            return true;
        }
        MCRObjectID objectId = MCRMetadataManager.getObjectId(derivateID, 10, TimeUnit.MINUTES);
        return MCRAccessManager.checkPermission(objectId, "view-derivate");
    }

}

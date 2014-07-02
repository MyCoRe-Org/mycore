package org.mycore.iview2.frontend;

import javax.servlet.http.HttpSession;

import org.mycore.datamodel.metadata.MCRObjectID;

public interface MCRIviewACLProvider {
    public boolean checkAccess(final HttpSession session,final MCRObjectID derivateID);
}

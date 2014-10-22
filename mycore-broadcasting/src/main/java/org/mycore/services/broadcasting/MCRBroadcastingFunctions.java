package org.mycore.services.broadcasting;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public abstract class MCRBroadcastingFunctions {

    public static String hasReceived(String sessionSensitive) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        boolean hasReceived = MCRBroadcastingServlet.hasReceived(session, "true".equalsIgnoreCase(sessionSensitive));
        return Boolean.valueOf(hasReceived).toString();
    }

}

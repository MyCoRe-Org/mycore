package org.mycore.orcid.user;

import java.io.IOException;

import org.jdom2.JDOMException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;
import org.xml.sax.SAXException;

public class MCRORCIDSession {

    private static final String KEY_ORCID_USER = "ORCID_USER";

    private static MCRORCIDUser setCurrentUser() {
        MCRUser user = MCRUserManager.getCurrentUser();
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);
        MCRSessionMgr.getCurrentSession().put(KEY_ORCID_USER, orcidUser);
        return orcidUser;
    }

    public static MCRORCIDUser getCurrentUser() {
        MCRORCIDUser orcidUser = (MCRORCIDUser) MCRSessionMgr.getCurrentSession().get(KEY_ORCID_USER);
        return (orcidUser == null ? setCurrentUser() : orcidUser);
    }

    public static int getNumWorks() throws JDOMException, IOException, SAXException {
        return getCurrentUser().getProfile().getWorksSection().getWorks().size();
    }
}

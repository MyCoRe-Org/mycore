package org.mycore.orcid.user;

import java.io.IOException;

import org.jdom2.JDOMException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;
import org.xml.sax.SAXException;

public class MCRORCIDSession {

    private static final String KEY_ORCID_USER = "ORCID_USER";

    public static MCRORCIDUser getCurrentUser() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRUser user = MCRUserManager.getCurrentUser();

        MCRORCIDUser orcidUser = (MCRORCIDUser) session.get(KEY_ORCID_USER);
        if ((orcidUser == null) || !orcidUser.getUser().equals(user)) {
            orcidUser = new MCRORCIDUser(user);
            session.put(KEY_ORCID_USER, orcidUser);
        }
        return orcidUser;
    }

    public static int getNumWorks() throws JDOMException, IOException, SAXException {
        return getCurrentUser().getProfile().getWorksSection().getWorks().size();
    }
}

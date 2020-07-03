/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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

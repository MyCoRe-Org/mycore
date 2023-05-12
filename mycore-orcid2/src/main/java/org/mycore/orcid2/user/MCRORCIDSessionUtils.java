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

package org.mycore.orcid2.user;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Provides orcid session utilities.
 */
public class MCRORCIDSessionUtils {

    private static final String KEY_ORCID_USER = "ORCID_USER";

    /**
     * Initializes current user's MCRORCIDUser.
     *
     * @return current MCRUser as MCRORCIDUser
     */
    public static MCRORCIDUser getCurrentUser() {
        final MCRSession session = MCRSessionMgr.getCurrentSession();
        // refetch user because of rest issues
        final MCRUser user = MCRUserManager.getUser(MCRUserManager.getCurrentUser().getUserID());
        MCRORCIDUser orcidUser = (MCRORCIDUser) session.get(KEY_ORCID_USER);
        if ((orcidUser == null) || !orcidUser.getUser().equals(user)) {
            orcidUser = new MCRORCIDUser(user);
            session.put(KEY_ORCID_USER, orcidUser);
        }
        return orcidUser;
    }
}

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

import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.user2.MCRUserManager;

/**
 * Provides utility methods for MCRORCIDUser.
 */
public class MCRORCIDUserUtils {

    /**
     * Returns MCRORCIDUser for given orcid.
     * 
     * @param orcid the orcid
     * @return MCRORCIDUser or null
     */
    public static MCRORCIDUser getORCIDUser(String orcid) {
        return MCRUserManager.getUsers(MCRORCIDUser.ATTR_ORCID_ID, orcid).findFirst()
            .map(user -> new MCRORCIDUser(user)).orElse(null);
    }

    /**
     * Returns MCRORCIDCredentials for given orcid.
     * 
     * @param orcid the orcid
     * @return MCRORCIDCredentials or null
     * @throws MCRORCIDException if the credentials are corrupt
     */
    public static MCRORCIDCredentials getCredentials(String orcid) throws MCRORCIDException {
        final MCRORCIDUser user = getORCIDUser(orcid);
        if (user != null) {
            return user.getCredentials(orcid);
        }
        return null;
    }
}

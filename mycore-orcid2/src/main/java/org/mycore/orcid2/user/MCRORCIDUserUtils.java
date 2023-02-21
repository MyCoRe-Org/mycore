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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.mycore.orcid2.auth.MCRORCIDOAuthClient;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Provides utility methods for MCRORCIDUser.
 */
public class MCRORCIDUserUtils {

    /**
     * Returns MCRORCIDCredentials by ORCID iD.
     * 
     * @param orcid the ORCID iD
     * @return MCRORCIDCredentials or null
     * @throws MCRORCIDException if the credentials are corrupt or there is more than one user
     */
    public static MCRORCIDCredentials getCredentialsByORCID(String orcid) throws MCRORCIDException {
        return Optional.ofNullable(getORCIDUserByORCID(orcid)).map(u -> u.getCredentialsByORCID(orcid)).orElse(null);
    }

    /**
     * Returns MCRORCIDUser by ORCID iD.
     * 
     * @param orcid the ORCID iD
     * @return MCRORCIDUser or null
     * @throws MCRORCIDException if there is more than one matching user
     */
    public static MCRORCIDUser getORCIDUserByORCID(String orcid) throws MCRORCIDException {
        final Set<MCRUser> users
            = MCRUserManager.getUsers(MCRORCIDUser.ATTR_ORCID_ID, orcid).collect(Collectors.toSet());
        if (users.isEmpty()) {
            return null;
        } else if (users.size() == 1) {
            return new MCRORCIDUser(users.iterator().next());
        }
        throw new MCRORCIDException("Found more than one user for given orcid");
    }

    /**
     * Returns Set of MCRUser for given MCRIdentifier.
     * 
     * @param id the MCRIdentifier
     * @return Set of MCRUser
     */
    public static Set<MCRUser> getUserByID(MCRIdentifier id) {
        return MCRUserManager.getUsers(MCRORCIDUser.ATTR_ID_PREFIX + id.getType(), id.getValue())
            .collect(Collectors.toSet());
    }

    /**
     * Revokes orcid access token of MCRORCIDUser by ORCID iD.
     * 
     * @param orcidUser the MCRORCIDUser
     * @param orcid the ORCID iD
     * @throws MCRORCIDException if revoke request fails
     */
    public static void revokeCredentialsByORCID(MCRORCIDUser orcidUser, String orcid) throws MCRORCIDException {
        final MCRORCIDCredentials credentials = Optional.ofNullable(getCredentialsByORCID(orcid))
            .orElseThrow(() -> new MCRORCIDException("Credentials do not exist"));
        MCRORCIDOAuthClient.getInstance().revokeToken(credentials.getAccessToken());
        orcidUser.removeCredentialsByORCID(orcid);
    }
}

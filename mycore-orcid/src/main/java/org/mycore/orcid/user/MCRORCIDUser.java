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

import org.mycore.orcid.MCRORCIDProfile;
import org.mycore.orcid.oauth.MCRTokenResponse;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Provides functionality to interact with MCRUser that is also an ORCID user.
 * The user's ORCID iD and access token are stored as attributes.
 *
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRORCIDUser {

    private static final String ATTR_ORCID_ID = "ORCID";

    private static final String ATTR_ORCID_TOKEN = "ORCID-AccessToken";

    private MCRUser user;

    private MCRORCIDProfile profile;

    public MCRORCIDUser(MCRUser user) {
        this.user = user;
    }

    /** Called from MCROAuthServlet to store the user's ORCID iD and token after successful OAuth authorization */
    public void store(MCRTokenResponse token) {
        user.getAttributes().put(ATTR_ORCID_ID, token.getORCID());
        user.getAttributes().put(ATTR_ORCID_TOKEN, token.getAccessToken());
        MCRUserManager.updateUser(user);
    }

    public boolean hasORCIDProfile() {
        return user.getUserAttribute(ATTR_ORCID_ID) != null;
    }

    /**
     * Returns true, if there is an access token stored for this user.
     */
    public boolean weAreTrustedParty() {
        return user.getUserAttribute(ATTR_ORCID_TOKEN) != null;
    }

    public MCRORCIDProfile getORCIDProfile() {
        if (!hasORCIDProfile()) {
            return null;
        }

        if (profile == null) {
            String orcid = user.getUserAttribute(ATTR_ORCID_ID);
            profile = new MCRORCIDProfile(orcid);
            if (weAreTrustedParty()) {
                String token = user.getUserAttribute(ATTR_ORCID_TOKEN);
                profile.setAccessToken(token);
            }
        }
        return profile;
    }
}

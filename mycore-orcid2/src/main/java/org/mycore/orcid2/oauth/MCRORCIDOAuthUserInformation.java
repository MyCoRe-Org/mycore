/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.orcid2.oauth;

import java.util.Objects;

import org.mycore.common.MCRUserInformation;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.util.MCRORCIDJSONMapper;
import org.mycore.user2.MCRRealm;

/**
 * Class representing user information for OAuth authentication via ORCID.
 * <p>
 * This class implements {@link MCRUserInformation} and provides the user's ORCID, name,
 * and associated credentials for ORCID OAuth authentication.
 * </p>
 */
public class MCRORCIDOAuthUserInformation implements MCRUserInformation {

    public static final String REALM_ID = "orcid.org";

    private final String orcid;

    private final String name;

    private final MCRORCIDCredential credential;

    /**
     * Constructs an instance of {code MCRORCIDOAuthUserInformation}.
     *
     * @param orcid the ORCID of the user
     * @param name the name of the user
     * @param credential the ORCID OAuth credential of the user
     */
    public MCRORCIDOAuthUserInformation(String orcid, String name, MCRORCIDCredential credential) {
        this.orcid = orcid;
        this.name = name;
        this.credential = credential;
    }

    @Override
    public String getUserID() {
        return orcid + "@" + REALM_ID;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public String getUserAttribute(String attribute) {
        if (Objects.equals(MCRORCIDUser.ATTR_ORCID_CREDENTIAL + orcid, attribute)) {
            return MCRORCIDJSONMapper.credentialToJSON(credential);
        }
        return switch (attribute) {
            case ATT_REAL_NAME -> name;
            case MCRRealm.USER_INFORMATION_ATTR -> REALM_ID;
            case MCRORCIDUser.ATTR_ORCID_ID -> orcid;
            default -> throw new IllegalArgumentException("Unexpected value: " + attribute);
        };
    }

}

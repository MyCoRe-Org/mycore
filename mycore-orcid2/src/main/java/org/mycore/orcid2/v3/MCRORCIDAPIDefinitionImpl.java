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

package org.mycore.orcid2.v3;

import org.mycore.orcid2.client.MCRORCIDAPIDefinition;

/**
 * See {@link org.mycore.orcid2.client.MCRORCIDAPIDefinition}.
 */
public class MCRORCIDAPIDefinitionImpl implements MCRORCIDAPIDefinition {

    private static final String MEMBER_URL = "https://api.orcid.org/v3.0";

    private static final String MEMBER_SANDBOX_URL = "https://api.sandbox.orcid.org/v3.0";

    private static final String PUBLIC_URL = "https://pub.orcid.org/v3.0";

    private static final String PUBLIC_SANDBOX_URL = "https://pub.sandbox.orcid.org/v3.0";

    public String getMemberURL() {
        return MEMBER_URL;
    }

    public String getMemberSandboxURL() {
        return MEMBER_SANDBOX_URL;
    }

    public String getPublicURL() {
        return PUBLIC_URL;
    }

    public String getPublicSandboxURL() {
        return PUBLIC_SANDBOX_URL;
    }
}

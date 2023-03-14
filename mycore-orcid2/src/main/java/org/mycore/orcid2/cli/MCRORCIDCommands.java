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

package org.mycore.orcid2.cli;

import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.user.MCRORCIDCredentials;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Provides cli commands for orcid2.
 */
@MCRCommandGroup(
    name = "ORCID2")
public class MCRORCIDCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * ORCiD token user attribute name.
     */
    protected static final String ORCID_TOKEN_ATTRIBUTE_NAME = "token_orcid";

    /**
     * Migrates all orcid token user attributes to MCRORCIDCredentials.
     * 
     * @throws MCRORCIDException if migration fails
     * @throws MCRException if there is more than ORCiD attribute for one user
     */
    @MCRCommand(syntax = "migrate all orcid access token attributes",
        help = "Migrates orcid user access token attributes to orcid2 credentials")
    public static void migrateORCIDTokenAttributes() throws MCRORCIDException, MCRException {
        final List<MCRUser> users
            = MCRUserManager.listUsers(null, null, null, null, ORCID_TOKEN_ATTRIBUTE_NAME, 0, Integer.MAX_VALUE);
        for (MCRUser user : users) {
            String orcid = null;
            orcid = user.getUserAttribute(MCRORCIDUser.ATTR_ORCID_ID);
            final String token = user.getUserAttribute(ORCID_TOKEN_ATTRIBUTE_NAME);
            if (orcid == null) {
                LOGGER.info("Ignored {}, ORCiD attribute is missing.", user.getUserName());
                continue;
            }
            final MCRORCIDCredentials credentials = new MCRORCIDCredentials(orcid, token);
            credentials.setTokenType("bearer");
            final MCRORCIDUser orcidUser = new MCRORCIDUser(user);
            orcidUser.storeCredentials(credentials);
            user.getAttributes().removeIf(a -> Objects.equals(a.getName(), ORCID_TOKEN_ATTRIBUTE_NAME));
            MCRUserManager.updateUser(user);
        }
        LOGGER.info("Migrated all user attributes.");
    }
}

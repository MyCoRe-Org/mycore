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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.mycore.common.MCRJPATestCase;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

public class MCRORCIDCommandsTest extends MCRJPATestCase {

    private static final String ORCID_ID = "0000-0001-2345-6789";

    private static final String ORCID_ACCESS_TOKEN = "token";

    @Test
    public void migrateORCIDTokenAttributeTest() {
        final MCRUser user = new MCRUser("junit");
        user.setUserAttribute(MCRORCIDCommands.ORCID_TOKEN_ATTRIBUTE_NAME, ORCID_ACCESS_TOKEN);
        user.setUserAttribute(MCRORCIDUser.ATTR_ORCID_ID, ORCID_ID);
        MCRUserManager.createUser(user);
        MCRORCIDCommands.migrateORCIDTokenAttributes();
        assertEquals(2, user.getAttributes().size());
        assertNull(user.getUserAttribute(MCRORCIDCommands.ORCID_TOKEN_ATTRIBUTE_NAME));
        assertEquals(ORCID_ID, user.getUserAttribute(MCRORCIDUser.ATTR_ORCID_ID));
        assertNotNull(user.getUserAttribute(MCRORCIDUser.ATTR_ORCID_CREDENTIAL + ORCID_ID));
        final MCRORCIDUser orcidUser = new MCRORCIDUser(user);
        final MCRORCIDCredential credential = orcidUser.getCredentialByORCID(ORCID_ID);
        assertNotNull(credential);
        assertEquals(ORCID_ACCESS_TOKEN, credential.getAccessToken());
    }
}

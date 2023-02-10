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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;

import org.junit.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRJPATestCase;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

public class MCRORCIDUserTest extends MCRJPATestCase {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ORCID = "0000-0001-2345-6789";

    private static final String ACCESS_TOKEN = "accessToken";

    @Test
    public void testStoreGetCredentials() {
        MCRUser user = new MCRUser("junit");
        MCRUserManager.createUser(user);
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);
        assertEquals(0, orcidUser.listCredentials().size());
        final MCRORCIDCredentials credentials = new MCRORCIDCredentials(ORCID, ACCESS_TOKEN);
        orcidUser.storeCredentials(credentials);
        assertEquals(2, user.getAttributes().size()); // id_orcid + orcid_credentials_orcid
        assertNotNull(user.getUserAttribute("orcid_credentials_" + ORCID));
        assertEquals(credentials.getORCID(), user.getUserAttribute("id_orcid"));
        assertEquals(credentials, orcidUser.getCredentialsByORCID(credentials.getORCID()));
    }

    @Test
    public void testRemoveAllCredentials() {
        MCRUser user = new MCRUser("junit");
        MCRUserManager.createUser(user);
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);
        final MCRORCIDCredentials credentials = new MCRORCIDCredentials(ORCID, ACCESS_TOKEN);
        orcidUser.storeCredentials(credentials);
        user.setUserAttribute("test", "test");
        orcidUser.removeAllCredentials();
        assertEquals(2, user.getAttributes().size()); // id_orcid + test
        assertEquals(ORCID, user.getUserAttribute("id_orcid"));
        assertEquals("test", user.getUserAttribute("test"));
    }

    @Test
    public void testSerialization() {
        final MCRORCIDCredentials credentials = new MCRORCIDCredentials(ORCID, ACCESS_TOKEN);
        credentials.setTokenType("bearer");
        credentials.setRefreshToken("refreshToken");
        credentials.setExpiresIn("631138518");
        credentials.setScope("/read-limited");
        credentials.setName("MyCoRe");
        credentials.setExpiration(LocalDate.now());
        final String credentialsString = MCRORCIDUser.serializeCredentials(credentials);
        LOGGER.info(credentialsString);
        final MCRORCIDCredentials result = MCRORCIDUser.deserializeCredentials(credentialsString);
        assertEquals(credentials.getAccessToken(), result.getAccessToken());
        assertEquals(credentials.getRefreshToken(), result.getRefreshToken());
        assertEquals(credentials.getTokenType(), result.getTokenType());
        assertEquals(credentials.getScope(), result.getScope());
        assertEquals(credentials.getExpiration(), result.getExpiration());
        assertNull(result.getName());
        assertNull(result.getORCID());
    }
}

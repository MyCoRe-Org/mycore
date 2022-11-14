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

import java.util.Date;

import org.junit.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRJPATestCase;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;
import org.mycore.user2.MCRUserManager;

public class MCRORCIDUserTest extends MCRJPATestCase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void testStoreGetCredentials() {
        MCRUser user = new MCRUser("junit");
        MCRUserManager.createUser(user);
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);
        assertNull(orcidUser.getCredentials());
        final MCRORCIDCredentials credentials = new MCRORCIDCredentials("orcid", "accessToken");
        orcidUser.storeCredentials(credentials);
        assertEquals(2, user.getAttributes().size()); // id_orcid + orcid_credentials_orcid
        assertNotNull(user.getUserAttribute("orcid_credentials_orcid"));
        assertEquals("orcid", user.getUserAttribute("id_orcid"));
        assertEquals(credentials, orcidUser.getCredentials());
    }

    @Test
    public void testRemoveCredentials() {
        MCRUser user = new MCRUser("junit");
        MCRUserManager.createUser(user);
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);
        final MCRORCIDCredentials credentials = new MCRORCIDCredentials("orcid", "accessToken");
        orcidUser.storeCredentials(credentials);
        user.setUserAttribute("test", "test");
        orcidUser.removeCredentials();
        assertEquals(2, user.getAttributes().size()); // id_orcid + test
        assertEquals("orcid", user.getUserAttribute("id_orcid"));
        assertEquals("test", user.getUserAttribute("test"));
    }

    @Test(expected = MCRORCIDException.class)
    public void testMoreThanOneCredentials() {
        final MCRUser user = new MCRUser("junit");
        MCRUserManager.createUser(user);
        user.getAttributes().add(new MCRUserAttribute("orcid_credentials_foo", "foo"));
        user.getAttributes().add(new MCRUserAttribute("orcid_credentials_bar", "bar"));
        new MCRORCIDUser(user).getCredentials(); // exception
        MCRUserManager.deleteUser(user);
    }

    @Test
    public void testSerialization() {
        final MCRORCIDCredentials credentials
            = new MCRORCIDCredentials("0000-0001-2345-6789", "f5af9f51-07e6-4332-8f1a-c0c11c1e3728");
        credentials.setTokenType("bearer");
        credentials.setRefreshToken("f725f747-3a65-49f6-a231-3e8944ce464d");
        credentials.setExpiresIn("631138518");
        credentials.setScope("/read-limited");
        credentials.setName("Sofia Garcia");
        credentials.setExpiration(new Date());
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

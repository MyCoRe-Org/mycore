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

package org.mycore.user2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.xml.MCRURIResolver;

public class MCRUserResolverTest extends MCRUserTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        MCRUser user = new MCRUser("junit");
        user.setUserAttribute("existing", "value");
        MCRUserManager.createUser(user);
        startNewTransaction();
    }

    @Test
    public void currentUserContainsStoredAttributesForOtherUserInformation() {
        // e.g. a JWT on the REST API or a login module: the session does not hold an MCRUser
        MCRSessionMgr.getCurrentSession().setUserInformation(new MCRUserInformation() {
            @Override
            public String getUserID() {
                return "junit";
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public String getUserAttribute(String attribute) {
                return null;
            }
        });

        assertEquals(List.of("existing"), resolveAttributeNames("user:current"));
    }

    @Test
    public void currentUserContainsAttributesAddedAfterLogin() {
        // the session holds the user as of the time of the login, the user is changed in another session
        MCRUser loginUser = MCRUserManager.getUser("junit").getSafeCopy();
        MCRUser stored = MCRUserManager.getUser("junit");
        stored.setUserAttribute("added", "value");
        MCRUserManager.updateUser(stored);
        startNewTransaction();
        MCRSessionMgr.getCurrentSession().setUserInformation(loginUser);

        assertEquals(List.of("added", "existing"), resolveAttributeNames("user:current"));
    }

    @Test
    public void currentUserIsResolvedForUsersThatAreNotStored() {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getGuestInstance());

        Element user = MCRURIResolver.instance().resolve("user:current");

        assertNotNull(user);
        assertEquals(MCRSystemUserInformation.getGuestInstance().getUserID(), user.getAttributeValue("name"));
    }

    private static List<String> resolveAttributeNames(String uri) {
        Element attributes = MCRURIResolver.instance().resolve(uri).getChild("attributes");
        return attributes == null ? List.of()
            : attributes.getChildren("attribute").stream().map(a -> a.getAttributeValue("name")).sorted().toList();
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.URIResolver.CachingResolver.Capacity", "0");
        testProperties.put("MCR.URIResolver.CachingResolver.MaxAge", "0");
        return testProperties;
    }
}

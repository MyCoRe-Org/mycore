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

package org.mycore.common;

import static org.junit.Assert.assertEquals;
import static org.mycore.common.MCRSystemUserInformation.GUEST;
import static org.mycore.common.MCRSystemUserInformation.SUPER_USER;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRSessionTest extends MCRTestCase {

    private MCRSession session;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.session = new MCRSession();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        this.session.close();
        super.tearDown();
    }

    @Test
    public void testIsGuestDefault() {
        assertEquals(GUEST, session.getUserInformation());
    }

    @Test
    public void loginSuperUser() {
        session.setUserInformation(SUPER_USER);
        assertEquals(SUPER_USER, session.getUserInformation());
    }

    @Test
    public void downGradeUser() {
        MCRUserInformation otherUser = getSimpleUserInformation("JUnit");
        session.setUserInformation(SUPER_USER);
        session.setUserInformation(otherUser);
        assertEquals(otherUser, session.getUserInformation());
        session.setUserInformation(GUEST);
        assertEquals(GUEST, session.getUserInformation());
    }

    @Test(expected = IllegalArgumentException.class)
    public void upgradeFail() {
        MCRUserInformation otherUser = getSimpleUserInformation("JUnit");
        session.setUserInformation(otherUser);
        session.setUserInformation(SUPER_USER);
    }

    private static MCRUserInformation getSimpleUserInformation(String userID) {
        return new MCRUserInformation() {
            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public String getUserID() {
                return userID;
            }

            @Override
            public String getUserAttribute(String attribute) {
                return null;
            }
        };
    }

}

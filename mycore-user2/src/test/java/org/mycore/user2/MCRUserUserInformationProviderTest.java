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

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.MCRUserInformationResolver;

import static org.junit.Assert.assertEquals;

public class MCRUserUserInformationProviderTest extends MCRUserTestCase {

    private MCRUser userWithoutRealm;

    private MCRUser userWithRealm;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        userWithoutRealm = new MCRUser("hugo");
        MCRUserManager.createUser(userWithoutRealm);
        userWithRealm = new MCRUser("hugo");
        userWithRealm.setRealm(MCRRealmFactory.getRealm("mycore.de"));
        MCRUserManager.createUser(userWithRealm);
    }


    @Test
    public final void testGetUserWithoutRealm() {
        assertEquals(Optional.of(userWithoutRealm), get("hugo"));
    }

    @Test
    public final void testGetUserWithRealm() {
        assertEquals(Optional.of(userWithRealm), get("hugo@mycore.de"));
    }

    @Test
    public final void testGetUnknownUser() {
        assertEquals(Optional.empty(), get("unknown"));
    }

    private Optional<MCRUserInformation> get(String userId) {
        return MCRUserInformationResolver.instance().get("user", userId);
    }

}

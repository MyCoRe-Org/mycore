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

package org.mycore.user2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.MCRUserInformationResolver;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class })
public class MCRUserProviderTest {

    private MCRUser userWithoutRealm;

    private MCRUser userWithRealm;

    @BeforeEach
    public void setUp() throws Exception {

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
        return MCRUserInformationResolver.obtainInstance().get("user", userId);
    }

}

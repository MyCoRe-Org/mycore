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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRSystemUserInformationProviderTest {

    @Test
    public void getGuest() {
        assertEquals(
            Optional.of(MCRSystemUserInformation.GUEST),
            get(MCRSystemUserInformation.GUEST.name()));
    }

    @Test
    public void getJanitor() {
        assertEquals(
            Optional.of(MCRSystemUserInformation.JANITOR),
            get(MCRSystemUserInformation.JANITOR.name()));
    }

    @Test
    public void getSystemUser() {
        assertEquals(
            Optional.of(MCRSystemUserInformation.SYSTEM_USER),
            get(MCRSystemUserInformation.SYSTEM_USER.name()));
    }

    @Test
    public void getSuperUser() {
        assertEquals(
            Optional.of(MCRSystemUserInformation.SUPER_USER),
            get(MCRSystemUserInformation.SUPER_USER.name()));
    }

    @Test
    public void getUnknown() {
        assertEquals(
            Optional.empty(),
            get("UNKNOWN"));
    }

    private Optional<MCRUserInformation> get(String userId) {
        return MCRUserInformationResolver.obtainInstance().get("system", userId);
    }

}

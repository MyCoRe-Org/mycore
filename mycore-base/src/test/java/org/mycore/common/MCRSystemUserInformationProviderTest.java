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

import java.util.Optional;

import org.junit.Test;

public class MCRSystemUserInformationProviderTest extends MCRTestCase {

    @Test
    public void getGuest() {
        assertEquals(
            Optional.of(MCRSystemUserInformation.getGuestInstance()),
            get(MCRSystemUserInformationProvider.GUEST));
    }

    @Test
    public void getJanitor() {
        assertEquals(
            Optional.of(MCRSystemUserInformation.getJanitorInstance()),
            get(MCRSystemUserInformationProvider.JANITOR));
    }

    @Test
    public void getSystemUser() {
        assertEquals(
            Optional.of(MCRSystemUserInformation.getSystemUserInstance()),
            get(MCRSystemUserInformationProvider.SYSTEM_USER));
    }

    @Test
    public void getSuperUser() {
        assertEquals(
            Optional.of(MCRSystemUserInformation.getSuperUserInstance()),
            get(MCRSystemUserInformationProvider.SUPER_USER));
    }

    @Test
    public void getUnknown() {
        assertEquals(
            Optional.empty(),
            get("UNKNOWN"));
    }
    
    private Optional<MCRUserInformation> get(String userId) {
        return MCRUserInformationResolver.instance().get("system", userId);
    }

}

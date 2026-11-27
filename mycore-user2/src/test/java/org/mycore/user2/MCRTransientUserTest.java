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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRUserInformation;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRUserExtension.class })
public class MCRTransientUserTest {

    private static final String REALM_ID = "mycore.de";

    @Test
    public void keepsUserNameIfMappedAttributesAreMissing() {
        MCRTransientUser user = new MCRTransientUser(userInformation("junit@" + REALM_ID, Map.of()));

        assertEquals("junit", user.getUserName());
        assertEquals("junit@" + REALM_ID, user.getUserID());
    }

    @Test
    public void mapsAvailableAttributes() {
        MCRTransientUser user = new MCRTransientUser(
            userInformation("junit@" + REALM_ID, Map.of("displayName", "Test Case")));

        assertEquals("junit@" + REALM_ID, user.getUserID());
        assertEquals("Test Case", user.getRealName());
    }

    private static MCRUserInformation userInformation(String userId, Map<String, String> attributes) {
        return new MCRUserInformation() {
            @Override
            public String getUserID() {
                return userId;
            }

            @Override
            public boolean isUserInRole(String role) {
                return false;
            }

            @Override
            public String getUserAttribute(String attribute) {
                if (MCRRealm.USER_INFORMATION_ATTR.equals(attribute)) {
                    return userId.substring(userId.lastIndexOf('@') + 1);
                }
                return attributes.get(attribute);
            }
        };
    }
}

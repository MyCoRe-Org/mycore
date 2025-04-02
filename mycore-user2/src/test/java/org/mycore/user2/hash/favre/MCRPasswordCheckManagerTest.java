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

package org.mycore.user2.hash.favre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.hash.MCRPasswordCheckData;
import org.mycore.user2.hash.MCRPasswordCheckManager;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.User.PasswordCheck.ConfigurationChecks", empty = true)
})
public class MCRPasswordCheckManagerTest {

    protected static final String TYPE = "type";

    protected static final String PASSWORD = "passwd123";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.bcrypt.Class", classNameOf = MCRBCryptStrategy.class),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.bcrypt.Cost", string = "12"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.SelectedStrategy", string = "bcrypt")
    })
    public final void testBCrypt() {

        MCRPasswordCheckManager manager = MCRPasswordCheckManager.createInstance();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("bcrypt", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

}

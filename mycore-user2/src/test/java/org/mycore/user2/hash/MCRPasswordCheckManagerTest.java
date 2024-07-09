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

package org.mycore.user2.hash;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MCRPasswordCheckManagerTest extends MCRPasswordCheckManagerTestBase {

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "crypt")
    })
    public final void testCrypt() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("crypt", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "md5")
    })
    public final void testMD5() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("md5", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "sha1")
    })
    public final void testSHA1() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("sha1", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "sha256")
    })
    public final void testSHA256() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("sha256", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "sha512")
    })
    public final void testSHA512() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("sha512", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "s2k")
    })
    public final void testS2K() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("s2k", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "pbkdf2")
    })
    public final void testPBKDF2() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("pbkdf2", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test(expected = MCRException.class)
    public final void testUnknownType() {

        SecureRandom random = new SecureRandom();
        Map<String, MCRPasswordCheckStrategy> strategies = new HashMap<>();
        strategies.put("foo", new MCRMD5Strategy(1));

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager(random, strategies, "foo", false);

        MCRPasswordCheckData data = manager.create(PASSWORD);
        MCRPasswordCheckData data2 = new MCRPasswordCheckData("bar", data.salt(), data.hash());
        manager.verify(data2, PASSWORD);

    }

    @Test
    public final void testNotPreferred() {

        SecureRandom random = new SecureRandom();
        Map<String, MCRPasswordCheckStrategy> strategies = new HashMap<>();
        strategies.put("old", new MCRMD5Strategy(1));
        strategies.put("new", new MCRMD5Strategy(2));

        MCRPasswordCheckManager managerOld = new MCRPasswordCheckManager(random, strategies, "old", false);
        MCRPasswordCheckManager managerNew = new MCRPasswordCheckManager(random, strategies, "new", false);

        MCRPasswordCheckResult result = managerNew.verify(managerOld.create(PASSWORD), PASSWORD);

        assertTrue(result.valid());
        assertTrue(result.deprecated());

    }

}

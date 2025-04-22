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

package org.mycore.user2.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.User.PasswordCheck.ConfigurationChecks", empty = true)
})
public class MCRPasswordCheckManagerTest {

    protected static final Set<MCRPasswordCheckManager.ConfigurationCheck> NO_CHECKS = Collections.emptySet();

    protected static final String TYPE = "type";

    protected static final String PASSWORD = "passwd123";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.SelectedStrategy", string = "crypt")
    })
    public final void testCrypt() {

        MCRPasswordCheckManager manager = MCRPasswordCheckManager.createInstance();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("crypt", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.SelectedStrategy", string = "md5")
    })
    public final void testMD5() {

        MCRPasswordCheckManager manager = MCRPasswordCheckManager.createInstance();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("md5", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.SelectedStrategy", string = "sha1")
    })
    public final void testSHA1() {

        MCRPasswordCheckManager manager = MCRPasswordCheckManager.createInstance();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("sha1", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.SelectedStrategy", string = "sha256")
    })
    public final void testSHA256() {

        MCRPasswordCheckManager manager = MCRPasswordCheckManager.createInstance();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("sha256", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.SelectedStrategy", string = "sha512")
    })
    public final void testSHA512() {

        MCRPasswordCheckManager manager = MCRPasswordCheckManager.createInstance();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("sha512", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.SelectedStrategy", string = "s2k")
    })
    public final void testS2K() {

        MCRPasswordCheckManager manager = MCRPasswordCheckManager.createInstance();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("s2k", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.SelectedStrategy", string = "pbkdf2")
    })
    public final void testPBKDF2() {

        MCRPasswordCheckManager manager = MCRPasswordCheckManager.createInstance();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("pbkdf2", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    public final void testUnknownType() {

        assertThrows(MCRException.class, () -> {

            SecureRandom random = new SecureRandom();
            Map<String, MCRPasswordCheckStrategy> strategies = new HashMap<>();
            strategies.put("foo", new MCRMD5Strategy(0, 1));

            MCRPasswordCheckManager manager = new MCRPasswordCheckManager(random, strategies, "foo", NO_CHECKS);

            MCRPasswordCheckData data = manager.create(PASSWORD);
            MCRPasswordCheckData data2 = new MCRPasswordCheckData("bar", data.salt(), data.hash());
            manager.verify(data2, PASSWORD);

        });

    }

    @Test
    public final void testNotPreferred() {

        SecureRandom random = new SecureRandom();
        Map<String, MCRPasswordCheckStrategy> strategies = new HashMap<>();
        strategies.put("old", new MCRMD5Strategy(0, 1));
        strategies.put("new", new MCRMD5Strategy(0, 2));

        MCRPasswordCheckManager managerOld = new MCRPasswordCheckManager(random, strategies, "old", NO_CHECKS);
        MCRPasswordCheckManager managerNew = new MCRPasswordCheckManager(random, strategies, "new", NO_CHECKS);

        MCRPasswordCheckResult result = managerNew.verify(managerOld.create(PASSWORD), PASSWORD);

        assertTrue(result.valid());
        assertTrue(result.deprecated());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.SelectedStrategy", string = "md5"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.ConfigurationChecks", string = "OUTDATED_STRATEGY")
    })
    public final void testOutdatedCheck() {

        // Exception might be ExceptionInInitializerError if MCRPasswordCheckManager
        // hasn't been initialized before in another test
        Throwable e = assertThrows(Throwable.class, MCRPasswordCheckManager::createInstance);
        while (e.getCause() != null) {
            e = e.getCause();
        }

        assertInstanceOf(MCRConfigurationException.class, e);
        assertTrue(e.getMessage().contains("Detected outdated password check strategy"));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.SelectedStrategy", string = "md5"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.md5.Iterations", string = "2"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.ConfigurationChecks", string = "INCOMPATIBLE_CHANGE")
    })
    public final void testIncompatibleChangeCheck() {

        // Exception might be ExceptionInInitializerError if MCRPasswordCheckManager
        // hasn't been initialized before in another test
        Throwable e = assertThrows(Throwable.class, MCRPasswordCheckManager::createInstance);
        while (e.getCause() != null) {
            e = e.getCause();
        }

        assertInstanceOf(MCRConfigurationException.class, e);
        assertTrue(e.getMessage().contains("Detected incompatible value change"));

    }

}

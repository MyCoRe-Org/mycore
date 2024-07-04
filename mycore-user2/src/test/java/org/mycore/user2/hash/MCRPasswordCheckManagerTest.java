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
import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class MCRPasswordCheckManagerTest extends MCRTestCase {

    public static final String TYPE = "type";

    public static final String PASSWORD = "passwd123";

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
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "pbkdf2")
    })
    public final void testPBKDNF2() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("pbkdf2", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.bcrypt.Class", classNameOf = MCRBCryptStrategy.class),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.bcrypt.Cost", string = "12"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "bcrypt")
    })
    public final void testBCrypt() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("bcrypt", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.Class", classNameOf = MCRArgon2Strategy.class),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.SaltSizeBytes", string = "32"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.HashSizeBytes", string = "64"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.Iterations", string = "4"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.MemoryLimitKiloBytes", string = "66536"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.Parallelism", string = "1"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "argon2")
    })
    public final void testArgon2() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("argon2", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test(expected = MCRException.class)
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "md5")
    })
    public final void testUnknownType() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        MCRPasswordCheckData data2 = new MCRPasswordCheckData("foo", data.salt(), data.hash());
        manager.verify(data2, PASSWORD);

    }


    @Test
    public final void testNotPreferred() {

        SecureRandom random = MCRPasswordCheckManager.getConfiguredRandom();
        Map<String, MCRPasswordCheckStrategy> strategies = MCRPasswordCheckManager.getConfiguredStrategies();

        MCRPasswordCheckManager managerMD5 = new MCRPasswordCheckManager(random, strategies, "md5");
        MCRPasswordCheckManager managerSHA1 = new MCRPasswordCheckManager(random, strategies, "sha1");

        MCRPasswordCheckData dataMD5 = managerMD5.create(PASSWORD);
        MCRPasswordCheckResult resultSHA1 = managerSHA1.verify(dataMD5, PASSWORD);

        assertTrue(resultSHA1.valid());
        assertTrue(resultSHA1.deprecated());

    }

}

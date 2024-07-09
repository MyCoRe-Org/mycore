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

package org.mycore.user2.hash.bouncycastle;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.user2.hash.MCRPasswordCheckData;
import org.mycore.user2.hash.MCRPasswordCheckManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MCRPasswordCheckManagerTest extends MCRTestCase {

    public static final String TYPE = "type";

    public static final String PASSWORD = "passwd123";

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.bcrypt.Class", classNameOf = MCRBCryptStrategy.class),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.bcrypt.Cost", string = "13"),
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
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.scrypt.Class", classNameOf = MCRSCryptStrategy.class),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.scrypt.SaltSizeBytes", string = "32"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.scrypt.HashSizeBytes", string = "64"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.scrypt.Cost", string = "17"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.scrypt.BlockSize", string = "8"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.scrypt.Parallelism", string = "1"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "scrypt")
    })
    public final void testSCrypt() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("scrypt", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.Class", classNameOf = MCRArgon2Strategy.class),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.SaltSizeBytes", string = "32"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.HashSizeBytes", string = "64"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.Iterations", string = "8"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.MemoryLimitKiloBytes", string = "65536"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategies.argon2.Parallelism", string = "1"),
        @MCRTestProperty(key = "MCR.User.PasswordCheck.Strategy", string = "argon2")
    })
    public final void testArgon2() {

        MCRPasswordCheckManager manager = new MCRPasswordCheckManager();
        MCRPasswordCheckData data = manager.create(PASSWORD);

        assertEquals("argon2", data.type());
        assertTrue(manager.verify(data, PASSWORD).valid());

    }

}

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

import org.junit.Test;
import org.mycore.common.MCRTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MCRArgon2StrategyTest extends MCRTestCase {

    public static final String TYPE = "type";

    public static final String PASSWORD = "passwd123";

    @Test
    public final void test() {

        MCRPasswordCheckStrategy strategy = new MCRArgon2Strategy(32, 64, 8, 65536, 1);
        MCRPasswordCheckData data = strategy.create(new SecureRandom(), TYPE, PASSWORD);

        assertEquals(TYPE, data.type());
        assertNotNull(data.salt());
        assertEquals(128, data.hash().length());

        MCRPasswordCheckResult result = strategy.verify(data, PASSWORD);
        assertTrue(result.valid());
        assertFalse(result.deprecated());

    }

    @Test
    public final void testSaltSizeChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRArgon2Strategy(8, 8, 1, 1024, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRArgon2Strategy(16, 8, 1, 1024, 1);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertTrue(resultNew.valid());
        assertTrue(resultNew.deprecated());

    }

    @Test
    public final void testHashSizeChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRArgon2Strategy(8, 8, 1, 1024, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRArgon2Strategy(8, 16, 1, 1024, 1);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertTrue(resultNew.valid());
        assertTrue(resultNew.deprecated());

    }

    @Test
    public final void testIterationsChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRArgon2Strategy(8, 8, 1, 1024, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRArgon2Strategy(8, 8, 2, 1024, 1);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertFalse(resultNew.valid());
        assertFalse(resultNew.deprecated());

    }

    @Test
    public final void testMemoryLimitChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRArgon2Strategy(8, 8, 1, 1024, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRArgon2Strategy(8, 8, 1, 2048, 1);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertFalse(resultNew.valid());
        assertFalse(resultNew.deprecated());

    }

    @Test
    public final void testParallelismChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRArgon2Strategy(8, 8, 1, 1024, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRArgon2Strategy(8, 8, 1, 1024, 2);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertFalse(resultNew.valid());
        assertFalse(resultNew.deprecated());

    }

}

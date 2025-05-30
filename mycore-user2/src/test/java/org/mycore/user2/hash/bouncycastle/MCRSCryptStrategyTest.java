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

package org.mycore.user2.hash.bouncycastle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;
import org.mycore.user2.hash.MCRPasswordCheckData;
import org.mycore.user2.hash.MCRPasswordCheckResult;
import org.mycore.user2.hash.MCRPasswordCheckStrategy;

public class MCRSCryptStrategyTest {

    public static final String TYPE = "type";

    public static final String PASSWORD = "passwd123";

    @Test
    public final void test() {

        int saltSizeBytes = 32;
        int hashSizeBytes = 64;

        MCRPasswordCheckStrategy strategy = new MCRSCryptStrategy(saltSizeBytes, hashSizeBytes, 17, 8, 1);
        MCRPasswordCheckData data = strategy.create(new SecureRandom(), TYPE, PASSWORD);

        assertEquals(TYPE, data.type());
        assertEquals(saltSizeBytes * 2, data.salt().length());
        assertEquals(hashSizeBytes * 2, data.hash().length());

        MCRPasswordCheckResult result = strategy.verify(data, PASSWORD);
        assertTrue(result.valid());
        assertFalse(result.deprecated());

    }

    @Test
    public final void testSaltSizeChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRSCryptStrategy(8, 8, 1, 1, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRSCryptStrategy(16, 8, 1, 1, 1);
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

        MCRPasswordCheckStrategy strategyOld = new MCRSCryptStrategy(8, 8, 1, 1, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRSCryptStrategy(8, 16, 1, 1, 1);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertTrue(resultNew.valid());
        assertTrue(resultNew.deprecated());

    }

    @Test
    public final void testCostChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRSCryptStrategy(8, 8, 1, 1, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRSCryptStrategy(8, 8, 2, 1, 1);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertFalse(resultNew.valid());
        assertFalse(resultNew.deprecated());

    }

    @Test
    public final void testBlockSizeChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRSCryptStrategy(8, 8, 1, 1, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRSCryptStrategy(8, 8, 1, 2, 1);
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

        MCRPasswordCheckStrategy strategyOld = new MCRSCryptStrategy(8, 8, 1, 1, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRSCryptStrategy(8, 8, 1, 1, 2);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertFalse(resultNew.valid());
        assertFalse(resultNew.deprecated());

    }

}

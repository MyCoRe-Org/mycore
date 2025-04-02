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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

public class MCRSHA1StrategyTest {

    public static final String TYPE = "type";

    public static final String PASSWORD = "passwd123";

    public static final int SHA1_HASH_STRING_LENGTH = 40;

    @Test
    public final void test() {

        MCRPasswordCheckStrategy strategy = new MCRSHA1Strategy(8, 1000);
        MCRPasswordCheckData data = strategy.create(new SecureRandom(), TYPE, PASSWORD);

        assertEquals(TYPE, data.type());
        assertNotNull(data.salt());
        assertEquals(SHA1_HASH_STRING_LENGTH, data.hash().length());

        MCRPasswordCheckResult result = strategy.verify(data, PASSWORD);
        assertTrue(result.valid());
        assertFalse(result.deprecated());

    }

    @Test
    public final void testSaltSizeChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRSHA1Strategy(8, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRSHA1Strategy(16, 1);
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

        MCRPasswordCheckStrategy strategyOld = new MCRSHA1Strategy(8, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRSHA1Strategy(8, 2);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertFalse(resultNew.valid());
        assertFalse(resultNew.deprecated());

    }

}

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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;

import org.junit.jupiter.api.Test;

public class MCRMD5StrategyTest {

    public static final String TYPE = "type";

    public static final String PASSWORD = "passwd123";

    public static final String MD5_HASH_STRING = "1164f9e5b8cef768396dcd5374e4b6eb";

    @Test
    public final void test() {

        MCRPasswordCheckStrategy strategy = new MCRMD5Strategy(0, 1);
        MCRPasswordCheckData data = strategy.create(new SecureRandom(), TYPE, PASSWORD);

        assertEquals(TYPE, data.type());
        assertEquals("", data.salt());
        assertEquals(MD5_HASH_STRING, data.hash());

        MCRPasswordCheckResult result = strategy.verify(data, PASSWORD);
        assertTrue(result.valid());
        assertFalse(result.deprecated());

    }

    @Test
    public final void testSaltSizeChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRMD5Strategy(8, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRMD5Strategy(16, 1);
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

        MCRPasswordCheckStrategy strategyOld = new MCRMD5Strategy(8, 1);
        MCRPasswordCheckStrategy strategyNew = new MCRMD5Strategy(8, 2);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertFalse(resultNew.valid());
        assertFalse(resultNew.deprecated());

    }

}

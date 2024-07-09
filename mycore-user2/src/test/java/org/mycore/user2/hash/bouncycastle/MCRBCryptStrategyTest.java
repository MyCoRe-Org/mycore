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

import java.security.SecureRandom;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.user2.hash.MCRPasswordCheckData;
import org.mycore.user2.hash.MCRPasswordCheckResult;
import org.mycore.user2.hash.MCRPasswordCheckStrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MCRBCryptStrategyTest extends MCRTestCase {

    public static final String TYPE = "type";

    public static final String PASSWORD = "passwd123";

    public static final int BCRYPT_HASH_STRING_LENGTH = 60;

    @Test
    public final void test() {

        MCRPasswordCheckStrategy strategy = new MCRBCryptStrategy(12);
        MCRPasswordCheckData data = strategy.create(new SecureRandom(), TYPE, PASSWORD);

        assertEquals(TYPE, data.type());
        assertNull(data.salt());
        assertEquals(BCRYPT_HASH_STRING_LENGTH, data.hash().length());

        MCRPasswordCheckResult result = strategy.verify(data, PASSWORD);
        assertTrue(result.valid());
        assertFalse(result.deprecated());

    }

    @Test
    public final void testCostChange() {

        MCRPasswordCheckStrategy strategyOld = new MCRBCryptStrategy(4);
        MCRPasswordCheckStrategy strategyNew = new MCRBCryptStrategy(5);
        MCRPasswordCheckData dataOld = strategyOld.create(new SecureRandom(), TYPE, PASSWORD);

        MCRPasswordCheckResult resultOld = strategyOld.verify(dataOld, PASSWORD);
        assertTrue(resultOld.valid());
        assertFalse(resultOld.deprecated());

        MCRPasswordCheckResult resultNew = strategyNew.verify(dataOld, PASSWORD);
        assertTrue(resultNew.valid());
        assertTrue(resultNew.deprecated());

    }

    @Test
    public final void testCompatibility() {

        MCRPasswordCheckStrategy strategy = new MCRBCryptStrategy(4);
        MCRPasswordCheckStrategy otherStrategy = new org.mycore.user2.hash.favre.MCRBCryptStrategy(4);
        MCRPasswordCheckData data = otherStrategy.create(new SecureRandom(), TYPE, PASSWORD);

        assertEquals(TYPE, data.type());
        assertNull(data.salt());
        assertEquals(BCRYPT_HASH_STRING_LENGTH, data.hash().length());

        MCRPasswordCheckResult result = strategy.verify(data, PASSWORD);
        assertTrue(result.valid());
        assertFalse(result.deprecated());

    }

}

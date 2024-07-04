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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MCRCryptStrategyTest extends MCRTestCase {

    public static final String TYPE = "type";

    public static final String PASSWORD = "passwd123";

    public static final String CRYPT_HASH_STRING = "AAEwxSmatLWzQ";

    @Test
    public final void test() {

        MCRPasswordCheckStrategy strategy = new MCRCryptStrategy();
        MCRPasswordCheckData data = strategy.create(new SecureRandom(), TYPE, PASSWORD);

        assertEquals(TYPE, data.type());
        assertNull(data.salt());
        assertEquals(CRYPT_HASH_STRING, data.hash());

        assertTrue(strategy.verify(data, PASSWORD).valid());

    }

}

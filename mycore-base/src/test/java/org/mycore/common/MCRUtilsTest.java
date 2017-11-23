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

package org.mycore.common;

import static org.junit.Assert.assertEquals;

import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.junit.Test;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUtilsTest extends MCRTestCase {

    private static final String TEST = "Hello World!";

    private static final String TEST_SHA1 = "2ef7bde608ce5404e97d5f042f95f89f1c232871";

    private static final String TEST_MD5 = "ed076287532e86365e841e92bfc50d8c";

    /**
     * Test method for {@link org.mycore.common.MCRUtils#asSHA1String(int, byte[], String)}.
     */
    @Test
    public final void testAsSHA1String() {
        try {
            String sha1String = MCRUtils.asSHA1String(1, null, TEST);
            assertEquals("SHA-1 string has not the right length", TEST_SHA1.length(), sha1String.length());
            assertEquals("SHA-1 string does not match", TEST_SHA1, sha1String);
        } catch (NoSuchAlgorithmException e) {
            LogManager.getLogger(this.getClass()).warn("SHA-1 algorithm not available");
        }
    }

    /**
     * Test method for {@link org.mycore.common.MCRUtils#asMD5String(int, byte[], String)}.
     */
    @Test
    public final void testAsMD5String() {
        try {
            String md5String = MCRUtils.asMD5String(1, null, TEST);
            assertEquals("MD5 string has not the right length", TEST_MD5.length(), md5String.length());
            assertEquals("MD5 string does not match", TEST_MD5, md5String);
        } catch (NoSuchAlgorithmException e) {
            LogManager.getLogger(this.getClass()).warn("MD5 algorithm not available");
        }
    }
}

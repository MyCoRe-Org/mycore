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

package org.mycore.common;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUtilsTest extends MCRTestCase {

    private static final String TEST = "Hello World!";

    private static final String TEST_SHA1 = "2ef7bde608ce5404e97d5f042f95f89f1c232871";

    private static final String TEST_SHA256 = "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069";

    private static final String TEST_SHA512 = "861844d6704e8573fec34d967e20bcfef3d424cf48be04e6dc08f2bd58c72974" +
        "3371015ead891cc3cf1c9d34b49264b510751b1ff9e537937bc46b5d6ff4ecc8";

    private static final String TEST_MD5 = "ed076287532e86365e841e92bfc50d8c";

    @Rule
    public TemporaryFolder baseDir = new TemporaryFolder();

    @Rule
    public TemporaryFolder evilDir = new TemporaryFolder();

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
     * Test method for {@link org.mycore.common.MCRUtils#asSHA256String(int, byte[], String)}.
     */
    @Test
    public final void testAsSHA256String() {
        try {
            String sha256String = MCRUtils.asSHA256String(1, null, TEST);
            assertEquals("SHA-256 string has not the right length", TEST_SHA256.length(), sha256String.length());
            assertEquals("SHA-256 string does not match", TEST_SHA256, sha256String);
        } catch (NoSuchAlgorithmException e) {
            LogManager.getLogger(this.getClass()).warn("SHA-256 algorithm not available");
        }
    }

    /**
     * Test method for {@link org.mycore.common.MCRUtils#asSHA512String(int, byte[], String)}.
     */
    @Test
    public final void testAsSHA512String() {
        try {
            String sha512String = MCRUtils.asSHA512String(1, null, TEST);
            assertEquals("SHA-512 string has not the right length", TEST_SHA512.length(), sha512String.length());
            assertEquals("SHA-512 string does not match", TEST_SHA512, sha512String);
        } catch (NoSuchAlgorithmException e) {
            LogManager.getLogger(this.getClass()).warn("SHA-512 algorithm not available");
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

    @Test(expected = MCRException.class)
    public final void testResolveEvil() {
        MCRUtils.safeResolve(baseDir.getRoot().toPath(), evilDir.getRoot().toPath());
    }

    @Test
    public final void testResolve() {
        Path path = baseDir.getRoot().toPath();
        Path resolve = MCRUtils.safeResolve(path, "foo", "bar");
        assertEquals(path.resolve("foo").resolve("bar"), resolve);
    }
}

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRPasswordCheckUtilsTest extends MCRTestCase {

    @Test
    public final void testStringEquals() {

        assertTrue(MCRPasswordCheckUtils.fixedEffortEquals((String) null, (String) null));
        assertFalse(MCRPasswordCheckUtils.fixedEffortEquals("foo", null));
        assertFalse(MCRPasswordCheckUtils.fixedEffortEquals(null, "bar"));
        assertFalse(MCRPasswordCheckUtils.fixedEffortEquals("foo", "bar"));
        assertTrue(MCRPasswordCheckUtils.fixedEffortEquals("foo", "foo"));

    }

    @Test
    public final void testCharArrayEquals() {

        char[] foo = "foo".toCharArray();
        char[] bar = "bar".toCharArray();

        assertTrue(MCRPasswordCheckUtils.fixedEffortEquals((char[]) null, (char[]) null));
        assertFalse(MCRPasswordCheckUtils.fixedEffortEquals(foo, null));
        assertFalse(MCRPasswordCheckUtils.fixedEffortEquals(null, bar));
        assertFalse(MCRPasswordCheckUtils.fixedEffortEquals(foo, bar));
        assertTrue(MCRPasswordCheckUtils.fixedEffortEquals(foo, foo));

    }

    @Test
    public final void testByteArrayEquals() {

        byte[] foo = new byte[] { 0, 1, 2 };
        byte[] bar = new byte[] { 2, 1, 1 };

        assertTrue(MCRPasswordCheckUtils.fixedEffortEquals((byte[]) null, (byte[]) null));
        assertFalse(MCRPasswordCheckUtils.fixedEffortEquals(foo, null));
        assertFalse(MCRPasswordCheckUtils.fixedEffortEquals(null, bar));
        assertFalse(MCRPasswordCheckUtils.fixedEffortEquals(foo, bar));
        assertTrue(MCRPasswordCheckUtils.fixedEffortEquals(foo, foo));

    }

}

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.user2.hash.MCRPasswordCheckUtils.fixedEffortEquals;

import org.junit.jupiter.api.Test;

@SuppressWarnings("ConstantValue")
public class MCRPasswordCheckUtilsTest {

    @Test
    public final void testStringEquals() {

        assertTrue(fixedEffortEquals((String) null, null));
        assertFalse(fixedEffortEquals("foo", null));
        assertFalse(fixedEffortEquals(null, "bar"));
        assertFalse(fixedEffortEquals("foo", "bar"));
        assertTrue(fixedEffortEquals("foo", "foo"));

    }

    @Test
    public final void testCharArrayEquals() {

        char[] foo = "foo".toCharArray();
        char[] bar = "bar".toCharArray();

        assertTrue(fixedEffortEquals((char[]) null, null));
        assertFalse(fixedEffortEquals(foo, null));
        assertFalse(fixedEffortEquals(null, bar));
        assertFalse(fixedEffortEquals(foo, bar));
        assertTrue(fixedEffortEquals(foo, foo));

    }

    @Test
    public final void testByteArrayEquals() {

        byte[] foo = { 0, 1, 2 };
        byte[] bar = { 2, 1, 1 };

        assertTrue(fixedEffortEquals((byte[]) null, null));
        assertFalse(fixedEffortEquals(foo, null));
        assertFalse(fixedEffortEquals(null, bar));
        assertFalse(fixedEffortEquals(foo, bar));
        assertTrue(fixedEffortEquals(foo, foo));

    }

}

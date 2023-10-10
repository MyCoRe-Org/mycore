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

package org.mycore.common.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRInstanceNameTest extends MCRTestCase {

    @Test
    public void nameWithoutSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Foo.Bar");

        assertEquals("Foo.Bar", name.actual());
        assertEquals("Foo.Bar", name.canonical());
        assertEquals(MCRInstanceName.Suffix.NONE, name.suffix());

    }

    @Test
    public void subNameWithoutSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Foo.Bar").subName("Baz");

        assertEquals("Foo.Bar.Baz", name.actual());
        assertEquals("Foo.Bar.Baz", name.canonical());
        assertEquals(MCRInstanceName.Suffix.NONE, name.suffix());

    }

    @Test
    public void nameWithUpperCaseSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Foo.Bar.Class");

        assertEquals("Foo.Bar.Class", name.actual());
        assertEquals("Foo.Bar", name.canonical());
        assertEquals(MCRInstanceName.Suffix.UPPER_CASE, name.suffix());

    }

    @Test
    public void subNameWithUpperCaseSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Foo.Bar.Class").subName("Baz");

        assertEquals("Foo.Bar.Baz.Class", name.actual());
        assertEquals("Foo.Bar.Baz", name.canonical());
        assertEquals(MCRInstanceName.Suffix.UPPER_CASE, name.suffix());

    }

    @Test
    public void nameWithLowerCaseSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Foo.Bar.class");

        assertEquals("Foo.Bar.class", name.actual());
        assertEquals("Foo.Bar", name.canonical());
        assertEquals(MCRInstanceName.Suffix.LOWER_CASE, name.suffix());

    }

    @Test
    public void subNameWithLowerCaseSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Foo.Bar.class").subName("Baz");

        assertEquals("Foo.Bar.Baz.class", name.actual());
        assertEquals("Foo.Bar.Baz", name.canonical());
        assertEquals(MCRInstanceName.Suffix.LOWER_CASE, name.suffix());

    }

    @Test
    public void emptyNameWithoutSuffix() {

        MCRInstanceName name = MCRInstanceName.of("");

        assertEquals("", name.actual());
        assertEquals("", name.canonical());
        assertEquals(MCRInstanceName.Suffix.NONE, name.suffix());

    }

    @Test
    public void subEmptyNameWithoutSuffix() {

        MCRInstanceName name = MCRInstanceName.of("").subName("Baz");

        assertEquals("Baz", name.actual());
        assertEquals("Baz", name.canonical());
        assertEquals(MCRInstanceName.Suffix.NONE, name.suffix());

    }

    @Test
    public void emptyNameWithUpperCaseSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Class");

        assertEquals("Class", name.actual());
        assertEquals("", name.canonical());
        assertEquals(MCRInstanceName.Suffix.UPPER_CASE, name.suffix());

    }

    @Test
    public void subEmptyNameWithUpperCaseSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Class").subName("Baz");

        assertEquals("Baz.Class", name.actual());
        assertEquals("Baz", name.canonical());
        assertEquals(MCRInstanceName.Suffix.UPPER_CASE, name.suffix());

    }

    @Test
    public void emptyNameWithLowerCaseSuffix() {

        MCRInstanceName name = MCRInstanceName.of("class");

        assertEquals("class", name.actual());
        assertEquals("", name.canonical());
        assertEquals(MCRInstanceName.Suffix.LOWER_CASE, name.suffix());

    }

    @Test
    public void subEmptyNameWithLowerCaseSuffix() {

        MCRInstanceName name = MCRInstanceName.of("class").subName("Baz");

        assertEquals("Baz.class", name.actual());
        assertEquals("Baz", name.canonical());
        assertEquals(MCRInstanceName.Suffix.LOWER_CASE, name.suffix());

    }

}

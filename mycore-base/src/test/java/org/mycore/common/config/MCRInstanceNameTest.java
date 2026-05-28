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

package org.mycore.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.mycore.common.config.instantiator.MCRInstanceName;

public class MCRInstanceNameTest {

    @Test
    public void emptyName() {

        assertThrows(IllegalArgumentException.class, () -> MCRInstanceName.of(""));

    }

    @Test
    public void nameWithoutSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Foo.Bar");

        assertEquals("Foo.Bar", name.canonical());
        assertEquals("Foo.Bar.Class", name.actual());

    }

    @Test
    public void nestedWithoutSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Foo.Bar").nested("Baz");

        assertEquals("Foo.Bar.Baz", name.canonical());
        assertEquals("Foo.Bar.Baz.Class", name.actual());

    }

    @Test
    public void nameWithSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Foo.Bar.Class");

        assertEquals("Foo.Bar", name.canonical());
        assertEquals("Foo.Bar.Class", name.actual());

    }

    @Test
    public void nestedWithSuffix() {

        MCRInstanceName name = MCRInstanceName.of("Foo.Bar.Class").nested("Baz");

        assertEquals("Foo.Bar.Baz", name.canonical());
        assertEquals("Foo.Bar.Baz.Class", name.actual());

    }

}

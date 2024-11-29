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

package org.mycore.common.log;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRTreeMessageTest extends MCRTestCase {

    private static final String SEPARATOR = System.lineSeparator();

    @Test
    public void emptyDescription() {

        MCRTreeMessage description = new MCRTreeMessage();

        String message = description.logMessage("Introduction");

        assertEquals("Introduction", message);

    }

    @Test
    public void simpleDescription() {

        MCRTreeMessage description = getSimpleDescription();

        String message = description.logMessage("Introduction");

        String expected = "Introduction" + SEPARATOR +
            "├─ Foo: foo" + SEPARATOR +
            "├─ Bar: bar" + SEPARATOR +
            "└─ Baz: baz";

        assertEquals(expected, message);

    }

    @Test
    public void complexDescription() {

        MCRTreeMessage description = getComplexDescription();

        String message = description.logMessage("Introduction");

        String expected = "Introduction" + SEPARATOR +
            "├─ One:" + SEPARATOR +
            "│  ├─ Foo: foo" + SEPARATOR +
            "│  ├─ Bar: bar" + SEPARATOR +
            "│  └─ Baz: baz" + SEPARATOR +
            "├─ Two:" + SEPARATOR +
            "│  ├─ Foo: foo" + SEPARATOR +
            "│  ├─ Bar: bar" + SEPARATOR +
            "│  └─ Baz: baz" + SEPARATOR +
            "└─ Three:" + SEPARATOR +
            "   ├─ Foo: foo" + SEPARATOR +
            "   ├─ Bar: bar" + SEPARATOR +
            "   └─ Baz: baz";

        assertEquals(expected, message);

    }

    @Test
    public void mixedDescription() {

        MCRTreeMessage description = new MCRTreeMessage();
        description.add("Header", "header");
        description.add("One", getSimpleDescription());
        description.add("Two", getSimpleDescription());
        description.add("Three", getSimpleDescription());
        description.add("Trailer", "trailer");

        String message = description.logMessage("Introduction");

        String expected = "Introduction" + SEPARATOR +
            "├─ Header: header" + SEPARATOR +
            "├─ One:" + SEPARATOR +
            "│  ├─ Foo: foo" + SEPARATOR +
            "│  ├─ Bar: bar" + SEPARATOR +
            "│  └─ Baz: baz" + SEPARATOR +
            "├─ Two:" + SEPARATOR +
            "│  ├─ Foo: foo" + SEPARATOR +
            "│  ├─ Bar: bar" + SEPARATOR +
            "│  └─ Baz: baz" + SEPARATOR +
            "├─ Three:" + SEPARATOR +
            "│  ├─ Foo: foo" + SEPARATOR +
            "│  ├─ Bar: bar" + SEPARATOR +
            "│  └─ Baz: baz" + SEPARATOR +
            "└─ Trailer: trailer";

        assertEquals(expected, message);

    }

    @Test
    public void nestedDescription() {

        MCRTreeMessage description = new MCRTreeMessage();
        description.add("23", getComplexDescription());
        description.add("42", getComplexDescription());

        String message = description.logMessage("Introduction");

        String expected = "Introduction" + SEPARATOR +
            "├─ 23:" + SEPARATOR +
            "│  ├─ One:" + SEPARATOR +
            "│  │  ├─ Foo: foo" + SEPARATOR +
            "│  │  ├─ Bar: bar" + SEPARATOR +
            "│  │  └─ Baz: baz" + SEPARATOR +
            "│  ├─ Two:" + SEPARATOR +
            "│  │  ├─ Foo: foo" + SEPARATOR +
            "│  │  ├─ Bar: bar" + SEPARATOR +
            "│  │  └─ Baz: baz" + SEPARATOR +
            "│  └─ Three:" + SEPARATOR +
            "│     ├─ Foo: foo" + SEPARATOR +
            "│     ├─ Bar: bar" + SEPARATOR +
            "│     └─ Baz: baz" + SEPARATOR +
            "└─ 42:" + SEPARATOR +
            "   ├─ One:" + SEPARATOR +
            "   │  ├─ Foo: foo" + SEPARATOR +
            "   │  ├─ Bar: bar" + SEPARATOR +
            "   │  └─ Baz: baz" + SEPARATOR +
            "   ├─ Two:" + SEPARATOR +
            "   │  ├─ Foo: foo" + SEPARATOR +
            "   │  ├─ Bar: bar" + SEPARATOR +
            "   │  └─ Baz: baz" + SEPARATOR +
            "   └─ Three:" + SEPARATOR +
            "      ├─ Foo: foo" + SEPARATOR +
            "      ├─ Bar: bar" + SEPARATOR +
            "      └─ Baz: baz";

        assertEquals(expected, message);

    }

    private MCRTreeMessage getSimpleDescription() {
        MCRTreeMessage description = new MCRTreeMessage();
        description.add("Foo", "foo");
        description.add("Bar", "bar");
        description.add("Baz", "baz");
        return description;
    }

    private MCRTreeMessage getComplexDescription() {
        MCRTreeMessage description = new MCRTreeMessage();
        description.add("One", getSimpleDescription());
        description.add("Two", getSimpleDescription());
        description.add("Three", getSimpleDescription());
        return description;
    }

}

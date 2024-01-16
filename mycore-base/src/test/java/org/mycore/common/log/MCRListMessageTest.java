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

package org.mycore.common.log;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

import static org.junit.Assert.assertEquals;

public class MCRListMessageTest extends MCRTestCase {

    private static final String SEPARATOR = System.lineSeparator();

    @Test
    public void emptyDescription() {

        MCRListMessage description = new MCRListMessage();

        String message = description.logMessage("Introduction");

        assertEquals("Introduction", message);

    }

    @Test
    public void simpleDescription() {

        MCRListMessage description = getSimpleDescription();

        String message = description.logMessage("Introduction");

        String expected = "Introduction" + SEPARATOR +
            "├─ Foo: foo" + SEPARATOR +
            "├─ Bar: bar" + SEPARATOR +
            "└─ Baz: baz";

        assertEquals(expected, message);

    }

    private MCRListMessage getSimpleDescription() {
        MCRListMessage description = new MCRListMessage();
        description.add("Foo", "foo");
        description.add("Bar", "bar");
        description.add("Baz", "baz");
        return description;
    }

}

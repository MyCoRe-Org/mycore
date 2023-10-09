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

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.log.MCRTableMessage.Column;

public class MCRTableMessageTest extends MCRTestCase {

    private static final String SEPARATOR = System.lineSeparator();

    @Test
    public void emptyTable() {

        MCRTableMessage<Number> table = new MCRTableMessage<>(new Column<>("Arabic", Number::number),
            new Column<>("Roman", Number::roman), new Column<>("English", Number::english));

        String message = table.logMessage("Introduction");

        String expected = "Introduction" + SEPARATOR +
            "│ Arabic │ Roman │ English │" + SEPARATOR +
            "├────────┼───────┼─────────┤";

        assertEquals(expected, message);

    }

    @Test
    public void columnLEssTable() {

        MCRTableMessage<Number> table = new MCRTableMessage<>();
        table.add(new Number(0, Optional.empty(), "zero"));
        table.add(new Number(1, Optional.of("I"), "one"));
        table.add(new Number(23, Optional.of("XXIII"), "twenty-three"));
        table.add(new Number(42, Optional.of("XLII"), "forty-two"));

        String message = table.logMessage("Introduction");

        String expected = "Introduction";

        assertEquals(expected, message);

    }

    @Test
    public void simpleTable() {

        MCRTableMessage<Number> table = new MCRTableMessage<>(new Column<>("Arabic", Number::number),
            new Column<>("Roman", Number::roman), new Column<>("English", Number::english));
        table.add(new Number(0, Optional.empty(), "zero"));
        table.add(new Number(1, Optional.of("I"), "one"));
        table.add(new Number(23, Optional.of("XXIII"), "twenty-three"));
        table.add(new Number(42, Optional.of("XLII"), "forty-two"));

        String message = table.logMessage("Introduction");

        String expected = "Introduction" + SEPARATOR +
            "│ Arabic │ Roman │ English      │" + SEPARATOR +
            "├────────┼───────┼──────────────┤" + SEPARATOR +
            "│ 0      │       │ zero         │" + SEPARATOR +
            "│ 1      │ I     │ one          │" + SEPARATOR +
            "│ 23     │ XXIII │ twenty-three │" + SEPARATOR +
            "│ 42     │ XLII  │ forty-two    │";

        assertEquals(expected, message);

    }

    private record Number(int number, Optional<String> roman, String english) {
    }

}

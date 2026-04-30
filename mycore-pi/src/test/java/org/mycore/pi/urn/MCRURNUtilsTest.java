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

package org.mycore.pi.urn;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class MCRURNUtilsTest {

    private static final String[] VALID_DATES = {
        "2025-11-07T15:00:21.000Z",
        "2025-11-07T15:00:21Z",
        "2025-11-07T15:00:21.1Z",
        "2025-11-07T15:00:21-12:00",
        "2025-11-07T15:00:21+01:00",
        "2025-11-07T15:00:21.123456Z",
    };

    @Test
    public void parseValidDates() {
        for (String date : VALID_DATES) {
            Date result = MCRURNUtils.parseDNBRegisterDate(date);
            Assert.assertNotNull("should parse: " + date, result);
            Assert.assertEquals("parsed date should match Instant.parse for: " + date,
                Date.from(Instant.parse(date)), result);
        }
    }

    @Test
    public void parseNullReturnsNull() {
        Assert.assertNull(MCRURNUtils.parseDNBRegisterDate(null));
    }

    @Test(expected = DateTimeParseException.class)
    public void parseInvalidThrows() {
        MCRURNUtils.parseDNBRegisterDate("invalidDate");
    }

}

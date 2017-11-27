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

package org.mycore.mods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Year;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.GregorianCalendar;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

import com.ibm.icu.util.Calendar;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRMODSDateHelperTest extends MCRTestCase {

    @Test
    public void testNull() {
        assertNull(MCRMODSDateHelper.getDate(null));
        assertNull(MCRMODSDateHelper.getCalendar(null));

        Element element = new Element("date");
        assertNull(MCRMODSDateHelper.getDate(element));
        assertNull(MCRMODSDateHelper.getCalendar(element));
    }

    @Test
    public void testISO8601yearOnly() {
        Element element = new Element("date");

        int fullYear = Year.now().get(ChronoField.YEAR);

        MCRMODSDateHelper.setDate(element, new Date(), MCRMODSDateFormat.iso8601_4);

        int year = Integer.parseInt(element.getText());
        assertEquals(fullYear, year);
        assertEquals("iso8601", element.getAttributeValue("encoding"));

        Date parsed = MCRMODSDateHelper.getDate(element);
        assertEquals(year, parsed.toInstant().atZone(ZoneId.systemDefault()).get(ChronoField.YEAR));

        element.removeAttribute("encoding");
        assertEquals(fullYear, MCRMODSDateHelper.getCalendar(element).get(Calendar.YEAR));
    }

    @Test
    public void testISO8601Date() {
        String date = "20110929";
        Element element = new Element("date").setText(date);

        GregorianCalendar parsed = MCRMODSDateHelper.getCalendar(element);
        assertEquals(2011, parsed.get(Calendar.YEAR));
        assertEquals(9 - 1, parsed.get(Calendar.MONTH));
        assertEquals(29, parsed.get(Calendar.DAY_OF_MONTH));

        MCRMODSDateHelper.setDate(element, parsed, MCRMODSDateFormat.iso8601_8);
        assertEquals("iso8601", element.getAttributeValue("encoding"));
        assertEquals(date, element.getText());
    }

    @Test
    public void testW3CDTFDate10() {
        String date = "2011-09-29";
        Element element = new Element("date").setText(date);

        GregorianCalendar parsed = MCRMODSDateHelper.getCalendar(element);
        assertEquals(2011, parsed.get(Calendar.YEAR));
        assertEquals(9 - 1, parsed.get(Calendar.MONTH));
        assertEquals(29, parsed.get(Calendar.DAY_OF_MONTH));

        MCRMODSDateHelper.setDate(element, parsed, MCRMODSDateFormat.w3cdtf_10);
        assertEquals("w3cdtf", element.getAttributeValue("encoding"));
        assertEquals(date, element.getText());
    }

    @Test
    public void testDateFormatsWithoutTimezone() {
        // Christmas :-) 
        int year = 2015, month = 12, day = 25;

        GregorianCalendar gIn = new GregorianCalendar(year, month - 1, day);
        Element element = new Element("date");
        MCRMODSDateHelper.setDate(element, gIn, MCRMODSDateFormat.w3cdtf_10);

        // Not christmas :-( ?
        assertEquals(year + "-" + month + "-" + day, element.getText());

        // Not christmas :-( ?
        GregorianCalendar gOut = MCRMODSDateHelper.getCalendar(element);
        assertEquals(day, gOut.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testW3CDTFDate19() {
        String date = "2011-09-29T13:14:15";
        Element element = new Element("date").setText(date);

        GregorianCalendar parsed = MCRMODSDateHelper.getCalendar(element);
        assertEquals(2011, parsed.get(Calendar.YEAR));
        assertEquals(9 - 1, parsed.get(Calendar.MONTH));
        assertEquals(29, parsed.get(Calendar.DAY_OF_MONTH));
        assertEquals(13, parsed.get(Calendar.HOUR_OF_DAY));
        assertEquals(14, parsed.get(Calendar.MINUTE));
        assertEquals(15, parsed.get(Calendar.SECOND));

        MCRMODSDateHelper.setDate(element, parsed, MCRMODSDateFormat.w3cdtf_19);
        assertEquals("w3cdtf", element.getAttributeValue("encoding"));
        assertEquals(date, element.getText());
    }
}

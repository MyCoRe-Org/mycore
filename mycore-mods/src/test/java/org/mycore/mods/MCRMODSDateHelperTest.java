/*
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.mods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

        Date date = new Date();
        int fullYear = 1900 + date.getYear();

        MCRMODSDateHelper.setDate(element, date, "iso8601-4");

        String year = element.getText();
        assertEquals(fullYear, Integer.parseInt(year));
        assertEquals("iso8601", element.getAttributeValue("encoding"));

        Date parsed = MCRMODSDateHelper.getDate(element);
        assertEquals(date.getYear(), parsed.getYear());

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

        MCRMODSDateHelper.setDate(element, parsed, "iso8601-8");
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

        MCRMODSDateHelper.setDate(element, parsed, "w3cdtf-10");
        assertEquals("w3cdtf", element.getAttributeValue("encoding"));
        assertEquals(date, element.getText());
    }

    @Test
    public void testDateFormatsWithoutTimezone() {
        // Christmas :-) 
        int year=2015, month=12, day=25;
        
        GregorianCalendar gIn = new GregorianCalendar(year,month-1,day);
        Element element = new Element("date");
        MCRMODSDateHelper.setDate(element, gIn, "w3cdtf-10");

        // Not christmas :-( ?
        assertEquals(year + "-" + month + "-" + day, element.getText() );

        // Not christmas :-( ?
        GregorianCalendar gOut = MCRMODSDateHelper.getCalendar(element);
        assertEquals( day, gOut.get( Calendar.DAY_OF_MONTH ) );
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

        MCRMODSDateHelper.setDate(element, parsed, "w3cdtf-19");
        assertEquals("w3cdtf", element.getAttributeValue("encoding"));
        assertEquals(date, element.getText());
    }
}

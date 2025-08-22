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

package org.mycore.datamodel.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

public class MCRISO8601DateTest {

    @Test
    public void getDate() {
        MCRISO8601Date ts = new MCRISO8601Date();
        assertNull(ts.getDate(), "Date is not Null");
        Date dt = new Date();
        ts.setDate(dt);
        assertNotNull(ts.getDate(), "Date is Null");
        assertEquals(dt, ts.getDate(), "Set date differs from get date");
    }

    @Test
    public void getFormat() {
        MCRISO8601Date ts = new MCRISO8601Date();
        assertNull(ts.getIsoFormat(), "Format used is not Null");
        ts.setFormat(MCRISO8601Format.COMPLETE);
        assertEquals(MCRISO8601Format.COMPLETE, ts.getIsoFormat(), "Set format differs from get format");
    }

    @Test
    public void getISOString() {
        MCRISO8601Date ts = new MCRISO8601Date();
        assertNull(ts.getISOString(), "Date is not Null");
        Date dt = new Date();
        ts.setDate(dt);
        assertNotNull(ts.getISOString(), "Date is Null");
    }

    @Test
    public void testFormat() {
        String year = "2015";
        String simpleFormat = "yyyy";
        String language = "de";
        //simulate MCRXMLFunctions.format();
        Locale locale = Locale.forLanguageTag(language);
        MCRISO8601Date mcrdate = new MCRISO8601Date();
        mcrdate.setFormat((String) null);
        mcrdate.setDate(year);
        String formatted = mcrdate.format(simpleFormat, locale, TimeZone.getDefault().getID());
        assertEquals(year, formatted);
    }

    /*
     * Test method for
     * 'org.mycore.datamodel.metadata.MCRMetaTimestamp.getSecond()'
     */
    @Test
    public void setDate() {
        MCRISO8601Date ts = new MCRISO8601Date();
        String timeString = "1997-07-16T19:20:30.452300+01:00";
        System.out.println(timeString);
        ts.setDate(timeString);
        assertNotNull(ts.getDate(), "Date is null");
        // this can be a different String, but point in time should be the same
        System.out.println(ts.getISOString());
        ts.setFormat(MCRISO8601Format.COMPLETE_HH_MM);
        System.out.println(ts.getISOString());
        // wrong date format for the following string should null the internal
        // date.
        timeString = "1997-07-16T19:20:30+01:00";
        System.out.println(timeString);
        ts.setDate(timeString);
        assertNull(ts.getDate(), "Date is not null");
        ts.setFormat((String) null); // check auto format determination
        ts.setDate(timeString);
        assertNotNull(ts.getDate(), "Date is null");
        // check if shorter format declarations fail if String is longer
        ts.setFormat(MCRISO8601Format.YEAR);
        timeString = "1997-07";
        ts.setDate(timeString);
        assertNull(ts.getDate(), "Date is not null");
        System.out.println(ts.getISOString());
        timeString = "01.12.1986";
        ts.setFormat((String) null);
        ts.setDate(timeString);
        assertNull(ts.getDate(), "Date is not null");

    }
}

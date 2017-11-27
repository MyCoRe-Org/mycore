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

package org.mycore.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;

/**
 * This class is a JUnit test case for org.mycore.common.MCRCalendar.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision: 1.3 $ $Date: 2008/06/02 10:10:05 $
 * 
 */
public class MCRCalendarTest extends MCRTestCase {

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRCalendar.getDateToFormattedString()'
     */
    @Test
    public void getDateToFormattedString() {
        GregorianCalendar default_calendar = new GregorianCalendar();
        default_calendar.set(1964, 1, 24);
        String dstring;
        Calendar cal;
        // gregorian without format
        cal = (GregorianCalendar) default_calendar.clone();
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal);
        assertEquals("calendar without format", "1964-02-24 AD", dstring);
        // gregorian with format
        cal = (GregorianCalendar) default_calendar.clone();
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal, "dd.MM.yyyy G");
        assertEquals("calendar with format dd.MM.yyyy G", "24.02.1964 AD", dstring);
        // gregorian with format
        cal = (GregorianCalendar) default_calendar.clone();
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal, "yyyy-MM-dd");
        assertEquals("gregorian with format yyyy-MM-dd", "1964-02-24", dstring);
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRCalendar.getHistoryDateAsCalendar(String, boolean, String)'
     */
    @Test
    public void getHistoryDateAsCalendar() {
        String cstring, dstring;
        cstring = MCRCalendar.getCalendarDateToFormattedString(new GregorianCalendar());
        Calendar cal;

        /* check julian calendar implementation */
        // all entries are empty
        try {
            cal = MCRCalendar.getHistoryDateAsCalendar(null, false, null);
        } catch (MCRException e) {
            cal = new GregorianCalendar();
        }
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal);
        assertEquals("Date is not the current date.", cstring, dstring);
        // 0A.01.0001 BC (wrong gregorian)
        try {
            cal = MCRCalendar.getHistoryDateAsCalendar("-0A.01.0001", false, MCRCalendar.TAG_JULIAN);
        } catch (MCRException e) {
            cal = new GregorianCalendar();
        }
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal);
        assertEquals("common", cstring, dstring);

        /* syntax expanding check */
        // 1 (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("common", "1721424", dstring);
        // 1.1 (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1", true, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("common", "1721454", dstring);
        // 1.1.1 (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("common", "1721424", dstring);
        // 1.1.1 QU(gregorian)
        try {
            cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1 QU", false, MCRCalendar.TAG_JULIAN);
        } catch (MCRException e) {
            cal = new GregorianCalendar();
        }
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal);
        assertEquals("common", cstring, dstring);
        // - infinity (julian)
        cal.set(Calendar.JULIAN_DAY, MCRCalendar.MIN_JULIAN_DAY_NUMBER);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 01.01.4713 BC", "0", dstring);
        // + infinity (julian)
        cal.set(Calendar.JULIAN_DAY, MCRCalendar.MAX_JULIAN_DAY_NUMBER);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 28.01.4000 AD", "3182057", dstring);

        // 02.01.4713 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("02.01.4713 bc", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 02.01.4713 BC", "1", dstring);
        // 01.01.0814 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("-814", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 01.01.0814 BC", "1424110", dstring);
        // 01.01.0814 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("-01.01.814", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 01.01.0814 BC", "1424110", dstring);
        // 15.03.0044 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("BC 15.03.44", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 15.03.0044 BC", "1705426", dstring);
        // 01.01.0001 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0001 BC", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 01.01.0001 BC", "1721058", dstring);
        // 31.12.0001 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("31.12.0001 v. Chr", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 31.12.0001 BC", "1721423", dstring);
        // 01.01.0000 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0000", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 01.01.0000 AD", "1721058", dstring);
        // 01.01.0001 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.01 AD", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian", "1721424", dstring);
        // 04.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.1582 N. Chr", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 04.10.1582 AD", "2299160", dstring);
        // 05.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 05.10.1582 AD", "2299161", dstring);
        // 06.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.1582", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 06.10.1582 AD", "2299162", dstring);
        // 15.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 15.10.1582 AD", "2299171", dstring);
        // 16.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 16.10.1582 AD", "2299172", dstring);
        // 28.02.1700 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("28.02.1700", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 28.02.1700 AD", "2342041", dstring);
        // 29.02.1700 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("29.02.1700", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 29.02.1700 AD", "2342042", dstring);
        // 01.03.1700 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.03.1700", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 01.03.1700 AD", "2342043", dstring);
        // 28.02.1800 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("28.02.1800", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 28.02.1800 AD", "2378566", dstring);
        // 29.02.1800 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("29.02.1800", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 29.02.1800 AD", "2378567", dstring);
        // 01.03.1800 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.03.1800", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 01.03.1800 AD", "2378568", dstring);
        // 29.02.1900 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("29.02.1900", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 29.02.1900 AD", "2415092", dstring);
        // 29.02.2100 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("29.02.2100", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("julian: 29.02.2100 AD", "2488142", dstring);

        /* gregorian date check */
        // 04.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("gregorian: 04.10.1582 AD", "2299160", dstring);
        // 05.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("gregorian: 05.10.1582 AD", "2299161", dstring);
        // 15.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("gregorian: 15.10.1582 AD", "2299161", dstring);
        // 06.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("gregorian: 06.10.1582 AD", "2299162", dstring);
        // 31.01.1800 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1800", true, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("gregorian: 31.01.1800 AD", "2378527", dstring);
        // 31.01.1800 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1/1800", true, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("gregorian: 31.01.1800 AD", "2378527", dstring);
        // 01.31.1800 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("9/1/1800", true, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("gregorian: 01.31.1800 AD", "2378505", dstring);
        // 24.02.1964 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24", true, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("gregorian: 1964-02-24 AD", "2438450", dstring);

        // 01.01.0001 h. (islamic)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0001 h.", false, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("islamic: 01.01.0001 H.", "1948440", dstring);
        // 01.01.800 H. (islamic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", false, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("islamic: 01.01.800 H.", "2231579", dstring);
        // 30.01.800 H. (islamic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", true, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("islamic: 30.01.800 H.", "2231608", dstring);
        // 29.12.800 H. (islamic)
        cal = MCRCalendar.getHistoryDateAsCalendar("800", true, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("islamic: 29.12.800 H.", "2231932", dstring);

        // 01.01.0001 A.M. (coptioc)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1 a.M.", false, MCRCalendar.TAG_COPTIC);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("coptic: 01.01.0001 A.M.", "1825030", dstring);
        // 01.01.1724 A.M. (coptioc)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1724 A.M.", false, MCRCalendar.TAG_COPTIC);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("coptic: 01.01.1724 A.M.", "2454356", dstring);
        // 05.13.1724 E.E. (coptic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1724 a.M.", true, MCRCalendar.TAG_COPTIC);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("coptic: 05.13.2000 E.E.", "2454720", dstring);
        // 01.01.0001 E.E. (ethiopic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 E.E.", false, MCRCalendar.TAG_ETHIOPIC);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("coptic: 01.01.0001 E.E.", "1724221", dstring);
        // 05.13.2000 E.E. (ethiopic)
        cal = MCRCalendar.getHistoryDateAsCalendar("2000 E.E.", true, MCRCalendar.TAG_ETHIOPIC);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("coptic: 05.13.2000 E.E.", "2454720", dstring);
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRCalendar.getJulianDayNumberAsString(Calendar)'
     */
    @Test
    public void getDateToFormattedStringForCalendar() {
        Calendar calendar;
        String dstring;

        // 15.03.44 BC (julian)
        calendar = MCRCalendar.getHistoryDateAsCalendar("-15.3.44", true, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy G");
        assertEquals("is not julian date 15.03.44 BC", "15.03.0044 BC", dstring);
        // 15.03.44 BC (gregorian)
        calendar = MCRCalendar.getHistoryDateAsCalendar("-15.3.44", true, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy G");
        assertEquals("is not gregorian date 15.03.44 BC", "15.03.0044 BC", dstring);
        // 29.02.1700 BC (julian)
        calendar = MCRCalendar.getHistoryDateAsCalendar("29.02.1700", true, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy G");
        assertEquals("is not julian date 11.03.1700 AD", "11.03.1700 AD", dstring);
        // 29.02.1700 BC (julian)
        calendar = MCRCalendar.getHistoryDateAsCalendar("29.02.1700", true, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy G");
        assertEquals("is not julian date 01.03.1700 AD", "01.03.1700 AD", dstring);
        // 30.01.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("30.1.800 H.", true, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("is not islamic date 30.01.0800 H.", "30.01.800 h.", dstring);
        // 01.01.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", false, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("is not islamic date 01.01.0800 H.", "01.01.800 h.", dstring);
        // 30.01.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", true, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("is not islamic date 30.01.0800 H.", "30.01.800 h.", dstring);
        // 01.01.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("800", false, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("is not islamic date 01.01.0800 H.", "01.01.800 h.", dstring);
        // 29.12.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("800", true, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("is not islamic date 29.12.0800 H.", "29.12.800 h.", dstring);
        // 29.12.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("800 H.", true, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("is not islamic date 29.12.0800 H.", "29.12.800 h.", dstring);

        // 01.01.1724 A.M. (coptic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("1724", false, MCRCalendar.TAG_COPTIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("is not coptic date 01.01.1724 A.M.", "01.01.1724 A.M.", dstring);

        // 01.01.2000 A.M. (ethiopic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("2000", true, MCRCalendar.TAG_ETHIOPIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("is not ethiopic date 05.13.2000 E.E.", "05.13.2000 E.E.", dstring);
    }
}

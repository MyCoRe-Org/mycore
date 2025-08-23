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

package org.mycore.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ibm.icu.util.BuddhistCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CopticCalendar;
import com.ibm.icu.util.EthiopicCalendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.JapaneseCalendar;

/**
 * This class is a JUnit test case for org.mycore.common.MCRCalendar.
 *
 * @author Jens Kupferschmidt
 */
public class MCRCalendarTest {

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRCalendar.getDateToFormattedString()'
     */
    @Test
    public void getDateToFormattedString() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.set(1964, 1, 24);
        String dstring;
        Calendar cal;
        // gregorian without format
        cal = (GregorianCalendar) gregorianCalendar.clone();
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal);
        assertEquals("1964-02-24 AD", dstring, "calendar without format");
        // gregorian with format
        cal = (GregorianCalendar) gregorianCalendar.clone();
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal, "dd.MM.yyyy G");
        assertEquals("24.02.1964 AD", dstring, "calendar with format dd.MM.yyyy G");
        // gregorian with format
        cal = (GregorianCalendar) gregorianCalendar.clone();
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal, "yyyy-MM-dd");
        assertEquals("1964-02-24", dstring, "gregorian with format yyyy-MM-dd");
    }

    @Test
    public void testParseGregorianDate() {
        Calendar cal;

        // 04.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 4, Calendar.OCTOBER, 1582, GregorianCalendar.AD);
        assertEquals(2299160, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("2299160", MCRCalendar.getJulianDayNumberAsString(cal));

        // 05.10.1582 AD (gregorian) -> 15.10.1582 (https://en.wikipedia.org/wiki/Inter_gravissimas)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 15, Calendar.OCTOBER, 1582, GregorianCalendar.AD);

        assertEquals(2299161, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("2299161", MCRCalendar.getJulianDayNumberAsString(cal));

        // 06.10.1582 AD (gregorian) -> 16.10.1582 in the ICU implementation
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 16, Calendar.OCTOBER, 1582, GregorianCalendar.AD);

        assertEquals(2299162, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("2299162", MCRCalendar.getJulianDayNumberAsString(cal));

        // 15.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 15, Calendar.OCTOBER, 1582, GregorianCalendar.AD);

        assertEquals(2299161, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("2299161", MCRCalendar.getJulianDayNumberAsString(cal));

        // 16.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 16, Calendar.OCTOBER, 1582, GregorianCalendar.AD);

        assertEquals(2299162, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("2299162", MCRCalendar.getJulianDayNumberAsString(cal));

        // 01.01.1800 AD (gregorian) with missing day and last=false
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1800", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 1, Calendar.JANUARY, 1800, GregorianCalendar.AD);

        assertEquals(2378497, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("2378497", MCRCalendar.getJulianDayNumberAsString(cal));

        // 31.01.1800 AD (gregorian) with missing day and last=true
        cal = MCRCalendar.getHistoryDateAsCalendar("1/1800", true, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 31, Calendar.JANUARY, 1800, GregorianCalendar.AD);

        assertEquals(2378527, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("2378527", MCRCalendar.getJulianDayNumberAsString(cal));

        // 09.01.1800 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("9/1/1800", true, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 9, Calendar.JANUARY, 1800, GregorianCalendar.AD);

        assertEquals(2378505, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("2378505", MCRCalendar.getJulianDayNumberAsString(cal));

        // 24.02.1964 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24", true, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 24, Calendar.FEBRUARY, 1964, GregorianCalendar.AD);

        assertEquals(2438450, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("2438450", MCRCalendar.getJulianDayNumberAsString(cal));

        // 1 BC with last=true (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 BC", true, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 31, Calendar.DECEMBER, 1, GregorianCalendar.BC);

        assertEquals(1721423, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("1721423", MCRCalendar.getJulianDayNumberAsString(cal));
    }

    @Test
    public void testParseJulianDate() {
        // 02.01.4713 BC (julian)
        Calendar cal = MCRCalendar.getHistoryDateAsCalendar("02.01.4713 bc", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 2, GregorianCalendar.JANUARY, 4713, GregorianCalendar.BC);
        assertCalEqualWithJulianDay(cal, 1);

        // 01.01.0814 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("-814", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 1, GregorianCalendar.JANUARY, 814, GregorianCalendar.BC);
        assertCalEqualWithJulianDay(cal, 1424110);

        // 01.01.0814 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("-01.01.814", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 1, GregorianCalendar.JANUARY, 814, GregorianCalendar.BC);
        assertCalEqualWithJulianDay(cal, 1424110);

        // 15.03.0044 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("BC 15.03.44", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 15, GregorianCalendar.MARCH, 44, GregorianCalendar.BC);
        assertCalEqualWithJulianDay(cal, 1705426);

        // 01.01.0001 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0001 BC", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 1, GregorianCalendar.JANUARY, 1, GregorianCalendar.BC);
        assertCalEqualWithJulianDay(cal, 1721058);

        // 31.12.0001 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("31.12.0001 v. Chr", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 31, GregorianCalendar.DECEMBER, 1, GregorianCalendar.BC);
        assertCalEqualWithJulianDay(cal, 1721423);

        // 01.01.0000 -> 1.1.1 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0000", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 1, GregorianCalendar.JANUARY, 1, GregorianCalendar.BC);
        assertCalEqualWithJulianDay(cal, 1721058);

        // 01.01.0001 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.01 AD", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 1, GregorianCalendar.JANUARY, 1, GregorianCalendar.AD);
        assertCalEqualWithJulianDay(cal, 1721424);

        // 04.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.1582 N. Chr", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 4, GregorianCalendar.OCTOBER, 1582, GregorianCalendar.AD);
        assertCalEqualWithJulianDay(cal, 2299160);

        // 05.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 5, GregorianCalendar.OCTOBER, 1582, GregorianCalendar.AD);
        assertCalEqualWithJulianDay(cal, 2299161);

        // 06.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 6, GregorianCalendar.OCTOBER, 1582, GregorianCalendar.AD);
        assertCalEqualWithJulianDay(cal, 2299162);

        // 15.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 15, GregorianCalendar.OCTOBER, 1582, GregorianCalendar.AD);
        assertCalEqualWithJulianDay(cal, 2299171);

        // 16.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 16, GregorianCalendar.OCTOBER, 1582, GregorianCalendar.AD);
        assertCalEqualWithJulianDay(cal, 2299172);

        // 28.02.1700 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("28.02.1700", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 28, GregorianCalendar.FEBRUARY, 1700, GregorianCalendar.AD);
        assertCalEqualWithJulianDay(cal, 2342041);

        // 29.02.1700 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("29.02.1700", false, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 29, GregorianCalendar.FEBRUARY, 1700, GregorianCalendar.AD);
        assertCalEqualWithJulianDay(cal, 2342042);

        // 1 BC with last=true (jul)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 BC", true, MCRCalendar.TAG_JULIAN);
        assertCalEqualWithActual(cal, 31, GregorianCalendar.DECEMBER, 1, GregorianCalendar.BC);
        assertCalEqualWithJulianDay(cal, 1721423);
    }

    private void assertCalEqualWithJulianDay(Calendar cal, int number) {
        assertEquals(MCRCalendar.getJulianDayNumber(cal), number);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), String.valueOf(number));
    }

    @Test
    public void testParseIslamicDate() {
        Calendar cal;

        // 01.01.0001 h. (islamic)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0001 h.", false, MCRCalendar.TAG_ISLAMIC);
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(IslamicCalendar.MUHARRAM, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.YEAR));

        // first day of Islamic calendar is 16.7.622 in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("16.7.622", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(greg), MCRCalendar.getJulianDayNumber(cal));
        assertEquals(1948440, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("1948440", MCRCalendar.getJulianDayNumberAsString(cal));

        // 01.01.800 H. (islamic) -> 24.09.1397 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", false, MCRCalendar.TAG_ISLAMIC);
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(IslamicCalendar.MUHARRAM, cal.get(Calendar.MONTH));
        assertEquals(800, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("24.09.1397", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 30.01.800 H. (islamic) -> 23.10.1397 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", true, MCRCalendar.TAG_ISLAMIC);
        assertEquals(30, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(IslamicCalendar.MUHARRAM, cal.get(Calendar.MONTH));
        assertEquals(800, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("23.10.1397", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 29.12.800 H. (islamic) -> 12.09.1398 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("800", true, MCRCalendar.TAG_ISLAMIC);
        assertEquals(29, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(IslamicCalendar.DHU_AL_HIJJAH, cal.get(Calendar.MONTH));
        assertEquals(800, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("12.09.1398", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // -1 (isl) -> 15.07.622 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_ISLAMIC);
        assertEquals(29, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(IslamicCalendar.DHU_AL_HIJJAH, cal.get(Calendar.MONTH));
        assertEquals(0, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("15.7.622", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

    }

    @Test
    public void testParseCopticDate() {
        Calendar cal;

        // 01.01.0001 A.M. (coptic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1 a.M.", false, MCRCalendar.TAG_COPTIC);
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(CopticCalendar.TOUT, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.YEAR));

        // first day of Coptic calendar is 29.8.284 in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("29.8.284", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(greg), MCRCalendar.getJulianDayNumber(cal));
        assertEquals(1825030, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("1825030", MCRCalendar.getJulianDayNumberAsString(cal));

        // 01.01.1724 A.M. (coptic) -> 12.09.2007
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1724 A.M.", false, MCRCalendar.TAG_COPTIC);
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(CopticCalendar.TOUT, cal.get(Calendar.MONTH));
        assertEquals(1724, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("12.09.2007", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 05.13.1724 E.E. (coptic) -> 10.09.2008
        cal = MCRCalendar.getHistoryDateAsCalendar("1724 a.M.", true, MCRCalendar.TAG_COPTIC);
        assertEquals(5, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(CopticCalendar.NASIE, cal.get(Calendar.MONTH));
        assertEquals(1724, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("10.09.2008", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // -5.13.1 (cop) -> 28.8.284
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_COPTIC);
        assertEquals(5, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(CopticCalendar.NASIE, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.YEAR));
        assertEquals(GregorianCalendar.BC, cal.get(Calendar.ERA));

        greg = MCRCalendar.getHistoryDateAsCalendar("28.8.284", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));
    }

    @Test
    public void testParseEthiopianDate() {
        Calendar cal;

        // 01.01.0001 E.E. (ethiopic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 E.E.", false, MCRCalendar.TAG_ETHIOPIC);
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(EthiopicCalendar.MESKEREM, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.YEAR));

        // first day of Ehtiopian calendar is 29.8.8 in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("29.8.8", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(greg), MCRCalendar.getJulianDayNumber(cal));
        assertEquals(1724221, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("1724221", MCRCalendar.getJulianDayNumberAsString(cal));

        // 05.13.2000 E.E. (ethiopic) -> 10.09.2008 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("2000 E.E.", true, MCRCalendar.TAG_ETHIOPIC);
        assertEquals(5, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(EthiopicCalendar.PAGUMEN, cal.get(Calendar.MONTH));
        assertEquals(2000, cal.get(Calendar.YEAR));
        assertEquals(1, cal.get(Calendar.ERA));

        greg = MCRCalendar.getHistoryDateAsCalendar("10.09.2008", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // years before 0 are represented in Amete Alem format (starting with count from 5500 BC)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_ETHIOPIC);
        assertEquals(5, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(EthiopicCalendar.PAGUMEN, cal.get(Calendar.MONTH));
        assertEquals(5500, cal.get(Calendar.YEAR));
        assertEquals(0, cal.get(Calendar.ERA));

        greg = MCRCalendar.getHistoryDateAsCalendar("28.8.8", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));
    }

    @Test
    public void testParseHebrewDate() {
        Calendar cal;

        // 1.1.1 (hebrew)
        cal = MCRCalendar.getHistoryDateAsCalendar("1", false, MCRCalendar.TAG_HEBREW);
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(HebrewCalendar.TISHRI, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.YEAR));

        // first day of Hebrew calendar is 7.10.3761 BC in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("7.10.3761 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(347998, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("347998", MCRCalendar.getJulianDayNumberAsString(cal));

        // 04.10.1582 (hebrew) - 29.04.2178 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_HEBREW);
        assertEquals(4, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(HebrewCalendar.SIVAN, cal.get(Calendar.MONTH));
        assertEquals(1582, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("17.05.2179 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 18.01.5343 (hebrew) -> 04.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("18.01.5343", false, MCRCalendar.TAG_HEBREW);
        assertEquals(18, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(HebrewCalendar.TISHRI, cal.get(Calendar.MONTH));
        assertEquals(5343, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 19.01.5343 (hebrew) -> 15.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("19.01.5343", false, MCRCalendar.TAG_HEBREW);
        assertEquals(19, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(HebrewCalendar.TISHRI, cal.get(Calendar.MONTH));
        assertEquals(5343, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 19.01.5343 (hebrew) -> 16.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("20.01.5343", false, MCRCalendar.TAG_HEBREW);
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(HebrewCalendar.TISHRI, cal.get(Calendar.MONTH));
        assertEquals(5343, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 01.01.1800 (hebrew) with missing day and last=false
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1800", false, MCRCalendar.TAG_HEBREW);
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(HebrewCalendar.TISHRI, cal.get(Calendar.MONTH));
        assertEquals(1800, cal.get(Calendar.YEAR));

        greg = MCRCalendar.getHistoryDateAsCalendar("09.09.1962 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // -1 (hebrew) not supported
        assertThrows(MCRException.class,
            () -> MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_HEBREW));
    }

    @Test
    public void testParseBuddhistDate() {
        Calendar cal;

        // 1.1.1 (buddhist)
        cal = MCRCalendar.getHistoryDateAsCalendar("1", false, MCRCalendar.TAG_BUDDHIST);
        assertCalEqualWithActual(cal, 1, BuddhistCalendar.JANUARY, 1);

        // first day of Buddhist calendar is 1.1.543 BC in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("1.1.543 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(1523093, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("1523093", MCRCalendar.getJulianDayNumberAsString(cal));

        // year 0
        cal = MCRCalendar.getHistoryDateAsCalendar("0", false, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("1.1.544 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 1, BuddhistCalendar.JANUARY, 0);
        assertGregEqualWithCal(greg, cal);

        // year -1
        cal = MCRCalendar.getHistoryDateAsCalendar("-1.1.1", false, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("1.1.544 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 1, BuddhistCalendar.JANUARY, 0);
        assertGregEqualWithCal(greg, cal);

        // year -100
        cal = MCRCalendar.getHistoryDateAsCalendar("-100", false, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("1.1.643 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 1, BuddhistCalendar.JANUARY, -99);
        assertGregEqualWithCal(greg, cal);

        // 04.10.2125 (buddhist) -> year 1582 in gregorian calendar
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 4, BuddhistCalendar.OCTOBER, 2125);
        assertGregEqualWithCal(greg, cal);

        // 05.10.2125 (buddhist) -> 15.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 15, BuddhistCalendar.OCTOBER, 2125);
        assertGregEqualWithCal(greg, cal);

        // 06.10.2125 (buddhist) -> 15.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 15, BuddhistCalendar.OCTOBER, 2125);
        assertGregEqualWithCal(greg, cal);

        // 15.10.2125 (buddhist) -> 15.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 15, BuddhistCalendar.OCTOBER, 2125);
        assertGregEqualWithCal(greg, cal);

        // 16.10.2125 (buddhist) -> 15.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 16, BuddhistCalendar.OCTOBER, 2125);
        assertGregEqualWithCal(greg, cal);

        // 01.01.1800 (buddhist) with missing day and last=false -> 01.01.257 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1800", false, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("01.01.1257", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 1, BuddhistCalendar.JANUARY, 1800);
        assertGregEqualWithCal(greg, cal);

        // 24.02.1964 (buddhist) -> 24.04142 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24", true, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("24.02.1421", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 24, BuddhistCalendar.FEBRUARY, 1964);
        assertGregEqualWithCal(greg, cal);

        // 24.02.1964 BE (buddhist) -> 24.02.2507 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24 B.E.", true, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("24.02.2507 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 24, BuddhistCalendar.FEBRUARY, -1963);
        assertGregEqualWithCal(greg, cal);

        // -1 (buddhist) -> 24.02.2507 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_BUDDHIST);
        greg = MCRCalendar.getHistoryDateAsCalendar("31.12.544 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 31, BuddhistCalendar.DECEMBER, 0);
        assertGregEqualWithCal(greg, cal);
    }

    @Test
    public void testParsePersianDate() {
        Calendar cal;
        // 01.01.0001  (persian) -> 22.3.622 greg
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_PERSIC);
        assertCalEqualWithActual(cal, 21, GregorianCalendar.MARCH, 622);

        // first day of Persian calendar is 21.3.622 in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("21.3.622", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(1948323, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("1948323", MCRCalendar.getJulianDayNumberAsString(cal));

        // 01.01.800 (persian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800", false, MCRCalendar.TAG_PERSIC);
        greg = MCRCalendar.getHistoryDateAsCalendar("21.03.1421", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 21, GregorianCalendar.MARCH, 1421);
        assertGregEqualWithCal(greg, cal);

        // 31.01.800 (persian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800", true, MCRCalendar.TAG_PERSIC);
        greg = MCRCalendar.getHistoryDateAsCalendar("20.04.1421", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 20, GregorianCalendar.APRIL, 1421);
        assertGregEqualWithCal(greg, cal);

        // 29.12.800 (persian)
        cal = MCRCalendar.getHistoryDateAsCalendar("800", true, MCRCalendar.TAG_PERSIC);
        greg = MCRCalendar.getHistoryDateAsCalendar("20.03.1422", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 20, GregorianCalendar.MARCH, 1422);
        assertGregEqualWithCal(greg, cal);

        // gregorian calendar reform on October, 5th 1582 -> skip days between 5 and 15
        cal = MCRCalendar.getHistoryDateAsCalendar("12.7.961", false, MCRCalendar.TAG_PERSIC);
        greg = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 4, GregorianCalendar.OCTOBER, 1582);
        assertGregEqualWithCal(greg, cal);

        cal = MCRCalendar.getHistoryDateAsCalendar("13.7.961", false, MCRCalendar.TAG_PERSIC);
        greg = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 15, GregorianCalendar.OCTOBER, 1582);
        assertGregEqualWithCal(greg, cal);

        cal = MCRCalendar.getHistoryDateAsCalendar("14.7.961", false, MCRCalendar.TAG_PERSIC);
        greg = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 16, GregorianCalendar.OCTOBER, 1582);
        assertGregEqualWithCal(greg, cal);

        // -1.1.1 (pers) -> 22.03.621 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", false, MCRCalendar.TAG_PERSIC);
        greg = MCRCalendar.getHistoryDateAsCalendar("21.03.621", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 21, GregorianCalendar.MARCH, 621);
        assertGregEqualWithCal(greg, cal);

        // -29.12.1 (pers) -> 21.03.621 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_PERSIC);
        greg = MCRCalendar.getHistoryDateAsCalendar("21.03.622", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 21, GregorianCalendar.MARCH, 622);
        assertGregEqualWithCal(greg, cal);
    }

    @Test
    public void testParseArmenianDate() {
        assertHistoryDates("1.1.1", 13, GregorianCalendar.JULY, 552, "13.7.552", false);
        assertHistoryDates("1.2.1", 12, GregorianCalendar.AUGUST, 552, "12.08.552", false);
        assertHistoryDates("5.13.1", 12, GregorianCalendar.JULY, 553, "12.07.553", false);

        assertHistoryDates("2.9.48", 28, GregorianCalendar.FEBRUARY, 600, "28.02.600", false);
        assertHistoryDates("3.9.48", 29, GregorianCalendar.FEBRUARY, 600, "29.02.600", false);
        assertHistoryDates("4.9.48", 1, GregorianCalendar.MARCH, 600, "01.03.600", false);

        assertHistoryDates("1.1.1462", 26, GregorianCalendar.JULY, 2012, "26.07.2012", false);
        assertHistoryDates("1.1.101", 18, GregorianCalendar.JUNE, 652, "18.06.652", false);
        assertHistoryDates("1.1.1031", 29, GregorianCalendar.OCTOBER, 1581, "29.10.1581", false);

        assertHistoryDates("11.12.1031", 4, GregorianCalendar.OCTOBER, 1582, "04.10.1582", false);
        assertHistoryDates("12.12.1031", 15, GregorianCalendar.OCTOBER, 1582, "15.10.1582", false);
        assertHistoryDates("13.12.1031", 16, GregorianCalendar.OCTOBER, 1582, "16.10.1582", false);

        assertHistoryDates("12.500", 4, GregorianCalendar.FEBRUARY, 1052, "04.02.1052", false);
        assertHistoryDates("12.500", 4, GregorianCalendar.MARCH, 1052, "04.03.1052", true);

        assertHistoryDates("500", 11, GregorianCalendar.MARCH, 1051, "11.03.1051", false);
        assertHistoryDates("500", 9, GregorianCalendar.MARCH, 1052, "09.03.1052", true);

        assertHistoryDates("-1", 12, GregorianCalendar.JULY, 552, "12.07.552", true);
    }

    @Test
    public void testParseEgyptianDate() {
        Calendar cal;

        // 01.01.0001  (egyptian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_EGYPTIAN);
        assertCalEqualWithActual(cal, 18, GregorianCalendar.FEBRUARY, 747, GregorianCalendar.BC);

        // first day of Egyptian calendar is 18.2.747 BC in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("18.2.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(1448630, MCRCalendar.getJulianDayNumber(cal));
        assertEquals("1448630", MCRCalendar.getJulianDayNumberAsString(cal));

        // 1.1 -> 30.1.1 in Gregorian date: 19.3.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1 A.N.", true, MCRCalendar.TAG_EGYPTIAN);
        greg = MCRCalendar.getHistoryDateAsCalendar("19.03.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 19, GregorianCalendar.MARCH, 747, GregorianCalendar.BC);
        assertGregEqualWithCal(greg, cal);

        // 10.1 (last=false) -> 1.10.1 in Gregorian date: 14.12.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("10.1 A.N.", false, MCRCalendar.TAG_EGYPTIAN);
        greg = MCRCalendar.getHistoryDateAsCalendar("15.11.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 15, GregorianCalendar.NOVEMBER, 747, GregorianCalendar.BC);
        assertGregEqualWithCal(greg, cal);

        // 10.1 (last=true) -> 30.10.1 in Gregorian date: 14.12.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("10.1 A.N.", true, MCRCalendar.TAG_EGYPTIAN);
        greg = MCRCalendar.getHistoryDateAsCalendar("14.12.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 14, GregorianCalendar.DECEMBER, 747, GregorianCalendar.BC);
        assertGregEqualWithCal(greg, cal);

        // 1.2.1 -> in Gregorian date: 20.3.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("1.2.1 A.N.", false, MCRCalendar.TAG_EGYPTIAN);
        greg = MCRCalendar.getHistoryDateAsCalendar("20.03.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 20, GregorianCalendar.MARCH, 747, GregorianCalendar.BC);
        assertGregEqualWithCal(greg, cal);

        // 13.1 (last=false) -> 1.13.1 (in Gregorian date: 17.2.746)
        cal = MCRCalendar.getHistoryDateAsCalendar("13.1 A.N.", false, MCRCalendar.TAG_EGYPTIAN);
        greg = MCRCalendar.getHistoryDateAsCalendar("13.02.746 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 13, GregorianCalendar.FEBRUARY, 746, GregorianCalendar.BC);
        assertGregEqualWithCal(greg, cal);

        // 1 (last=true) -> 5.13.1 (in Gregorian date: 17.2.746)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 A.N.", true, MCRCalendar.TAG_EGYPTIAN);
        greg = MCRCalendar.getHistoryDateAsCalendar("17.02.746 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 17, GregorianCalendar.FEBRUARY, 746, GregorianCalendar.BC);
        assertGregEqualWithCal(greg, cal);

        // -1 (last=true) -> 17.02.747 BC (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_EGYPTIAN);
        greg = MCRCalendar.getHistoryDateAsCalendar("17.02.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, 17, GregorianCalendar.FEBRUARY, 747, GregorianCalendar.BC);
        assertGregEqualWithCal(greg, cal);
    }

    @Test
    public void testParseJapaneseDate() {
        Calendar cal;

        // Meiji era: 8.9.1868 - 29.07.1912
        cal = MCRCalendar.getHistoryDateAsCalendar("8.9.M1", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 8, JapaneseCalendar.SEPTEMBER, 1, JapaneseCalendar.MEIJI);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("8.9.1868", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("29.7.M45", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 29, JapaneseCalendar.JULY, 45, JapaneseCalendar.MEIJI);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("29.7.1912", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("30.7.M45", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 30, JapaneseCalendar.JULY, 1, JapaneseCalendar.TAISHO);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("30.7.1912", false, MCRCalendar.TAG_GREGORIAN)));

        // Taisho era: 30.7.1912-24.12.1926
        cal = MCRCalendar.getHistoryDateAsCalendar("30.7.T1", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 30, JapaneseCalendar.JULY, 1, JapaneseCalendar.TAISHO);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("30.7.1912", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("24.12.T15", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 24, JapaneseCalendar.DECEMBER, 15, JapaneseCalendar.TAISHO);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("24.12.1926", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("25.12.T15", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 25, JapaneseCalendar.DECEMBER, 1, JapaneseCalendar.SHOWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("25.12.1926", false, MCRCalendar.TAG_GREGORIAN)));

        // Showa era: 25.12.1926-07.01.1989
        cal = MCRCalendar.getHistoryDateAsCalendar("25.12.S1", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 25, JapaneseCalendar.DECEMBER, 1, JapaneseCalendar.SHOWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("25.12.1926", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("7.1.S64", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 7, JapaneseCalendar.JANUARY, 64, JapaneseCalendar.SHOWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("7.1.1989", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("8.1.S64", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 8, JapaneseCalendar.JANUARY, 1, JapaneseCalendar.HEISEI);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("8.1.1989", false, MCRCalendar.TAG_GREGORIAN)));

        // Heisei era: 08.01.1989-30.04.2019
        cal = MCRCalendar.getHistoryDateAsCalendar("8.1.H1", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 8, JapaneseCalendar.JANUARY, 1, JapaneseCalendar.HEISEI);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("8.1.1989", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("30.4.H31", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 30, JapaneseCalendar.APRIL, 31, JapaneseCalendar.HEISEI);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("30.4.2019", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("1.5.H31", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 1, JapaneseCalendar.MAY, 1, JapaneseCalendar.REIWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("1.5.2019", false, MCRCalendar.TAG_GREGORIAN)));

        // Reiwa era: 01.05.2019 - present
        cal = MCRCalendar.getHistoryDateAsCalendar("1.5.R1", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 1, JapaneseCalendar.MAY, 1, JapaneseCalendar.REIWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("1.5.2019", false, MCRCalendar.TAG_GREGORIAN)));

        // check ISO format
        cal = MCRCalendar.getHistoryDateAsCalendar("R1-5-1", true, MCRCalendar.TAG_JAPANESE);
        assertCalEqualWithActual(cal, 1, JapaneseCalendar.MAY, 1, JapaneseCalendar.REIWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(
            MCRCalendar.getHistoryDateAsCalendar("1.5.2019", false, MCRCalendar.TAG_GREGORIAN)));
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRCalendar.getHistoryDateAsCalendar(String, boolean, String)'
     */
    @Test
    public void getHistoryDateAsCalendar() {
        String cstring;
        String dstring;
        cstring = MCRCalendar.getCalendarDateToFormattedString(new GregorianCalendar());
        Calendar cal;

        /* check julian calendar implementation */
        // all entries are empty
        try {
            cal = MCRCalendar.getHistoryDateAsCalendar(null, false, MCRCalendar.CalendarType.ISLAMIC);
        } catch (MCRException e) {
            cal = new GregorianCalendar();
        }
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal);
        assertEquals(cstring, dstring, "Date is not the current date.");
        // 0A.01.0001 BC (wrong gregorian)
        try {
            cal = MCRCalendar.getHistoryDateAsCalendar("-0A.01.0001", false, MCRCalendar.TAG_JULIAN);
        } catch (MCRException e) {
            cal = new GregorianCalendar();
        }
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal);
        assertEquals(cstring, dstring, "common");

        /* syntax expanding check */
        // 1 (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("1721424", dstring, "common");
        // 1.1 (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1", true, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("1721454", dstring, "common");
        // 1.1.1 (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("1721424", dstring, "common");
        // 1.1.1 QU(gregorian)
        try {
            cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1 QU", false, MCRCalendar.TAG_JULIAN);
        } catch (MCRException e) {
            cal = new GregorianCalendar();
        }
        dstring = MCRCalendar.getCalendarDateToFormattedString(cal);
        assertEquals(cstring, dstring, "common");
        // - infinity (julian)
        cal.set(Calendar.JULIAN_DAY, MCRCalendar.MIN_JULIAN_DAY_NUMBER);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("0", dstring, "julian: 01.01.4713 BC");
        // + infinity (julian)
        cal.set(Calendar.JULIAN_DAY, MCRCalendar.MAX_JULIAN_DAY_NUMBER);
        dstring = MCRCalendar.getJulianDayNumberAsString(cal);
        assertEquals("3182057", dstring, "julian: 28.01.4000 AD");
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
        assertEquals("15.03.0044 BC", dstring, "is not julian date 15.03.44 BC");
        // 15.03.44 BC (gregorian)
        calendar = MCRCalendar.getHistoryDateAsCalendar("-15.3.44", true, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy G");
        assertEquals("15.03.0044 BC", dstring, "is not gregorian date 15.03.44 BC");
        // 29.02.1700 (julian)
        calendar = MCRCalendar.getHistoryDateAsCalendar("29.02.1700", true, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy G");
        assertEquals("29.02.1700 AD", dstring, "is not julian date 29.02.1700 AD");
        // 29.02.1700 (gregorian) -> no leap year in gregorian calendar
        assertThrows(MCRException.class,
            () -> MCRCalendar.getHistoryDateAsCalendar("29.02.1700", true, MCRCalendar.TAG_GREGORIAN));
        // 30.01.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("30.1.800 H.", true, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("30.01.800 h.", dstring, "is not islamic date 30.01.0800 H.");
        // 01.01.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", false, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("01.01.800 h.", dstring, "is not islamic date 01.01.0800 H.");
        // 30.01.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", true, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("30.01.800 h.", dstring, "is not islamic date 30.01.0800 H.");
        // 01.01.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("800", false, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("01.01.800 h.", dstring, "is not islamic date 01.01.0800 H.");
        // 29.12.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("800", true, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("29.12.800 h.", dstring, "is not islamic date 29.12.0800 H.");
        // 29.12.800 H. (islamic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("800 H.", true, MCRCalendar.TAG_ISLAMIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyy");
        assertEquals("29.12.800 h.", dstring, "is not islamic date 29.12.0800 H.");

        // 01.01.1724 A.M. (coptic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("1724", false, MCRCalendar.TAG_COPTIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy");
        assertEquals("01.01.1724 A.M.", dstring, "is not coptic date 01.01.1724 A.M.");

        // 01.01.2000 A.M. (ethiopic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("2000", true, MCRCalendar.TAG_ETHIOPIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy");
        assertEquals("05.13.2000 E.E.", dstring, "is not ethiopic date 05.13.2000 E.E.");

        calendar = MCRCalendar.getHistoryDateAsCalendar("1.1.500", false, MCRCalendar.TAG_ARMENIAN);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy");
        assertEquals("11.03.1051", dstring, "is not armenian date 11.03.1051");

        calendar = MCRCalendar.getHistoryDateAsCalendar("5.7.H2", false, MCRCalendar.TAG_JAPANESE);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.Y");
        assertEquals("05.07.1990", dstring, "is not japanese date 05.07.1990");

        calendar = MCRCalendar.getHistoryDateAsCalendar("2.7.20", false, MCRCalendar.TAG_BUDDHIST);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yy");
        assertEquals("02.07.20", dstring, "is not japanese date 02.07.20");

        calendar = MCRCalendar.getHistoryDateAsCalendar("2.7.20", false, MCRCalendar.TAG_BUDDHIST);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.Y");
        assertEquals("02.07.-523", dstring, "is not buddhist date 02.07.-523");
    }

    @Test
    public void testIsoFormat() {
        assertFalse(MCRCalendar.isoFormat("23.03.2022"));
        assertFalse(MCRCalendar.isoFormat("23/03/2022"));
        assertTrue(MCRCalendar.isoFormat("23-03-2022"));
        assertFalse(MCRCalendar.isoFormat("-23.03.2022"));
        assertFalse(MCRCalendar.isoFormat("-23/03/2022"));
        assertTrue(MCRCalendar.isoFormat("-23-03-2022"));

        assertFalse(MCRCalendar.isoFormat("800"));
    }

    @Test
    public void testDelimiter() {
        assertEquals(".", MCRCalendar.delimiter("23.03.2022"));
        assertEquals("/", MCRCalendar.delimiter("23/03/2022"));
        assertEquals("-", MCRCalendar.delimiter("23-03-2022"));
        assertEquals(".", MCRCalendar.delimiter("-23.03.2022"));
        assertEquals("/", MCRCalendar.delimiter("-23/03/2022"));
        assertEquals("-", MCRCalendar.delimiter("-23-03-2022"));

        assertEquals(".", MCRCalendar.delimiter("800"));
    }

    @Test
    public void testBeforeZero() {
        assertFalse(MCRCalendar.beforeZero("23.03.2022", MCRCalendar.CalendarType.GREGORIAN));
        assertFalse(MCRCalendar.beforeZero("23/03/2022", MCRCalendar.CalendarType.GREGORIAN));
        assertFalse(MCRCalendar.beforeZero("23-03-2022", MCRCalendar.CalendarType.GREGORIAN));
        assertTrue(MCRCalendar.beforeZero("-23.03.2022", MCRCalendar.CalendarType.GREGORIAN));
        assertTrue(MCRCalendar.beforeZero("-23/03/2022", MCRCalendar.CalendarType.GREGORIAN));
        assertTrue(MCRCalendar.beforeZero("-23-03-2022", MCRCalendar.CalendarType.GREGORIAN));

        assertFalse(MCRCalendar.beforeZero("23-03-2022 AD", MCRCalendar.CalendarType.GREGORIAN));
        assertFalse(MCRCalendar.beforeZero("AD 23-03-2022", MCRCalendar.CalendarType.GREGORIAN));
        assertTrue(MCRCalendar.beforeZero("23-03-2022 BC", MCRCalendar.CalendarType.GREGORIAN));
        assertTrue(MCRCalendar.beforeZero("BC 23-03-2022", MCRCalendar.CalendarType.GREGORIAN));
    }

    @Test
    public void testGetLastDayOfMonth() {
        assertEquals(31,
            MCRCalendar.getLastDayOfMonth(GregorianCalendar.JANUARY, 2000, MCRCalendar.CalendarType.GREGORIAN));
        assertEquals(29,
            MCRCalendar.getLastDayOfMonth(GregorianCalendar.FEBRUARY, 2000, MCRCalendar.CalendarType.GREGORIAN));
        assertEquals(28,
            MCRCalendar.getLastDayOfMonth(GregorianCalendar.FEBRUARY, 2001, MCRCalendar.CalendarType.GREGORIAN));
        assertEquals(31,
            MCRCalendar.getLastDayOfMonth(GregorianCalendar.MARCH, 2000, MCRCalendar.CalendarType.GREGORIAN));
        assertEquals(30,
            MCRCalendar.getLastDayOfMonth(GregorianCalendar.APRIL, 2000, MCRCalendar.CalendarType.GREGORIAN));

        assertEquals(28,
            MCRCalendar.getLastDayOfMonth(GregorianCalendar.FEBRUARY, 1700, MCRCalendar.CalendarType.GREGORIAN));
        assertEquals(29,
            MCRCalendar.getLastDayOfMonth(GregorianCalendar.FEBRUARY, 1700, MCRCalendar.CalendarType.JULIAN));

        assertEquals(5, MCRCalendar.getLastDayOfMonth(CopticCalendar.NASIE, 2000, MCRCalendar.CalendarType.COPTIC));

        assertEquals(30, MCRCalendar.getLastDayOfMonth(11, 2000, MCRCalendar.CalendarType.EGYPTIAN));
        assertEquals(5, MCRCalendar.getLastDayOfMonth(12, 2000, MCRCalendar.CalendarType.EGYPTIAN));

        assertEquals(30, MCRCalendar.getLastDayOfMonth(11, 2000, MCRCalendar.CalendarType.ARMENIAN));
        assertEquals(5, MCRCalendar.getLastDayOfMonth(12, 2000, MCRCalendar.CalendarType.ARMENIAN));
    }

    @Test
    public void testIsLeapYear() {
        assertTrue(MCRCalendar.isLeapYear(2000, MCRCalendar.CalendarType.GREGORIAN));
        assertFalse(MCRCalendar.isLeapYear(1999, MCRCalendar.CalendarType.GREGORIAN));
        assertFalse(MCRCalendar.isLeapYear(1582, MCRCalendar.CalendarType.GREGORIAN));
        assertFalse(MCRCalendar.isLeapYear(1900, MCRCalendar.CalendarType.GREGORIAN));
        assertTrue(MCRCalendar.isLeapYear(1900, MCRCalendar.CalendarType.JULIAN));
    }

    @Test
    public void testGetCalendarTypeString() {
        assertEquals(MCRCalendar.TAG_GREGORIAN, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ARMENIAN)));
        assertEquals(MCRCalendar.TAG_BUDDHIST, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_BUDDHIST)));
        assertEquals(MCRCalendar.TAG_COPTIC, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_COPTIC)));
        assertEquals(MCRCalendar.TAG_ETHIOPIC, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ETHIOPIC)));
        assertEquals(MCRCalendar.TAG_GREGORIAN, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_GREGORIAN)));
        assertEquals(MCRCalendar.TAG_GREGORIAN, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_JULIAN)));
        assertEquals(MCRCalendar.TAG_HEBREW, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_HEBREW)));
        assertEquals(MCRCalendar.TAG_ISLAMIC, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ISLAMIC)));
        assertEquals(MCRCalendar.TAG_JAPANESE, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1H1", false, MCRCalendar.TAG_JAPANESE)));
        assertEquals(MCRCalendar.TAG_GREGORIAN, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_PERSIC)));
        assertEquals(MCRCalendar.TAG_GREGORIAN, MCRCalendar.getCalendarTypeString(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_EGYPTIAN)));
    }

    @Test
    public void testGetGregorianCalendarOfACalendar() {
        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ARMENIAN)),
            MCRCalendar.getHistoryDateAsCalendar("13.7.552", false, MCRCalendar.TAG_GREGORIAN));

        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_BUDDHIST)),
            MCRCalendar.getHistoryDateAsCalendar("1.1.543 BC", false, MCRCalendar.TAG_GREGORIAN));

        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_COPTIC)),
            MCRCalendar.getHistoryDateAsCalendar("29.8.284", false, MCRCalendar.TAG_GREGORIAN));

        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ETHIOPIC)),
            MCRCalendar.getHistoryDateAsCalendar("29.8.8", false, MCRCalendar.TAG_GREGORIAN));

        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_GREGORIAN)),
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_GREGORIAN));

        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_JULIAN)),
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_GREGORIAN));

        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_HEBREW)),
            MCRCalendar.getHistoryDateAsCalendar("-7.10.3761", false, MCRCalendar.TAG_GREGORIAN));

        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ISLAMIC)),
            MCRCalendar.getHistoryDateAsCalendar("16.7.622", false, MCRCalendar.TAG_GREGORIAN));

        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.H1", false, MCRCalendar.TAG_JAPANESE)),
            MCRCalendar.getHistoryDateAsCalendar("1.1.1989", false, MCRCalendar.TAG_GREGORIAN));

        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_PERSIC)),
            MCRCalendar.getHistoryDateAsCalendar("21.3.622", false, MCRCalendar.TAG_GREGORIAN));

        compareCalendarDates(MCRCalendar.getGregorianCalendarOfACalendar(
            MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_EGYPTIAN)),
            MCRCalendar.getHistoryDateAsCalendar("18.2.747 BC", false, MCRCalendar.TAG_GREGORIAN));
    }

    private void compareCalendarDates(Calendar actual, Calendar expected) {
        assertEquals(actual.get(Calendar.YEAR), expected.get(Calendar.YEAR));
        assertEquals(actual.get(Calendar.MONTH), expected.get(Calendar.MONTH));
        assertEquals(actual.get(Calendar.DAY_OF_MONTH), expected.get(Calendar.DAY_OF_MONTH));
        assertEquals(actual.get(Calendar.ERA), expected.get(Calendar.ERA));
    }

    private void assertCalEqualWithActual(Calendar cal, int actualDay, int actualMonth, int actualYear) {
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), actualDay);
        assertEquals(cal.get(Calendar.MONTH), actualMonth);
        assertEquals(cal.get(Calendar.YEAR), actualYear);
    }

    private void assertCalEqualWithActual(Calendar cal, int actualDay, int actualMonth, int actualYear,
        int actualEra) {
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), actualDay);
        assertEquals(cal.get(Calendar.MONTH), actualMonth);
        assertEquals(cal.get(Calendar.YEAR), actualYear);
        assertEquals(cal.get(Calendar.ERA), actualEra);
    }

    private void assertGregEqualWithCal(Calendar greg, Calendar cal) {
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));
    }

    private void assertHistoryDates(String firstDate, int expectedDay, int expectedMonth, int expectedYear,
        String secondDate, boolean isLast) {
        Calendar cal = MCRCalendar.getHistoryDateAsCalendar(firstDate, isLast, MCRCalendar.TAG_ARMENIAN);
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar(secondDate, false, MCRCalendar.TAG_GREGORIAN);
        assertCalEqualWithActual(cal, expectedDay, expectedMonth, expectedYear);
        assertGregEqualWithCal(greg, cal);
    }
}

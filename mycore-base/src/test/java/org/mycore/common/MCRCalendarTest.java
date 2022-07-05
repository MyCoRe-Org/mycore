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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
 * @version $Revision: 1.3 $ $Date: 2008/06/02 10:10:05 $
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

    @Test
    public void testParseGregorianDate() {
        Calendar cal;

        // 04.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 4);
        assertEquals(cal.get(Calendar.MONTH), Calendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2299160);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2299160");

        // 05.10.1582 AD (gregorian) -> 15.10.1582 (https://en.wikipedia.org/wiki/Inter_gravissimas)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(cal.get(Calendar.MONTH), Calendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2299161);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2299161");

        // 06.10.1582 AD (gregorian) -> 16.10.1582 in the ICU implementation
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 16);
        assertEquals(cal.get(Calendar.MONTH), Calendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2299162);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2299162");

        // 15.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(cal.get(Calendar.MONTH), Calendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2299161);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2299161");

        // 16.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 16);
        assertEquals(cal.get(Calendar.MONTH), Calendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2299162);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2299162");

        // 01.01.1800 AD (gregorian) with missing day and last=false
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1800", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), Calendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 1800);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2378497);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2378497");

        // 31.01.1800 AD (gregorian) with missing day and last=true
        cal = MCRCalendar.getHistoryDateAsCalendar("1/1800", true, MCRCalendar.TAG_GREGORIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 31);
        assertEquals(cal.get(Calendar.MONTH), Calendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 1800);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2378527);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2378527");

        // 09.01.1800 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("9/1/1800", true, MCRCalendar.TAG_GREGORIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 9);
        assertEquals(cal.get(Calendar.MONTH), Calendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 1800);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2378505);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2378505");

        // 24.02.1964 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24", true, MCRCalendar.TAG_GREGORIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 24);
        assertEquals(cal.get(Calendar.MONTH), Calendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 1964);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2438450);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2438450");

        // 1 BC with last=true (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 BC", true, MCRCalendar.TAG_GREGORIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 31);
        assertEquals(cal.get(Calendar.MONTH), Calendar.DECEMBER);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1721423);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1721423");
    }

    @Test
    public void testParseJulianDate() {
        Calendar cal;

        // 02.01.4713 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("02.01.4713 bc", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 2);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 4713);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1");

        // 01.01.0814 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("-814", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 814);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1424110);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1424110");

        // 01.01.0814 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("-01.01.814", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 814);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1424110);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1424110");

        // 15.03.0044 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("BC 15.03.44", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 44);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1705426);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1705426");

        // 01.01.0001 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0001 BC", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1721058);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1721058");

        // 31.12.0001 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("31.12.0001 v. Chr", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 31);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.DECEMBER);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1721423);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1721423");

        // 01.01.0000 -> 1.1.1 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0000", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1721058);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1721058");

        // 01.01.0001 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.01 AD", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1721424);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1721424");

        // 04.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.1582 N. Chr", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 4);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2299160);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2299160");

        // 05.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 5);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2299161);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2299161");

        // 06.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 6);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2299162);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2299162");

        // 15.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2299171);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2299171");

        // 16.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 16);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2299172);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2299172");

        // 28.02.1700 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("28.02.1700", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 28);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 1700);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2342041);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2342041");

        // 29.02.1700 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("29.02.1700", false, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 29);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 1700);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.AD);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 2342042);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "2342042");

        // 1 BC with last=true (jul)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 BC", true, MCRCalendar.TAG_JULIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 31);
        assertEquals(cal.get(Calendar.MONTH), Calendar.DECEMBER);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1721423);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1721423");
    }

    @Test
    public void testParseIslamicDate() {
        Calendar cal;

        // 01.01.0001 h. (islamic)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0001 h.", false, MCRCalendar.TAG_ISLAMIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), IslamicCalendar.MUHARRAM);
        assertEquals(cal.get(Calendar.YEAR), 1);

        // first day of Islamic calendar is 16.7.622 in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("16.7.622", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(greg), MCRCalendar.getJulianDayNumber(cal));
        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1948440);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1948440");

        // 01.01.800 H. (islamic) -> 24.09.1397 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", false, MCRCalendar.TAG_ISLAMIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), IslamicCalendar.MUHARRAM);
        assertEquals(cal.get(Calendar.YEAR), 800);

        greg = MCRCalendar.getHistoryDateAsCalendar("24.09.1397", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 30.01.800 H. (islamic) -> 23.10.1397 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", true, MCRCalendar.TAG_ISLAMIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 30);
        assertEquals(cal.get(Calendar.MONTH), IslamicCalendar.MUHARRAM);
        assertEquals(cal.get(Calendar.YEAR), 800);

        greg = MCRCalendar.getHistoryDateAsCalendar("23.10.1397", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 29.12.800 H. (islamic) -> 12.09.1398 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("800", true, MCRCalendar.TAG_ISLAMIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 29);
        assertEquals(cal.get(Calendar.MONTH), IslamicCalendar.DHU_AL_HIJJAH);
        assertEquals(cal.get(Calendar.YEAR), 800);

        greg = MCRCalendar.getHistoryDateAsCalendar("12.09.1398", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // -1 (isl) -> 15.07.622 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_ISLAMIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 29);
        assertEquals(cal.get(Calendar.MONTH), IslamicCalendar.DHU_AL_HIJJAH);
        assertEquals(cal.get(Calendar.YEAR), 0);

        greg = MCRCalendar.getHistoryDateAsCalendar("15.7.622", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

    }

    @Test
    public void testParseCopticDate() {
        Calendar cal;

        // 01.01.0001 A.M. (coptic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1 a.M.", false, MCRCalendar.TAG_COPTIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), CopticCalendar.TOUT);
        assertEquals(cal.get(Calendar.YEAR), 1);

        // first day of Coptic calendar is 29.8.284 in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("29.8.284", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(greg), MCRCalendar.getJulianDayNumber(cal));
        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1825030);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1825030");

        // 01.01.1724 A.M. (coptic) -> 12.09.2007
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1724 A.M.", false, MCRCalendar.TAG_COPTIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), CopticCalendar.TOUT);
        assertEquals(cal.get(Calendar.YEAR), 1724);

        greg = MCRCalendar.getHistoryDateAsCalendar("12.09.2007", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 05.13.1724 E.E. (coptic) -> 10.09.2008
        cal = MCRCalendar.getHistoryDateAsCalendar("1724 a.M.", true, MCRCalendar.TAG_COPTIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 5);
        assertEquals(cal.get(Calendar.MONTH), CopticCalendar.NASIE);
        assertEquals(cal.get(Calendar.YEAR), 1724);

        greg = MCRCalendar.getHistoryDateAsCalendar("10.09.2008", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // -5.13.1 (cop) -> 28.8.284
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_COPTIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 5);
        assertEquals(cal.get(Calendar.MONTH), CopticCalendar.NASIE);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        greg = MCRCalendar.getHistoryDateAsCalendar("28.8.284", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));
    }

    @Test
    public void testParseEthiopianDate() {
        Calendar cal;

        // 01.01.0001 E.E. (ethiopic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 E.E.", false, MCRCalendar.TAG_ETHIOPIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), EthiopicCalendar.MESKEREM);
        assertEquals(cal.get(Calendar.YEAR), 1);

        // first day of Ehtiopian calendar is 29.8.8 in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("29.8.8", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(greg), MCRCalendar.getJulianDayNumber(cal));
        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1724221);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1724221");

        // 05.13.2000 E.E. (ethiopic) -> 10.09.2008 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("2000 E.E.", true, MCRCalendar.TAG_ETHIOPIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 5);
        assertEquals(cal.get(Calendar.MONTH), EthiopicCalendar.PAGUMEN);
        assertEquals(cal.get(Calendar.YEAR), 2000);
        assertEquals(cal.get(Calendar.ERA), 1);

        greg = MCRCalendar.getHistoryDateAsCalendar("10.09.2008", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // years before 0 are represented in Amete Alem format (starting with count from 5500 BC)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_ETHIOPIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 5);
        assertEquals(cal.get(Calendar.MONTH), EthiopicCalendar.PAGUMEN);
        assertEquals(cal.get(Calendar.YEAR), 5500);
        assertEquals(cal.get(Calendar.ERA), 0);

        greg = MCRCalendar.getHistoryDateAsCalendar("28.8.8", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));
    }

    @Test
    public void testParseHebrewDate() {
        Calendar cal;

        // 1.1.1 (hebrew)
        cal = MCRCalendar.getHistoryDateAsCalendar("1", false, MCRCalendar.TAG_HEBREW);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), HebrewCalendar.TISHRI);
        assertEquals(cal.get(Calendar.YEAR), 1);

        // first day of Hebrew calendar is 7.10.3761 BC in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("7.10.3761 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumber(cal), 347998);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "347998");

        // 04.10.1582 (hebrew) - 29.04.2178 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_HEBREW);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 4);
        assertEquals(cal.get(Calendar.MONTH), HebrewCalendar.SIVAN);
        assertEquals(cal.get(Calendar.YEAR), 1582);

        greg = MCRCalendar.getHistoryDateAsCalendar("17.05.2179 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 18.01.5343 (hebrew) -> 04.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("18.01.5343", false, MCRCalendar.TAG_HEBREW);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 18);
        assertEquals(cal.get(Calendar.MONTH), HebrewCalendar.TISHRI);
        assertEquals(cal.get(Calendar.YEAR), 5343);

        greg = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 19.01.5343 (hebrew) -> 15.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("19.01.5343", false, MCRCalendar.TAG_HEBREW);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 19);
        assertEquals(cal.get(Calendar.MONTH), HebrewCalendar.TISHRI);
        assertEquals(cal.get(Calendar.YEAR), 5343);

        greg = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 19.01.5343 (hebrew) -> 16.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("20.01.5343", false, MCRCalendar.TAG_HEBREW);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 20);
        assertEquals(cal.get(Calendar.MONTH), HebrewCalendar.TISHRI);
        assertEquals(cal.get(Calendar.YEAR), 5343);

        greg = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 01.01.1800 (hebrew) with missing day and last=false
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1800", false, MCRCalendar.TAG_HEBREW);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), HebrewCalendar.TISHRI);
        assertEquals(cal.get(Calendar.YEAR), 1800);

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
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 1);

        // first day of Buddhist calendar is 1.1.543 BC in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("1.1.543 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1523093);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1523093");

        // year 0
        cal = MCRCalendar.getHistoryDateAsCalendar("0", false, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 0);

        greg = MCRCalendar.getHistoryDateAsCalendar("1.1.544 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // year -1
        cal = MCRCalendar.getHistoryDateAsCalendar("-1.1.1", false, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 0);

        greg = MCRCalendar.getHistoryDateAsCalendar("1.1.544 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // year -100
        cal = MCRCalendar.getHistoryDateAsCalendar("-100", false, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), -99);

        greg = MCRCalendar.getHistoryDateAsCalendar("1.1.643 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 04.10.2125 (buddhist) -> year 1582 in gregorian calendar
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 4);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 2125);

        greg = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 05.10.2125 (buddhist) -> 15.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 2125);

        greg = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 06.10.2125 (buddhist) -> 15.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 2125);

        greg = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 15.10.2125 (buddhist) -> 15.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 2125);

        greg = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 16.10.2125 (buddhist) -> 15.10.1582 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 16);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 2125);

        greg = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 01.01.1800 (buddhist) with missing day and last=false -> 01.01.257 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1800", false, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 1800);

        greg = MCRCalendar.getHistoryDateAsCalendar("01.01.1257", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 24.02.1964 (buddhist) -> 24.04142 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24", true, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 24);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 1964);

        greg = MCRCalendar.getHistoryDateAsCalendar("24.02.1421", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 24.02.1964 BE (buddhist) -> 24.02.2507 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24 B.E.", true, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 24);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), -1963);

        greg = MCRCalendar.getHistoryDateAsCalendar("24.02.2507 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // -1 (buddhist) -> 24.02.2507 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_BUDDHIST);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 31);
        assertEquals(cal.get(Calendar.MONTH), BuddhistCalendar.DECEMBER);
        assertEquals(cal.get(Calendar.YEAR), 0);

        greg = MCRCalendar.getHistoryDateAsCalendar("31.12.544 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));
    }

    @Test
    public void testParsePersianDate() {
        Calendar cal;

        // 01.01.0001  (persian) -> 22.3.622 greg
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_PERSIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 21);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 622);

        // first day of Persian calendar is 21.3.622 in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("21.3.622", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1948323);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1948323");

        // 01.01.800 (persian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800", false, MCRCalendar.TAG_PERSIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 21);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 1421);

        greg = MCRCalendar.getHistoryDateAsCalendar("21.03.1421", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 31.01.800 (persian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800", true, MCRCalendar.TAG_PERSIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 20);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.APRIL);
        assertEquals(cal.get(Calendar.YEAR), 1421);

        greg = MCRCalendar.getHistoryDateAsCalendar("20.04.1421", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 29.12.800 (persian)
        cal = MCRCalendar.getHistoryDateAsCalendar("800", true, MCRCalendar.TAG_PERSIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 20);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 1422);

        greg = MCRCalendar.getHistoryDateAsCalendar("20.03.1422", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // gregorian calendar reform on October, 5th 1582 -> skip days between 5 and 15
        cal = MCRCalendar.getHistoryDateAsCalendar("12.7.961", false, MCRCalendar.TAG_PERSIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 4);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);

        greg = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        cal = MCRCalendar.getHistoryDateAsCalendar("13.7.961", false, MCRCalendar.TAG_PERSIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);

        greg = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        cal = MCRCalendar.getHistoryDateAsCalendar("14.7.961", false, MCRCalendar.TAG_PERSIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 16);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);

        greg = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // -1.1.1 (pers) -> 22.03.621 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", false, MCRCalendar.TAG_PERSIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 21);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 621);

        greg = MCRCalendar.getHistoryDateAsCalendar("21.03.621", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // -29.12.1 (pers) -> 21.03.621 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_PERSIC);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 21);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 622);

        greg = MCRCalendar.getHistoryDateAsCalendar("21.03.622", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));
    }

    @Test
    public void testParseArmenianDate() {
        Calendar cal;

        // 01.01.0001  (armenian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 13);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JULY);
        assertEquals(cal.get(Calendar.YEAR), 552);

        // first day of Armenian calendar is 13.7.552 in Gregorian/11.7.552 in Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("13.7.552", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1922870);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1922870");

        cal = MCRCalendar.getHistoryDateAsCalendar("1.2.1", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 12);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.AUGUST);
        assertEquals(cal.get(Calendar.YEAR), 552);

        greg = MCRCalendar.getHistoryDateAsCalendar("12.08.552", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        cal = MCRCalendar.getHistoryDateAsCalendar("5.13.1", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 12);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JULY);
        assertEquals(cal.get(Calendar.YEAR), 553);

        greg = MCRCalendar.getHistoryDateAsCalendar("12.07.553", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        cal = MCRCalendar.getHistoryDateAsCalendar("2.9.48", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 28);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 600);

        greg = MCRCalendar.getHistoryDateAsCalendar("28.02.600", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        cal = MCRCalendar.getHistoryDateAsCalendar("3.9.48", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 29);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 600);

        greg = MCRCalendar.getHistoryDateAsCalendar("29.02.600", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        cal = MCRCalendar.getHistoryDateAsCalendar("4.9.48", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 600);

        greg = MCRCalendar.getHistoryDateAsCalendar("01.03.600", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1462", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 26);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JULY);
        assertEquals(cal.get(Calendar.YEAR), 2012);

        greg = MCRCalendar.getHistoryDateAsCalendar("26.07.2012", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.101", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 18);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JUNE);
        assertEquals(cal.get(Calendar.YEAR), 652);

        greg = MCRCalendar.getHistoryDateAsCalendar("18.06.652", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1031", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 29);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1581);

        greg = MCRCalendar.getHistoryDateAsCalendar("29.10.1581", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // checks for gregorian calendar switch in 1582
        // 11.12.1031 arm -> 4.10.1582 greg
        cal = MCRCalendar.getHistoryDateAsCalendar("11.12.1031", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 4);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);

        greg = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 12.12.1031 arm -> 15.10.1582 greg
        cal = MCRCalendar.getHistoryDateAsCalendar("12.12.1031", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);

        greg = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 13.12.1031 arm -> 16.10.1582 greg
        cal = MCRCalendar.getHistoryDateAsCalendar("13.12.1031", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 16);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.OCTOBER);
        assertEquals(cal.get(Calendar.YEAR), 1582);

        greg = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 12.500 (last=false) -> 1.12.500 -> 8.2.1052 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("12.500", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 4);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 1052);

        greg = MCRCalendar.getHistoryDateAsCalendar("04.02.1052", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 12.500 (last=true) -> 30.12.500 -> 8.3.1052 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("12.500", true, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 4);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 1052);

        greg = MCRCalendar.getHistoryDateAsCalendar("04.03.1052", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 500 (last=false) -> 1.1.500 -> 15.3.1051 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("500", false, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 11);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 1051);

        greg = MCRCalendar.getHistoryDateAsCalendar("11.03.1051", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 500 (last=true) -> 5.13.500 -> 13.3.1052 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("500", true, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 9);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 1052);

        greg = MCRCalendar.getHistoryDateAsCalendar("09.03.1052", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // -1 (last=true) -> 5.13.-1 -> 12.07.552 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_ARMENIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 12);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.JULY);
        assertEquals(cal.get(Calendar.YEAR), 552);

        greg = MCRCalendar.getHistoryDateAsCalendar("12.07.552", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));
    }

    @Test
    public void testParseEgyptianDate() {
        Calendar cal;

        // 01.01.0001  (egyptian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_EGYPTIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 18);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 747);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        // first day of Egyptian calendar is 18.2.747 BC in Gregorian/Julian calendar
        Calendar greg = MCRCalendar.getHistoryDateAsCalendar("18.2.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumber(cal), 1448630);
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), "1448630");

        // 1.1 -> 30.1.1 in Gregorian date: 19.3.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1 A.N.", true, MCRCalendar.TAG_EGYPTIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 19);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 747);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        greg = MCRCalendar.getHistoryDateAsCalendar("19.03.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 10.1 (last=false) -> 1.10.1 in Gregorian date: 14.12.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("10.1 A.N.", false, MCRCalendar.TAG_EGYPTIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 15);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.NOVEMBER);
        assertEquals(cal.get(Calendar.YEAR), 747);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        greg = MCRCalendar.getHistoryDateAsCalendar("15.11.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 10.1 (last=true) -> 30.10.1 in Gregorian date: 14.12.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("10.1 A.N.", true, MCRCalendar.TAG_EGYPTIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 14);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.DECEMBER);
        assertEquals(cal.get(Calendar.YEAR), 747);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        greg = MCRCalendar.getHistoryDateAsCalendar("14.12.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 1.2.1 -> in Gregorian date: 20.3.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("1.2.1 A.N.", false, MCRCalendar.TAG_EGYPTIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 20);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.MARCH);
        assertEquals(cal.get(Calendar.YEAR), 747);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        greg = MCRCalendar.getHistoryDateAsCalendar("20.03.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 13.1 (last=false) -> 1.13.1 (in Gregorian date: 17.2.746)
        cal = MCRCalendar.getHistoryDateAsCalendar("13.1 A.N.", false, MCRCalendar.TAG_EGYPTIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 13);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 746);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        greg = MCRCalendar.getHistoryDateAsCalendar("13.02.746 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // 1 (last=true) -> 5.13.1 (in Gregorian date: 17.2.746)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 A.N.", true, MCRCalendar.TAG_EGYPTIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 17);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 746);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        greg = MCRCalendar.getHistoryDateAsCalendar("17.02.746 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));

        // -1 (last=true) -> 17.02.747 BC (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("-1", true, MCRCalendar.TAG_EGYPTIAN);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 17);
        assertEquals(cal.get(Calendar.MONTH), GregorianCalendar.FEBRUARY);
        assertEquals(cal.get(Calendar.YEAR), 747);
        assertEquals(cal.get(Calendar.ERA), GregorianCalendar.BC);

        greg = MCRCalendar.getHistoryDateAsCalendar("17.02.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getJulianDayNumber(cal), MCRCalendar.getJulianDayNumber(greg));
        assertEquals(MCRCalendar.getJulianDayNumberAsString(cal), MCRCalendar.getJulianDayNumberAsString(greg));
    }

    @Test
    public void testParseJapaneseDate() {
        Calendar cal;

        // Meiji era: 8.9.1868 - 29.07.1912
        cal = MCRCalendar.getHistoryDateAsCalendar("8.9.M1", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 8);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.SEPTEMBER);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.MEIJI);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("8.9.1868", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("29.7.M45", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 29);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.JULY);
        assertEquals(cal.get(Calendar.YEAR), 45);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.MEIJI);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("29.7.1912", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("30.7.M45", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 30);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.JULY);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.TAISHO);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("30.7.1912", false, MCRCalendar.TAG_GREGORIAN)));

        // Taisho era: 30.7.1912-24.12.1926
        cal = MCRCalendar.getHistoryDateAsCalendar("30.7.T1", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 30);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.JULY);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.TAISHO);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("30.7.1912", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("24.12.T15", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 24);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.DECEMBER);
        assertEquals(cal.get(Calendar.YEAR), 15);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.TAISHO);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("24.12.1926", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("25.12.T15", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 25);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.DECEMBER);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.SHOWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("25.12.1926", false, MCRCalendar.TAG_GREGORIAN)));

        // Showa era: 25.12.1926-07.01.1989
        cal = MCRCalendar.getHistoryDateAsCalendar("25.12.S1", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 25);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.DECEMBER);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.SHOWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("25.12.1926", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("7.1.S64", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 7);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 64);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.SHOWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("7.1.1989", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("8.1.S64", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 8);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.HEISEI);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("8.1.1989", false, MCRCalendar.TAG_GREGORIAN)));

        // Heisei era: 08.01.1989-30.04.2019
        cal = MCRCalendar.getHistoryDateAsCalendar("8.1.H1", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 8);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.JANUARY);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.HEISEI);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("8.1.1989", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("30.4.H31", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 30);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.APRIL);
        assertEquals(cal.get(Calendar.YEAR), 31);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.HEISEI);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("30.4.2019", false, MCRCalendar.TAG_GREGORIAN)));

        cal = MCRCalendar.getHistoryDateAsCalendar("1.5.H31", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.MAY);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.REIWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("1.5.2019", false, MCRCalendar.TAG_GREGORIAN)));

        // Reiwa era: 01.05.2019 - present
        cal = MCRCalendar.getHistoryDateAsCalendar("1.5.R1", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.MAY);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.REIWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("1.5.2019", false, MCRCalendar.TAG_GREGORIAN)));

        // check ISO format
        cal = MCRCalendar.getHistoryDateAsCalendar("R1-5-1", true, MCRCalendar.TAG_JAPANESE);
        assertEquals(cal.get(Calendar.DAY_OF_MONTH), 1);
        assertEquals(cal.get(Calendar.MONTH), JapaneseCalendar.MAY);
        assertEquals(cal.get(Calendar.YEAR), 1);
        assertEquals(cal.get(Calendar.ERA), JapaneseCalendar.REIWA);

        assertEquals(MCRCalendar.getJulianDayNumber(cal),
                MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("1.5.2019", false, MCRCalendar.TAG_GREGORIAN)));
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
            cal = MCRCalendar.getHistoryDateAsCalendar(null, false, MCRCalendar.CalendarType.Islamic);
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
        // 29.02.1700 (julian)
        calendar = MCRCalendar.getHistoryDateAsCalendar("29.02.1700", true, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy G");
        assertEquals("is not julian date 29.02.1700 AD", "29.02.1700 AD", dstring);
        // 29.02.1700 (gregorian) -> no leap year in gregorian calendar
        assertThrows(MCRException.class,
            () -> MCRCalendar.getHistoryDateAsCalendar("29.02.1700", true, MCRCalendar.TAG_GREGORIAN));
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
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy");
        assertEquals("is not coptic date 01.01.1724 A.M.", "01.01.1724 A.M.", dstring);

        // 01.01.2000 A.M. (ethiopic)
        calendar = MCRCalendar.getHistoryDateAsCalendar("2000", true, MCRCalendar.TAG_ETHIOPIC);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy");
        assertEquals("is not ethiopic date 05.13.2000 E.E.", "05.13.2000 E.E.", dstring);

        calendar = MCRCalendar.getHistoryDateAsCalendar("1.1.500", false, MCRCalendar.TAG_ARMENIAN);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yyyy");
        assertEquals("is not armenian date 11.03.1051", "11.03.1051", dstring);

        calendar = MCRCalendar.getHistoryDateAsCalendar("5.7.H2", false, MCRCalendar.TAG_JAPANESE);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.Y");
        assertEquals("is not japanese date 05.07.1990", "05.07.1990", dstring);

        calendar = MCRCalendar.getHistoryDateAsCalendar("2.7.20", false, MCRCalendar.TAG_BUDDHIST);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.yy");
        assertEquals("is not japanese date 02.07.20", "02.07.20", dstring);

        calendar = MCRCalendar.getHistoryDateAsCalendar("2.7.20", false, MCRCalendar.TAG_BUDDHIST);
        dstring = MCRCalendar.getCalendarDateToFormattedString(calendar, "dd.MM.Y");
        assertEquals("is not buddhist date 02.07.-523", "02.07.-523", dstring);
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
        assertEquals(MCRCalendar.delimiter("23.03.2022"), ".");
        assertEquals(MCRCalendar.delimiter("23/03/2022"), "/");
        assertEquals(MCRCalendar.delimiter("23-03-2022"), "-");
        assertEquals(MCRCalendar.delimiter("-23.03.2022"), ".");
        assertEquals(MCRCalendar.delimiter("-23/03/2022"), "/");
        assertEquals(MCRCalendar.delimiter("-23-03-2022"), "-");

        assertEquals(MCRCalendar.delimiter("800"), ".");
    }

    @Test
    public void testBeforeZero() {
        assertFalse(MCRCalendar.beforeZero("23.03.2022", MCRCalendar.CalendarType.Gregorian));
        assertFalse(MCRCalendar.beforeZero("23/03/2022", MCRCalendar.CalendarType.Gregorian));
        assertFalse(MCRCalendar.beforeZero("23-03-2022", MCRCalendar.CalendarType.Gregorian));
        assertTrue(MCRCalendar.beforeZero("-23.03.2022", MCRCalendar.CalendarType.Gregorian));
        assertTrue(MCRCalendar.beforeZero("-23/03/2022", MCRCalendar.CalendarType.Gregorian));
        assertTrue(MCRCalendar.beforeZero("-23-03-2022", MCRCalendar.CalendarType.Gregorian));

        assertFalse(MCRCalendar.beforeZero("23-03-2022 AD", MCRCalendar.CalendarType.Gregorian));
        assertFalse(MCRCalendar.beforeZero("AD 23-03-2022", MCRCalendar.CalendarType.Gregorian));
        assertTrue(MCRCalendar.beforeZero("23-03-2022 BC", MCRCalendar.CalendarType.Gregorian));
        assertTrue(MCRCalendar.beforeZero("BC 23-03-2022", MCRCalendar.CalendarType.Gregorian));
    }

    @Test
    public void testGetLastDayOfMonth() {
        assertEquals(MCRCalendar.getLastDayOfMonth(GregorianCalendar.JANUARY, 2000, MCRCalendar.CalendarType.Gregorian),
                31);
        assertEquals(
                MCRCalendar.getLastDayOfMonth(GregorianCalendar.FEBRUARY, 2000, MCRCalendar.CalendarType.Gregorian),
                29);
        assertEquals(
                MCRCalendar.getLastDayOfMonth(GregorianCalendar.FEBRUARY, 2001, MCRCalendar.CalendarType.Gregorian),
                28);
        assertEquals(MCRCalendar.getLastDayOfMonth(GregorianCalendar.MARCH, 2000, MCRCalendar.CalendarType.Gregorian),
                31);
        assertEquals(MCRCalendar.getLastDayOfMonth(GregorianCalendar.APRIL, 2000, MCRCalendar.CalendarType.Gregorian),
                30);

        assertEquals(
                MCRCalendar.getLastDayOfMonth(GregorianCalendar.FEBRUARY, 1700, MCRCalendar.CalendarType.Gregorian),
                28);
        assertEquals(MCRCalendar.getLastDayOfMonth(GregorianCalendar.FEBRUARY, 1700, MCRCalendar.CalendarType.Julian),
                29);

        assertEquals(MCRCalendar.getLastDayOfMonth(CopticCalendar.NASIE, 2000, MCRCalendar.CalendarType.Coptic),
                5);

        assertEquals(MCRCalendar.getLastDayOfMonth(11, 2000, MCRCalendar.CalendarType.Egyptian),
                30);
        assertEquals(MCRCalendar.getLastDayOfMonth(12, 2000, MCRCalendar.CalendarType.Egyptian),
                5);

        assertEquals(MCRCalendar.getLastDayOfMonth(11, 2000, MCRCalendar.CalendarType.Armenian),
                30);
        assertEquals(MCRCalendar.getLastDayOfMonth(12, 2000, MCRCalendar.CalendarType.Armenian),
                5);
    }

    @Test
    public void testIsLeapYear() {
        assertTrue(MCRCalendar.isLeapYear(2000, MCRCalendar.CalendarType.Gregorian));
        assertFalse(MCRCalendar.isLeapYear(1999, MCRCalendar.CalendarType.Gregorian));
        assertFalse(MCRCalendar.isLeapYear(1582, MCRCalendar.CalendarType.Gregorian));
        assertFalse(MCRCalendar.isLeapYear(1900, MCRCalendar.CalendarType.Gregorian));
        assertTrue(MCRCalendar.isLeapYear(1900, MCRCalendar.CalendarType.Julian));
    }

    @Test
    public void testGetCalendarTypeString() {
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ARMENIAN)),
                MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_BUDDHIST)),
                MCRCalendar.TAG_BUDDHIST);
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_COPTIC)),
                MCRCalendar.TAG_COPTIC);
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ETHIOPIC)),
                MCRCalendar.TAG_ETHIOPIC);
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_GREGORIAN)),
                MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_JULIAN)),
                MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_HEBREW)),
                MCRCalendar.TAG_HEBREW);
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ISLAMIC)),
                MCRCalendar.TAG_ISLAMIC);
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1H1", false, MCRCalendar.TAG_JAPANESE)),
                MCRCalendar.TAG_JAPANESE);
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_PERSIC)),
                MCRCalendar.TAG_GREGORIAN);
        assertEquals(MCRCalendar.getCalendarTypeString(
                        MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_EGYPTIAN)),
                MCRCalendar.TAG_GREGORIAN);
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
}

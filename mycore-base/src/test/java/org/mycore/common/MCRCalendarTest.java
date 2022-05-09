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

import com.ibm.icu.util.BuddhistCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.CopticCalendar;
import com.ibm.icu.util.EthiopicCalendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.JapaneseCalendar;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(4));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299160));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299160"));

        // 05.10.1582 AD (gregorian) -> 15.10.1582 (https://en.wikipedia.org/wiki/Inter_gravissimas)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299161));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299161"));

        // 06.10.1582 AD (gregorian) -> 16.10.1582 in the ICU implementation
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(16));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299162));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299162"));

        // 15.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299161));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299161"));

        // 16.10.1582 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(16));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299162));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299162"));

        // 01.01.1800 AD (gregorian) with missing day and last=false
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1800", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(1800));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        // 31.01.1800 AD (gregorian) with missing day and last=true
        cal = MCRCalendar.getHistoryDateAsCalendar("1/1800", true, MCRCalendar.TAG_GREGORIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(31));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(1800));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2378527));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2378527"));

        // 09.01.1800 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("9/1/1800", true, MCRCalendar.TAG_GREGORIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(9));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(1800));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        // 24.02.1964 AD (gregorian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24", true, MCRCalendar.TAG_GREGORIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(24));
        assertThat(cal.get(Calendar.MONTH), is(Calendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(1964));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2438450));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2438450"));
    }

    @Test
    public void testParseJulianDate() {
        Calendar cal;

        // 02.01.4713 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("02.01.4713 bc", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(2));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(4713));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(1));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("1"));

        // 01.01.0814 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("-814", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(814));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(1424110));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("1424110"));

        // 01.01.0814 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("-01.01.814", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(814));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(1424110));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("1424110"));

        // 15.03.0044 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("BC 15.03.44", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.MARCH));
        assertThat(cal.get(Calendar.YEAR), is(44));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        // 01.01.0001 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0001 BC", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        // 31.12.0001 BC (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("31.12.0001 v. Chr", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(31));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.DECEMBER));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        // 01.01.0000 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0000", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        // 01.01.0001 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.01 AD", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        // 04.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.1582 N. Chr", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(4));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299160));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299160"));

        // 05.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(5));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299161));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299161"));

        // 06.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(6));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299162));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299162"));

        // 15.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299171));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299171"));

        // 16.10.1582 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(16));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299172));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299172"));

        // 28.02.1700 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("28.02.1700", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(28));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(1700));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2342041));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2342041"));

        // 29.02.1700 AD (julian)
        cal = MCRCalendar.getHistoryDateAsCalendar("29.02.1700", false, MCRCalendar.TAG_JULIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(29));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(1700));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.AD));
    }

    @Test
    public void testParseIslamicDate() {
        Calendar cal;

        // 01.01.0001 h. (islamic)
        cal = MCRCalendar.getHistoryDateAsCalendar("01.01.0001 h.", false, MCRCalendar.TAG_ISLAMIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(IslamicCalendar.MUHARRAM));
        assertThat(cal.get(Calendar.YEAR), is(1));

        // first day of Islamic calendar is 16.7.622 in Gregorian/Julian calendar
        Calendar firstIslamGreg = MCRCalendar.getHistoryDateAsCalendar("16.7.622", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(MCRCalendar.getJulianDayNumber(firstIslamGreg), is(MCRCalendar.getJulianDayNumber(cal)));
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(1948440));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("1948440"));

        // 01.01.800 H. (islamic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", false, MCRCalendar.TAG_ISLAMIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(IslamicCalendar.MUHARRAM));
        assertThat(cal.get(Calendar.YEAR), is(800));

        // 30.01.800 H. (islamic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800 H.", true, MCRCalendar.TAG_ISLAMIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(30));
        assertThat(cal.get(Calendar.MONTH), is(IslamicCalendar.MUHARRAM));
        assertThat(cal.get(Calendar.YEAR), is(800));

        // 29.12.800 H. (islamic)
        cal = MCRCalendar.getHistoryDateAsCalendar("800", true, MCRCalendar.TAG_ISLAMIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(29));
        assertThat(cal.get(Calendar.MONTH), is(IslamicCalendar.DHU_AL_HIJJAH));
        assertThat(cal.get(Calendar.YEAR), is(800));
    }

    @Test
    public void testParseCopticDate() {
        Calendar cal;

        // 01.01.0001 A.M. (coptic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1 a.M.", false, MCRCalendar.TAG_COPTIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(CopticCalendar.TOUT));
        assertThat(cal.get(Calendar.YEAR), is(1));

        // first day of Coptic calendar is 29.8.284 in Gregorian/Julian calendar
        Calendar firstCopGreg = MCRCalendar.getHistoryDateAsCalendar("29.8.284", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(MCRCalendar.getJulianDayNumber(firstCopGreg), is(MCRCalendar.getJulianDayNumber(cal)));
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(1825030));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("1825030"));

        // 01.01.1724 A.M. (coptic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1724 A.M.", false, MCRCalendar.TAG_COPTIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(CopticCalendar.TOUT));
        assertThat(cal.get(Calendar.YEAR), is(1724));

        // 05.13.1724 E.E. (coptic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1724 a.M.", true, MCRCalendar.TAG_COPTIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(5));
        assertThat(cal.get(Calendar.MONTH), is(CopticCalendar.NASIE));
        assertThat(cal.get(Calendar.YEAR), is(1724));
    }

    @Test
    public void testParseEthiopianDate() {
        Calendar cal;

        // 01.01.0001 E.E. (ethiopic)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 E.E.", false, MCRCalendar.TAG_ETHIOPIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(EthiopicCalendar.MESKEREM));
        assertThat(cal.get(Calendar.YEAR), is(1));

        // first day of Ehtiopian calendar is 29.8.8 in Gregorian/Julian calendar
        Calendar firstEthGreg = MCRCalendar.getHistoryDateAsCalendar("29.8.8", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(MCRCalendar.getJulianDayNumber(firstEthGreg), is(MCRCalendar.getJulianDayNumber(cal)));
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(1724221));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("1724221"));

        // 05.13.2000 E.E. (ethiopic)
        cal = MCRCalendar.getHistoryDateAsCalendar("2000 E.E.", true, MCRCalendar.TAG_ETHIOPIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(5));
        assertThat(cal.get(Calendar.MONTH), is(EthiopicCalendar.PAGUMEN));
        assertThat(cal.get(Calendar.YEAR), is(2000));
    }

    @Test
    public void testParseHebrewDate() {
        Calendar cal;

        // 1.1.1 (hebrew)
        cal = MCRCalendar.getHistoryDateAsCalendar("1", false, MCRCalendar.TAG_HEBREW);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(HebrewCalendar.TISHRI));
        assertThat(cal.get(Calendar.YEAR), is(1));

        // first day of Hebrew calendar is 7.10.3761 BC in Gregorian/Julian calendar
        Calendar firstHebGreg = MCRCalendar.getHistoryDateAsCalendar("7.10.3761 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(MCRCalendar.getJulianDayNumber(firstHebGreg)));
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(347998));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("347998"));

        // 04.10.1582 (hebrew)
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.1582", false, MCRCalendar.TAG_HEBREW);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(4));
        assertThat(cal.get(Calendar.MONTH), is(HebrewCalendar.SIVAN));
        assertThat(cal.get(Calendar.YEAR), is(1582));

        // 05.10.1582 (hebrew)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.1582", false, MCRCalendar.TAG_HEBREW);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(5));
        assertThat(cal.get(Calendar.MONTH), is(HebrewCalendar.SIVAN));
        assertThat(cal.get(Calendar.YEAR), is(1582));

        // 05.10.1582 (hebrew)
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.1582", false, MCRCalendar.TAG_HEBREW);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(6));
        assertThat(cal.get(Calendar.MONTH), is(HebrewCalendar.SIVAN));
        assertThat(cal.get(Calendar.YEAR), is(1582));

        // 15.10.1582 (hebrew)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.1582", false, MCRCalendar.TAG_HEBREW);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(HebrewCalendar.SIVAN));
        assertThat(cal.get(Calendar.YEAR), is(1582));

        // 16.10.1582 (hebrew)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.1582", false, MCRCalendar.TAG_HEBREW);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(16));
        assertThat(cal.get(Calendar.MONTH), is(HebrewCalendar.SIVAN));
        assertThat(cal.get(Calendar.YEAR), is(1582));

        // 01.01.1800 (hebrew) with missing day and last=false
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1800", false, MCRCalendar.TAG_HEBREW);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(HebrewCalendar.TISHRI));
        assertThat(cal.get(Calendar.YEAR), is(1800));

        // 24.02.1964 (hebrew)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24", true, MCRCalendar.TAG_HEBREW);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(24));
        assertThat(cal.get(Calendar.MONTH), is(HebrewCalendar.HESHVAN));
        assertThat(cal.get(Calendar.YEAR), is(1964));
    }

    @Test
    public void testParseBuddhistDate() {
        Calendar cal;

        // 1.1.1 (buddhist)
        cal = MCRCalendar.getHistoryDateAsCalendar("1", false, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(1));

        // first day of Buddhist calendar is 1.1.543 BC in Gregorian/Julian calendar
        Calendar firstBuddhGreg = MCRCalendar.getHistoryDateAsCalendar("1.1.543 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(MCRCalendar.getJulianDayNumber(firstBuddhGreg)));
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(1523093));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("1523093"));

        // year 0
        cal = MCRCalendar.getHistoryDateAsCalendar("0", false, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(0));

        // year -1
        cal = MCRCalendar.getHistoryDateAsCalendar("-1.1.1", false, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(0));

        // year -100
        cal = MCRCalendar.getHistoryDateAsCalendar("-100", false, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(-99));


        // 04.10.2125 (buddhist) -> year 1582 in gregorian calendar
        cal = MCRCalendar.getHistoryDateAsCalendar("04.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(4));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(2125));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299160));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299160"));

        // 05.10.2125 (buddhist)
        cal = MCRCalendar.getHistoryDateAsCalendar("05.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(2125));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299161));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299161"));

        // 06.10.2125 (buddhist)
        cal = MCRCalendar.getHistoryDateAsCalendar("06.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(2125));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299161));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299161"));

        // 15.10.2125 (buddhist)
        cal = MCRCalendar.getHistoryDateAsCalendar("15.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(2125));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299161));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299161"));

        // 16.10.2125 (buddhist)
        cal = MCRCalendar.getHistoryDateAsCalendar("16.10.2125", false, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(16));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(2125));

        assertThat(MCRCalendar.getJulianDayNumber(cal), is(2299162));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("2299162"));

        // 01.01.1800 (buddhist) with missing day and last=false
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1800", false, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(1800));

        // 24.02.1964 (buddhist)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24", true, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(24));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(1964));

        // 24.02.1964 (buddhist)
        cal = MCRCalendar.getHistoryDateAsCalendar("1964-02-24 B.E.", true, MCRCalendar.TAG_BUDDHIST);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(24));
        assertThat(cal.get(Calendar.MONTH), is(BuddhistCalendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(1964));
    }

    @Test
    public void testParsePersianDate() {
        Calendar cal;

        // 01.01.0001  (persian) -> 22.3.622 greg
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_PERSIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(21));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.MARCH));
        assertThat(cal.get(Calendar.YEAR), is(622));

        // first day of Persian calendar is 21.3.622 in Gregorian/Julian calendar
        Calendar firstPersGreg1 = MCRCalendar.getHistoryDateAsCalendar("21.3.622", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(MCRCalendar.getJulianDayNumber(firstPersGreg1)));
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(1948323));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("1948323"));

        // 01.01.800 (persian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800", false, MCRCalendar.TAG_PERSIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(21));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.MARCH));
        assertThat(cal.get(Calendar.YEAR), is(1421));

        // 31.01.800 (persian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.800", true, MCRCalendar.TAG_PERSIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(20));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.APRIL));
        assertThat(cal.get(Calendar.YEAR), is(1421));

        // 29.12.800 (persian)
        cal = MCRCalendar.getHistoryDateAsCalendar("800", true, MCRCalendar.TAG_PERSIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(20));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.MARCH));
        assertThat(cal.get(Calendar.YEAR), is(1422));

        // gregorian calendar reform on October, 5th 1582 -> skip days between 5 and 15
        cal = MCRCalendar.getHistoryDateAsCalendar("12.7.961", false, MCRCalendar.TAG_PERSIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(4));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));

        cal = MCRCalendar.getHistoryDateAsCalendar("13.7.961", false, MCRCalendar.TAG_PERSIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));

        cal = MCRCalendar.getHistoryDateAsCalendar("14.7.961", false, MCRCalendar.TAG_PERSIC);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(16));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));
    }

    @Test
    public void testParseArmenianDate() {
        Calendar cal;

        // 01.01.0001  (armenian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(13));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.JULY));
        assertThat(cal.get(Calendar.YEAR), is(552));

        // first day of Armenian calendar is 13.7.552 in Gregorian/11.7.552 in Julian calendar
        Calendar firstArmGreg = MCRCalendar.getHistoryDateAsCalendar("13.7.552", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(MCRCalendar.getJulianDayNumber(firstArmGreg)));
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(1922870));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("1922870"));

        cal = MCRCalendar.getHistoryDateAsCalendar("1.2.1", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(12));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.AUGUST));
        assertThat(cal.get(Calendar.YEAR), is(552));

        cal = MCRCalendar.getHistoryDateAsCalendar("5.13.1", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(12));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.JULY));
        assertThat(cal.get(Calendar.YEAR), is(553));

        cal = MCRCalendar.getHistoryDateAsCalendar("2.9.48", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(28));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(600));

        cal = MCRCalendar.getHistoryDateAsCalendar("3.9.48", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(29));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(600));

        cal = MCRCalendar.getHistoryDateAsCalendar("4.9.48", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.MARCH));
        assertThat(cal.get(Calendar.YEAR), is(600));

        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1462", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(26));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.JULY));
        assertThat(cal.get(Calendar.YEAR), is(2012));

        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.101", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(18));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.JUNE));
        assertThat(cal.get(Calendar.YEAR), is(652));

        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1031", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(29));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1581));

        // checks for gregorian calendar switch in 1582
        // 11.12.1031 arm -> 4.10.1582 greg
        cal = MCRCalendar.getHistoryDateAsCalendar("11.12.1031", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(4));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));

        // 12.12.1031 arm -> 15.10.1582 greg
        cal = MCRCalendar.getHistoryDateAsCalendar("12.12.1031", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));

        // 13.12.1031 arm -> 16.10.1582 greg
        cal = MCRCalendar.getHistoryDateAsCalendar("13.12.1031", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(16));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.OCTOBER));
        assertThat(cal.get(Calendar.YEAR), is(1582));

        // 12.500 (last=false) -> 1.12.500 -> 8.2.1052 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("12.500", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(4));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(1052));

        // 12.500 (last=true) -> 30.12.500 -> 8.3.1052 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("12.500", true, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(4));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.MARCH));
        assertThat(cal.get(Calendar.YEAR), is(1052));

        // 500 (last=false) -> 1.1.500 -> 15.3.1051 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("500", false, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(11));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.MARCH));
        assertThat(cal.get(Calendar.YEAR), is(1051));

        // 500 (last=true) -> 5.13.500 -> 13.3.1052 (greg)
        cal = MCRCalendar.getHistoryDateAsCalendar("500", true, MCRCalendar.TAG_ARMENIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(9));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.MARCH));
        assertThat(cal.get(Calendar.YEAR), is(1052));
    }

    @Test
    public void testParseEgyptianDate() {
        Calendar cal;

        // 01.01.0001  (egyptian)
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1.1", false, MCRCalendar.TAG_EGYPTIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(18));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(747));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        // first day of Egyptian calendar is 18.2.747 BC in Gregorian/Julian calendar
        Calendar firstEgGreg = MCRCalendar.getHistoryDateAsCalendar("18.2.747 BC", false, MCRCalendar.TAG_GREGORIAN);
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(MCRCalendar.getJulianDayNumber(firstEgGreg)));
        assertThat(MCRCalendar.getJulianDayNumber(cal), is(1448630));
        assertThat(MCRCalendar.getJulianDayNumberAsString(cal), is("1448630"));

        // 1.1 -> 30.1.1 in Gregorian date: 19.3.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("1.1 A.N.", true, MCRCalendar.TAG_EGYPTIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(19));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.MARCH));
        assertThat(cal.get(Calendar.YEAR), is(747));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        // 10.1 (last=false) -> 1.10.1 in Gregorian date: 14.12.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("10.1 A.N.", false, MCRCalendar.TAG_EGYPTIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(15));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.NOVEMBER));
        assertThat(cal.get(Calendar.YEAR), is(747));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        // 10.1 (last=true) -> 30.10.1 in Gregorian date: 14.12.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("10.1 A.N.", true, MCRCalendar.TAG_EGYPTIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(14));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.DECEMBER));
        assertThat(cal.get(Calendar.YEAR), is(747));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        // 1.2.1 -> in Gregorian date: 20.3.747 BC
        cal = MCRCalendar.getHistoryDateAsCalendar("1.2.1 A.N.", false, MCRCalendar.TAG_EGYPTIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(20));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.MARCH));
        assertThat(cal.get(Calendar.YEAR), is(747));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        // 13.1 (last=false) -> 1.13.1 (in Gregorian date: 17.2.746)
        cal = MCRCalendar.getHistoryDateAsCalendar("13.1 A.N.", false, MCRCalendar.TAG_EGYPTIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(13));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(746));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));

        // 1 (last=true) -> 5.13.1 (in Gregorian date: 17.2.746)
        cal = MCRCalendar.getHistoryDateAsCalendar("1 A.N.", true, MCRCalendar.TAG_EGYPTIAN);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(17));
        assertThat(cal.get(Calendar.MONTH), is(GregorianCalendar.FEBRUARY));
        assertThat(cal.get(Calendar.YEAR), is(746));
        assertThat(cal.get(Calendar.ERA), is(GregorianCalendar.BC));
    }

    @Test
    public void testParseJapaneseDate() {
        Calendar cal;


        // Meiji era: 8.9.1868 - 29.07.1912
        cal = MCRCalendar.getHistoryDateAsCalendar("8.9.M1", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(8));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.SEPTEMBER));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.MEIJI));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("8.9.1868", false, MCRCalendar.TAG_GREGORIAN))));

        cal = MCRCalendar.getHistoryDateAsCalendar("29.7.M45", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(29));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.JULY));
        assertThat(cal.get(Calendar.YEAR), is(45));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.MEIJI));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("29.7.1912", false, MCRCalendar.TAG_GREGORIAN))));

        cal = MCRCalendar.getHistoryDateAsCalendar("30.7.M45", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(30));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.JULY));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.TAISHO));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("30.7.1912", false, MCRCalendar.TAG_GREGORIAN))));

        // Taisho era: 30.7.1912-24.12.1926
        cal = MCRCalendar.getHistoryDateAsCalendar("30.7.T1", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(30));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.JULY));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.TAISHO));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("30.7.1912", false, MCRCalendar.TAG_GREGORIAN))));

        cal = MCRCalendar.getHistoryDateAsCalendar("24.12.T15", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(24));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.DECEMBER));
        assertThat(cal.get(Calendar.YEAR), is(15));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.TAISHO));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("24.12.1926", false, MCRCalendar.TAG_GREGORIAN))));

        cal = MCRCalendar.getHistoryDateAsCalendar("25.12.T15", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(25));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.DECEMBER));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.SHOWA));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("25.12.1926", false, MCRCalendar.TAG_GREGORIAN))));

        // Showa era: 25.12.1926-07.01.1989
        cal = MCRCalendar.getHistoryDateAsCalendar("25.12.S1", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(25));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.DECEMBER));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.SHOWA));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("25.12.1926", false, MCRCalendar.TAG_GREGORIAN))));

        cal = MCRCalendar.getHistoryDateAsCalendar("7.1.S64", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(7));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(64));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.SHOWA));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("7.1.1989", false, MCRCalendar.TAG_GREGORIAN))));

        cal = MCRCalendar.getHistoryDateAsCalendar("8.1.S64", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(8));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.HEISEI));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("8.1.1989", false, MCRCalendar.TAG_GREGORIAN))));

        // Heisei era: 08.01.1989-30.04.2019
        cal = MCRCalendar.getHistoryDateAsCalendar("8.1.H1", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(8));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.JANUARY));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.HEISEI));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("8.1.1989", false, MCRCalendar.TAG_GREGORIAN))));

        cal = MCRCalendar.getHistoryDateAsCalendar("30.4.H31", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(30));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.APRIL));
        assertThat(cal.get(Calendar.YEAR), is(31));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.HEISEI));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("30.4.2019", false, MCRCalendar.TAG_GREGORIAN))));

        cal = MCRCalendar.getHistoryDateAsCalendar("1.5.H31", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.MAY));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.REIWA));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("1.5.2019", false, MCRCalendar.TAG_GREGORIAN))));

        // Reiwa era: 01.05.2019 - present
        cal = MCRCalendar.getHistoryDateAsCalendar("1.5.R1", true, MCRCalendar.TAG_JAPANESE);
        assertThat(cal.get(Calendar.DAY_OF_MONTH), is(1));
        assertThat(cal.get(Calendar.MONTH), is(JapaneseCalendar.MAY));
        assertThat(cal.get(Calendar.YEAR), is(1));
        assertThat(cal.get(Calendar.ERA), is(JapaneseCalendar.REIWA));

        assertThat(MCRCalendar.getJulianDayNumber(cal),
                is(MCRCalendar.getJulianDayNumber(
                        MCRCalendar.getHistoryDateAsCalendar("1.5.2019", false, MCRCalendar.TAG_GREGORIAN))));

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
        try {
            MCRCalendar.getHistoryDateAsCalendar("29.02.1700", true, MCRCalendar.TAG_GREGORIAN);
            fail("Expected exception!");
        } catch (MCRException ignored) {
            // 1700 in gregorian calendar is no leap year
        }
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

    @Test
    public void testIsoFormat() {
        assertThat(MCRCalendar.isoFormat("23.03.2022"), is(false));
        assertThat(MCRCalendar.isoFormat("23/03/2022"), is(false));
        assertThat(MCRCalendar.isoFormat("23-03-2022"), is(true));
        assertThat(MCRCalendar.isoFormat("-23.03.2022"), is(false));
        assertThat(MCRCalendar.isoFormat("-23/03/2022"), is(false));
        assertThat(MCRCalendar.isoFormat("-23-03-2022"), is(true));

        assertThat(MCRCalendar.isoFormat("800"), is(false));
    }

    @Test
    public void testDelimiter() {
        assertThat(MCRCalendar.delimiter("23.03.2022"), is("."));
        assertThat(MCRCalendar.delimiter("23/03/2022"), is("/"));
        assertThat(MCRCalendar.delimiter("23-03-2022"), is("-"));
        assertThat(MCRCalendar.delimiter("-23.03.2022"), is("."));
        assertThat(MCRCalendar.delimiter("-23/03/2022"), is("/"));
        assertThat(MCRCalendar.delimiter("-23-03-2022"), is("-"));

        assertThat(MCRCalendar.delimiter("800"), is("."));
    }

    @Test
    public void testBeforeZero() {
        assertThat(MCRCalendar.beforeZero("23.03.2022", MCRCalendar.CalendarType.Gregorian), is(false));
        assertThat(MCRCalendar.beforeZero("23/03/2022", MCRCalendar.CalendarType.Gregorian), is(false));
        assertThat(MCRCalendar.beforeZero("23-03-2022", MCRCalendar.CalendarType.Gregorian), is(false));
        assertThat(MCRCalendar.beforeZero("-23.03.2022", MCRCalendar.CalendarType.Gregorian), is(true));
        assertThat(MCRCalendar.beforeZero("-23/03/2022", MCRCalendar.CalendarType.Gregorian), is(true));
        assertThat(MCRCalendar.beforeZero("-23-03-2022", MCRCalendar.CalendarType.Gregorian), is(true));

        assertThat(MCRCalendar.beforeZero("23-03-2022 AD", MCRCalendar.CalendarType.Gregorian), is(false));
        assertThat(MCRCalendar.beforeZero("AD 23-03-2022", MCRCalendar.CalendarType.Gregorian), is(false));
        assertThat(MCRCalendar.beforeZero("23-03-2022 BC", MCRCalendar.CalendarType.Gregorian), is(true));
        assertThat(MCRCalendar.beforeZero("BC 23-03-2022", MCRCalendar.CalendarType.Gregorian), is(true));
    }

    @Test
    public void testGetLastDayOfMonth() {
        assertThat(MCRCalendar.getLastDayOfMonth(GregorianCalendar.JANUARY, 2000, MCRCalendar.CalendarType.Gregorian),
                is(31));
        assertThat(MCRCalendar.getLastDayOfMonth(GregorianCalendar.FEBRUARY, 2000, MCRCalendar.CalendarType.Gregorian),
                is(29));
        assertThat(MCRCalendar.getLastDayOfMonth(GregorianCalendar.FEBRUARY, 2001, MCRCalendar.CalendarType.Gregorian),
                is(28));
        assertThat(MCRCalendar.getLastDayOfMonth(GregorianCalendar.MARCH, 2000, MCRCalendar.CalendarType.Gregorian),
                is(31));
        assertThat(MCRCalendar.getLastDayOfMonth(GregorianCalendar.APRIL, 2000, MCRCalendar.CalendarType.Gregorian),
                is(30));

        assertThat(MCRCalendar.getLastDayOfMonth(CopticCalendar.NASIE, 2000, MCRCalendar.CalendarType.Coptic),
                is(5));

        assertThat(MCRCalendar.getLastDayOfMonth(11, 2000, MCRCalendar.CalendarType.Egyptian),
                is(30));
        assertThat(MCRCalendar.getLastDayOfMonth(12, 2000, MCRCalendar.CalendarType.Egyptian),
                is(5));

        assertThat(MCRCalendar.getLastDayOfMonth(11, 2000, MCRCalendar.CalendarType.Armenian),
                is(30));
        assertThat(MCRCalendar.getLastDayOfMonth(12, 2000, MCRCalendar.CalendarType.Armenian),
                is(5));
    }

    @Test
    public void testIsLeapYear() {
        assertThat(MCRCalendar.isLeapYear(2000, MCRCalendar.CalendarType.Gregorian), is(true));
        assertThat(MCRCalendar.isLeapYear(1999, MCRCalendar.CalendarType.Gregorian), is(false));
        assertThat(MCRCalendar.isLeapYear(1582, MCRCalendar.CalendarType.Gregorian), is(false));
        assertThat(MCRCalendar.isLeapYear(1900, MCRCalendar.CalendarType.Gregorian), is(false));
        assertThat(MCRCalendar.isLeapYear(1900, MCRCalendar.CalendarType.Julian), is(true));
    }
}

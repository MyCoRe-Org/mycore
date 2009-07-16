/**
 * 
 * $Revision: 1.3 $ $Date: 2008/06/02 10:10:05 $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.common;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;

import org.mycore.common.MCRTestCase;

/**
 * This class is a JUnit test case for org.mycore.common.MCRCalendar.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision: 1.3 $ $Date: 2008/06/02 10:10:05 $
 * 
 */
public class MCRCalendarTest extends MCRTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRCalendar.getDateToFormattedString()'
     */
    public void testGetDateToFormattedString() {
        GregorianCalendar defaultcal = new GregorianCalendar();
        defaultcal.set(1964, 1, 24);
        String dstring;
        Calendar cal;
        // gregorian without format
        cal = (GregorianCalendar) defaultcal.clone();
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian without format", "24.02.1964 AD", dstring);
        // gregorian with format
        cal = (GregorianCalendar) defaultcal.clone();
        dstring = MCRCalendar.getDateToFormattedString(cal, "dd.MM.yyyy G");
        assertEquals("gregorian with format", "24.02.1964 AD", dstring);
        // gregorian with format
        cal = (GregorianCalendar) defaultcal.clone();
        dstring = MCRCalendar.getDateToFormattedString(cal, "yyyy-MM-dd");
        assertEquals("gregorian with format", "1964-02-24", dstring);
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRCalendar.getGregorianHistoryDate(String, boolean, String)'
     */
    public void testGetGregorianHistoryDate() {
        String cstring, dstring;
        cstring = MCRCalendar.getDateToFormattedString(new GregorianCalendar());
        Calendar cal;
        /* common check * */
        // all entries are empty
        try {
            cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate(null, false, null);
        } catch (MCRException e) {
            cal = (GregorianCalendar) new GregorianCalendar();
        }
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("Date is not the current date.", cstring, dstring);
        // 0A.01.0001 BC (wrong gregorian)
        try {
            cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("-0A.01.0001", false, MCRCalendar.TAG_GREGORIAN);
        } catch (MCRException e) {
            cal = (GregorianCalendar) new GregorianCalendar();
        }
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", cstring, dstring);

        /* gregorian check */
        // 1 (gregorian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("1", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "01.01.0001 AD", dstring);
        // 1.1 (gregorian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("1.1", true, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "31.01.0001 AD", dstring);
        // 1.1.1 (gregorian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("1.1.1", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "01.01.0001 AD", dstring);
        // 1.1.1 QU(gregorian)
        try {
            cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("1.1.1 QU", false, MCRCalendar.TAG_GREGORIAN);
        } catch (MCRException e) {
            cal = (GregorianCalendar) new GregorianCalendar();
        }
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", cstring, dstring);
        // 01.01.0001 BC (gregorian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("01.01.0001 bc", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "01.01.0001 BC", dstring);
        // 01.01.0001 BC (gregorian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("01.01.0001 v. Chr", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "01.01.0001 BC", dstring);
        // 01.01.0001 AD (gregorian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("01.01.0001 N. Chr", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "01.01.0001 AD", dstring);
        // 01.01.0001 BC (gregorian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("01.01.0001 BC", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "01.01.0001 BC", dstring);
        // 01.01.0001 BC (gregorian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("-01.01.0001", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "01.01.0001 BC", dstring);
        // 01.01.0000 AD (gregorian)
        cal = MCRCalendar.getGregorianHistoryDate("01.01.0000", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "01.01.0001 BC", dstring);
        // 01.01.0001 AD (gregorian)
        cal = MCRCalendar.getGregorianHistoryDate("01.01.0001", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "01.01.0001 AD", dstring);
        // 04.10.1582 AD (gregorian)
        cal = MCRCalendar.getGregorianHistoryDate("04.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "04.10.1582 AD", dstring);
        // 05.10.1582 AD (gregorian)
        cal = MCRCalendar.getGregorianHistoryDate("05.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "15.10.1582 AD", dstring);
        // 06.10.1582 AD (gregorian)
        cal = MCRCalendar.getGregorianHistoryDate("16.10.1582", false, MCRCalendar.TAG_GREGORIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("gregorian", "16.10.1582 AD", dstring);

        /* julian check */
        // 1 (julian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("1", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("julian", "01.01.0001 AD", dstring);
        // 1.1 (julian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("1.1", true, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("julian", "31.01.0001 AD", dstring);
        // 1.1.1 (julian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("1.1.1", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("julian", "30.12.0001 BC", dstring);
        // 1.1.1 QU(julian)
        /*
        try {
            cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("1.1.1 QU", false, MCRCalendar.TAG_JULIAN);
        } catch (MCRException e) {
            cal = (GregorianCalendar) new GregorianCalendar();
        }
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("julian", cstring, dstring);
        */
        // 01.01.0001 BC (julian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("01.01.0001 bc", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("julian", "30.12.0002 BC", dstring);
        // 01.01.0001 BC (julian)
        cal = (GregorianCalendar) MCRCalendar.getGregorianHistoryDate("-01.01.0001", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("julian", "30.12.0002 BC", dstring);
        // 01.01.0001 AD (julian)
        cal = MCRCalendar.getGregorianHistoryDate("01.01.0001 AD", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("julian", "30.12.0001 BC", dstring);
        // 04.10.1582 AD (gregorian)
        /*
        cal = MCRCalendar.getGregorianHistoryDate("04.10.1582", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("julian", "14.10.1582 AD", dstring);
        */
        // 05.10.1582 AD (gregorian)
        cal = MCRCalendar.getGregorianHistoryDate("05.10.1582", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("julian", "15.10.1582 AD", dstring);
        // 06.10.1582 AD (gregorian)
        cal = MCRCalendar.getGregorianHistoryDate("16.10.1582", false, MCRCalendar.TAG_JULIAN);
        dstring = MCRCalendar.getDateToFormattedString(cal);
        assertEquals("julian", "26.10.1582 AD", dstring);
    }
    
    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRCalendar.getJulianDay(GregorianCalendar)'
     */
    public void testGetJulianDay() {
        GregorianCalendar greg;
        // 24.03.0005 BC (julianisch)
        greg = new GregorianCalendar(5,2,24);
        greg.set(GregorianCalendar.ERA, GregorianCalendar.BC);
        int result = MCRCalendar.getJulianDayNumber(greg);
        assertEquals("Julian date 24.03.0005 BC is not 1719680.",1719680,result);
        // 31.12.1 BC (julianisch)
        greg = new GregorianCalendar(1,11,31);
        greg.set(GregorianCalendar.ERA, GregorianCalendar.BC);
        result = MCRCalendar.getJulianDayNumber(greg);
        assertEquals("Date 31.12.0001 BC is not 1721423.",1721423,result);
        // 1.1.1 AD (julianisch)
        greg = new GregorianCalendar(1,0,1);
        greg.set(GregorianCalendar.ERA, GregorianCalendar.AD);
        result = MCRCalendar.getJulianDayNumber(greg);
        assertEquals("Date 01.01.0001 is not 1721424.",1721424,result);
        // 27.01.333 AD (julianisch)
        result = MCRCalendar.getJulianDayNumber(new GregorianCalendar(333,0,27));
        assertEquals("Date 27.01.0333 AD is not 1842713.",1842713,result);
        // 04.10.1582 AD (julianisch)
        result = MCRCalendar.getJulianDayNumber(new GregorianCalendar(1582,9,4));
        assertEquals("Date 04.10.1582 AD is not 2299160.",2299160,result);
        // 15.10.1582 AD (gregorianisch)
        result = MCRCalendar.getJulianDayNumber(new GregorianCalendar(1582,9,15));
        assertEquals("Date 15.10.1582 AD is not 2299161.",2299161,result);
        // 01.01.2000 AD (gregorianisch)
        result = MCRCalendar.getJulianDayNumber(new GregorianCalendar(2000,0,1));
        assertEquals("Date 01.01.2000 AD is not 2451545.",2451545,result);     
        // 29.02.2003 AD (gregorianisch)
        result = MCRCalendar.getJulianDayNumber(new GregorianCalendar(2003,1,29));
        assertEquals("Date 29.02.2003 AD is not 2452700.",2452700,result);     
        // 01.03.2003 AD (gregorianisch)
        result = MCRCalendar.getJulianDayNumber(new GregorianCalendar(2003,2,1));
        assertEquals("Date 01.03.2003 AD is not 2452700.",2452700,result);     
        // 28.02.2003 AD (gregorianisch)
        result = MCRCalendar.getJulianDayNumber(new GregorianCalendar(2003,1,28));
        assertEquals("Date 28.02.2003 AD is not 2452699.",2452699,result);     
        // 01.03.2003 AD (gregorianisch)
        result = MCRCalendar.getJulianDayNumber(new GregorianCalendar(2003,2,0));
        assertEquals("Date 00.03.2003 AD is not 2452699.",2452699,result);             
    }
}

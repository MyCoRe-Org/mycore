/**
 * 
 * $Revision$ $Date$
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

package org.mycore.datamodel.metadata;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRTestCase;

/**
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMetaHistoryDate.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 *
 */
public class MCRMetaHistoryDateTest extends MCRTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setFromDOM(Element)'
     */
    public void testSetFromDOM() {

    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.createXML()'
     */
    public void testCreateXML() {

    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.getHistoryDate(String, boolean, String)'
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.getHistoryDate(String, boolean)'
     */
    public void testGetHistoryDate() {
    String dstring;
    Calendar cal;
    // 01.01.0001 BC (julianisch/gregorianisch)
    cal = (GregorianCalendar)MCRMetaHistoryDate.getHistoryDate("-01.01.0001",false,MCRMetaHistoryDate.TAG_GREGORIAN);
    dstring = MCRMetaHistoryDate.getDateToGregorianString(cal);
    assertEquals("Date is not 01.01.0001 BC.","01.01.0001 BC",dstring);            
    // 01.01.0001 BC (julianisch/gregorianisch)
    cal = (GregorianCalendar)MCRMetaHistoryDate.getGregorianHistoryDate("-01.01.0001",false);
    dstring = MCRMetaHistoryDate.getDateToGregorianString(cal);
    assertEquals("Date is not 01.01.0001 BC.","01.01.0001 BC",dstring);            
    // 01.01.0001 AD (julianisch/gregorianisch)
    cal = MCRMetaHistoryDate.getHistoryDate("01.01.0001 AD",false,null);
    dstring = MCRMetaHistoryDate.getDateToGregorianString(cal);
    assertEquals("Date is not 01.01.0001 AD.","01.01.0001 AD",dstring);            
    // 01.01.0001 AD (julianisch/gregorianisch)
    cal = MCRMetaHistoryDate.getGregorianHistoryDate("01.01.0001",false);
    dstring = MCRMetaHistoryDate.getDateToGregorianString(cal);
    assertEquals("Date is not 01.01.0001 AD.","01.01.0001 AD",dstring);            
    // 01.01.0045 BC (julianisch/gregorianisch)
    cal = MCRMetaHistoryDate.getGregorianHistoryDate("-45",false);
    dstring = MCRMetaHistoryDate.getDateToGregorianString(cal);
    assertEquals("Date is not 01.01.0045 BC.","01.01.0045 BC",dstring);            
    // 31.12.0045 BC (julianisch/gregorianisch)
    cal = MCRMetaHistoryDate.getGregorianHistoryDate("BC45",true);
    dstring = MCRMetaHistoryDate.getDateToGregorianString(cal);
    assertEquals("Date is not 31.12.0045 BC.","31.12.0045 BC",dstring);            
    // 31.12.0045 BC (julianisch/gregorianisch)
    cal = MCRMetaHistoryDate.getGregorianHistoryDate("10.45 v. Chr.",true);
    dstring = MCRMetaHistoryDate.getDateToGregorianString(cal);
    assertEquals("Date is not 31.10.0045 BC.","31.10.0045 BC",dstring);            
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.getJulianDayNumber(Calendar)'
     */
    public void testGetJulianDayNumber() {
        GregorianCalendar greg;
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        int result;
        // First day of Julianian Day number 1.1.4712 BC         
        result = MCRMetaHistoryDate.getJulianDayNumber(hd.getVon());
        assertEquals("Date 01.01.4712 BC is not 0.",0,result);
        // First day of (OUR) Julianian Day number 31.12.3999 AD         
        result = MCRMetaHistoryDate.getJulianDayNumber(hd.getBis());
        assertEquals("Date 31.12.3999 AD is not 3182057.",3182057,result);
        // 24.03.0005 BC (julianisch)
        greg = new GregorianCalendar(5,2,24);
        greg.set(GregorianCalendar.ERA, GregorianCalendar.BC);
        result = MCRMetaHistoryDate.getJulianDayNumber(greg);
        assertEquals("Julian date 24.03.0005 BC is not 1719680.",1719680,result);
        // 31.12.1 BC (julianisch)
        greg = new GregorianCalendar(1,11,31);
        greg.set(GregorianCalendar.ERA, GregorianCalendar.BC);
        result = MCRMetaHistoryDate.getJulianDayNumber(greg);
        assertEquals("Date 31.12.0001 BC is not 1721423.",1721423,result);
        // 1.1.1 AD (julianisch)
        greg = new GregorianCalendar(1,0,1);
        greg.set(GregorianCalendar.ERA, GregorianCalendar.AD);
        result = MCRMetaHistoryDate.getJulianDayNumber(greg);
        assertEquals("Date 01.01.0001 is not 1721424.",1721424,result);
        // 27.01.333 AD (julianisch)
        result = MCRMetaHistoryDate.getJulianDayNumber(new GregorianCalendar(333,0,27));
        assertEquals("Date 27.01.0333 AD is not 1842713.",1842713,result);
        // 04.10.1582 AD (julianisch)
        result = MCRMetaHistoryDate.getJulianDayNumber(new GregorianCalendar(1582,9,4));
        assertEquals("Date 04.10.1582 AD is not 2299160.",2299160,result);
        // 15.10.1582 AD (gregorianisch)
        result = MCRMetaHistoryDate.getJulianDayNumber(new GregorianCalendar(1582,9,15));
        assertEquals("Date 15.10.1582 AD is not 2299161.",2299161,result);
        // 01.01.2000 AD (gregorianisch)
        result = MCRMetaHistoryDate.getJulianDayNumber(new GregorianCalendar(2000,0,1));
        assertEquals("Date 01.01.2000 AD is not 2451545.",2451545,result);     
        // 29.02.2003 AD (gregorianisch)
        result = MCRMetaHistoryDate.getJulianDayNumber(new GregorianCalendar(2003,1,29));
        assertEquals("Date 29.02.2003 AD is not 2452700.",2452700,result);     
        // 01.03.2003 AD (gregorianisch)
        result = MCRMetaHistoryDate.getJulianDayNumber(new GregorianCalendar(2003,2,1));
        assertEquals("Date 01.03.2003 AD is not 2452700.",2452700,result);     
        // 28.02.2003 AD (gregorianisch)
        result = MCRMetaHistoryDate.getJulianDayNumber(new GregorianCalendar(2003,1,28));
        assertEquals("Date 28.02.2003 AD is not 2452699.",2452699,result);     
        // 01.03.2003 AD (gregorianisch)
        result = MCRMetaHistoryDate.getJulianDayNumber(new GregorianCalendar(2003,2,0));
        assertEquals("Date 00.03.2003 AD is not 2452699.",2452699,result);     
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setVonDate(GregorianCalendar)'
     */
    public void testSetVonDateGregorianCalendar() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setVonDate(new GregorianCalendar(1964,1,24));
        assertEquals("Von value is not 24.02.1964 AD","24.02.1964 AD",hd.getVonToGregorianString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setVonDate(String)'
     */
    public void testSetVonDateString() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setVonDate("24.02.1964",MCRMetaHistoryDate.TAG_GREGORIAN);
        assertEquals("Von value is not 24.02.1964 AD","24.02.1964 AD",hd.getVonToGregorianString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setBisDate(GregorianCalendar)'
     */
    public void testSetBisDateGregorianCalendar() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setBisDate(new GregorianCalendar(1964,1,24));
        assertEquals("Bis value is not 24.02.1964 AD","24.02.1964 AD",hd.getBisToGregorianString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setBisDate(String)'
     */
    public void testSetBisDateString() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setBisDate("24.02.1964",MCRMetaHistoryDate.TAG_GREGORIAN);
        assertEquals("Bis value is not 24.02.1964 AD","24.02.1964 AD",hd.getBisToGregorianString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.debug()'
     */
    public void testDebug() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setVonDate("05.10.1582",MCRMetaHistoryDate.TAG_GREGORIAN);
        hd.setBisDate("15.10.1582",MCRMetaHistoryDate.TAG_GREGORIAN);
        hd.debug();
    }
}

/**
 * $RCSfile$
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

import java.util.GregorianCalendar;

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
    private static Logger LOGGER;

    protected void setUp() throws Exception {
        super.setUp();
        if (setProperty("MCR.log4j.logger.org.mycore.datamodel.metadata.MCRMetaHistoryDate","INFO", false)){
            //DEBUG will print a Stacktrace if we test for errors, but that's O.K.
            MCRConfiguration.instance().configureLogging();
        }
        if (LOGGER == null) {
            LOGGER = Logger.getLogger(MCRMetaHistoryDateTest.class);
        }
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
     */
    public void testGetHistoryDate() {
    String dstring;
    // 01.01.0001 BC (julianisch)
    GregorianCalendar greg = MCRMetaHistoryDate.getHistoryDate("-01.01.0001",false,"gregorian");
    dstring = MCRMetaHistoryDate.getDateToString(greg);
    assertEquals("Date is not 01.01.0001 BC.","01.01.0001 BC",dstring);            
    // 01.01.0001 BC (julianisch)
    greg = MCRMetaHistoryDate.getHistoryDate("BC01.01.0001",false,"julian");
    dstring = MCRMetaHistoryDate.getDateToString(greg);
    assertEquals("Date is not 01.01.0001 BC.","01.01.0001 BC",dstring); 
    // 01.01.0001 AD (julianisch)
    greg = MCRMetaHistoryDate.getHistoryDate("01.01.0001 AD",false,null);
    dstring = MCRMetaHistoryDate.getDateToString(greg);
    assertEquals("Date is not 01.01.0001 AD.","01.01.0001 AD",dstring);            
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setText(String)'
     */
    public void testSetText() {

    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setDefaultVonBis()'
     */
    public void testSetDefaultVonBis() {

    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setDateVonBis(String)'
     */
    public void testSetDateVonBis() {

    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setVonDate(GregorianCalendar)'
     */
    public void testSetVonDateGregorianCalendar() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setVonDate(new GregorianCalendar(1964,1,24));
        assertEquals("Von value is not 24.02.1964 AD","24.02.1964 AD",hd.getVonToString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setVonDate(String)'
     */
    public void testSetVonDateString() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setVonDate("24.02.1964");
        assertEquals("Von value is not 24.02.1964 AD","24.02.1964 AD",hd.getVonToString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setBisDate(GregorianCalendar)'
     */
    public void testSetBisDateGregorianCalendar() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setBisDate(new GregorianCalendar(1964,1,24));
        assertEquals("Bis value is not 24.02.1964 AD","24.02.1964 AD",hd.getBisToString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setBisDate(String)'
     */
    public void testSetBisDateString() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setBisDate("24.02.1964");
        assertEquals("Bis value is not 24.02.1964 AD","24.02.1964 AD",hd.getBisToString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.getJulianDay(GregorianCalendar)'
     */
    public void testGetJulianDayNumber() {
        int result = 0;
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        GregorianCalendar greg;
        // First day of julianian day number 1.1.4712 BC 
        result = MCRMetaHistoryDate.getJulianDayNumber(hd.getVon());
        assertEquals("Date 01.01.4712 BC is not 0.",0,result);
        // 01.01.4711 BC (julianisch)
        greg = new GregorianCalendar(4711,0,1);
        greg.set(GregorianCalendar.ERA, GregorianCalendar.BC);
        result = MCRMetaHistoryDate.getJulianDayNumber(greg);
        assertEquals("Date 01.01.4711 BC is not 366.",366,result);
        // 31.12.1 BC (julianisch)
        greg = new GregorianCalendar(1,11,31);
        greg.set(GregorianCalendar.ERA, GregorianCalendar.BC);
        result = MCRMetaHistoryDate.getJulianDayNumber(greg);
        assertEquals("Date 31.12.0001 BC is not 1721057.",1721057,result);
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
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.getGregorianCalendar(int)'
     */
    public void testGetGregorianCalendar() {
        GregorianCalendar greg;
        String dstring;
        // negative input
        greg = MCRMetaHistoryDate.getGregorianCalendar(-1);
        dstring = MCRMetaHistoryDate.getDateToString(greg);
        assertEquals("Date 0 is not 01.01.4712 BC.","01.01.4712 BC",dstring);        
        // First day of julianian day number 1.1.4712 BC 
        greg = MCRMetaHistoryDate.getGregorianCalendar(0);
        dstring = MCRMetaHistoryDate.getDateToString(greg);
        assertEquals("Date 0 is not 01.01.4712 BC.","01.01.4712 BC",dstring);        
        // 01.01.4711 BC (julianisch)
        greg = MCRMetaHistoryDate.getGregorianCalendar(366);
        dstring = MCRMetaHistoryDate.getDateToString(greg);
        assertEquals("Date 366 is not 01.01.4711 BC.","01.01.4711 BC",dstring);        
        // 31.12.0001 BC (julianisch)
        greg = MCRMetaHistoryDate.getGregorianCalendar(1721057);
        dstring = MCRMetaHistoryDate.getDateToString(greg);
        assertEquals("Date 1721057 is not 31.12.0001 BC.","31.12.0001 BC",dstring);        
        // 01.01.0001  AD (julianisch)
        greg = MCRMetaHistoryDate.getGregorianCalendar(1721424);
        dstring = MCRMetaHistoryDate.getDateToString(greg);
        assertEquals("Date 1721424 is not 01.01.0001 AD.","01.01.0001 AD",dstring);        
        // 27.01.333 AD (julianisch)
        greg = MCRMetaHistoryDate.getGregorianCalendar(1842713);
        dstring = MCRMetaHistoryDate.getDateToString(greg);
        assertEquals("Date 1842713 is not 27.01.333 AD.","27.01.0333 AD",dstring);        
        // 4.10.1582 AD (julianisch)
        greg = MCRMetaHistoryDate.getGregorianCalendar(2299160);
        dstring = MCRMetaHistoryDate.getDateToString(greg);
        assertEquals("Date 2299160 is not 04.10.1582 AD.","04.10.1582 AD",dstring);        
        // 15.10.1582 AD (gregorianisch)
        greg = MCRMetaHistoryDate.getGregorianCalendar(2299161);
        dstring = MCRMetaHistoryDate.getDateToString(greg);
        assertEquals("Date 2299161 is not 15.10.1582 AD.","15.10.1582 AD",dstring);        
        // 01.01.2000 AD (gregorianisch)
        greg = MCRMetaHistoryDate.getGregorianCalendar(2451545);
        dstring = MCRMetaHistoryDate.getDateToString(greg);
        assertEquals("Date 2451545 is not 01.01.2000 AD.","01.01.2000 AD",dstring);        
    }

}

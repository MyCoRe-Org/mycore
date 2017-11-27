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

package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;
import org.mycore.common.MCRCalendar;
import org.mycore.common.MCRTestCase;

import com.ibm.icu.util.GregorianCalendar;

/**
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMetaHistoryDate.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 *
 */
public class MCRMetaHistoryDateTest extends MCRTestCase {

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setFromDOM(Element)'
     */
    @Test
    @Ignore("not implemented")
    public void testSetFromDOM() {

    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.createXML()'
     */
    @Test
    @Ignore("not implemented")
    public void testCreateXML() {

    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setVonDate(GregorianCalendar)'
     */
    @Test
    public void setVonDateGregorianCalendar() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setVonDate(new GregorianCalendar(1964, 1, 24));
        assertEquals("Von value is not 1964-02-24 AD", "1964-02-24 AD", hd.getVonToString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setVonDate(String)'
     */
    @Test
    public void setVonDateString() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setVonDate("24.02.1964", MCRCalendar.TAG_GREGORIAN);
        assertEquals("Von value is not 1964-02-24 AD", "1964-02-24 AD", hd.getVonToString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setBisDate(GregorianCalendar)'
     */
    @Test
    public void setBisDateGregorianCalendar() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setBisDate(new GregorianCalendar(1964, 1, 24));
        assertEquals("Bis value is not 1964-02-24 AD", "1964-02-24 AD", hd.getBisToString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.setBisDate(String)'
     */
    @Test
    public void setBisDateString() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setBisDate("24.02.1964", MCRCalendar.TAG_GREGORIAN);
        assertEquals("Bis value is not 1964-02-24 AD", "1964-02-24 AD", hd.getBisToString());
    }

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRMetaHistoryDate.debug()'
     */
    @Test
    public void debug() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setVonDate("05.10.1582", MCRCalendar.TAG_GREGORIAN);
        hd.setBisDate("15.10.1582", MCRCalendar.TAG_GREGORIAN);
        hd.debug();
    }
}

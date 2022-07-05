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

import org.jdom2.Element;
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

    /**
     * check set date methods
     */
    @Test
    public void checkSetDateMethods() {
        MCRMetaHistoryDate hd = new MCRMetaHistoryDate();
        hd.setVonDate(new GregorianCalendar(1964, 1, 23));
        assertEquals("Von value is not 1964-02-23 AD", "1964-02-23 AD", hd.getVonToString());

        hd = new MCRMetaHistoryDate();
        hd.setVonDate("23.02.1964", MCRCalendar.TAG_GREGORIAN);
        assertEquals("Von value is not 1964-02-23 AD", "1964-02-23 AD", hd.getVonToString());

        hd = new MCRMetaHistoryDate();
        hd.setBisDate(new GregorianCalendar(1964, 1, 23));
        assertEquals("Bis value is not 1964-02-23 AD", "1964-02-23 AD", hd.getBisToString());

        hd = new MCRMetaHistoryDate();
        hd.setBisDate("23.02.1964", MCRCalendar.TAG_GREGORIAN);
        assertEquals("Bis value is not 1964-02-23 AD", "1964-02-23 AD", hd.getBisToString());
    }

    /**
     * check createXML, setFromDom, equals and clone
     */
    @Test
    public void checkCreateParseEqualsClone() {

        MCRMetaHistoryDate julian_date = new MCRMetaHistoryDate("subtag", "type", 0);
        julian_date.setCalendar(MCRCalendar.TAG_JULIAN);
        julian_date.setVonDate("22.02.1964", julian_date.getCalendar());
        julian_date.setBisDate("22.02.1964", julian_date.getCalendar());
        julian_date.addText("mein Tag", "de");

        MCRMetaHistoryDate gregorian_date = new MCRMetaHistoryDate("subtag", "type", 0);
        gregorian_date.setCalendar(MCRCalendar.TAG_GREGORIAN);
        gregorian_date.setVonDate("06.03.1964", gregorian_date.getCalendar());
        gregorian_date.setBisDate("06.03.1964", gregorian_date.getCalendar());
        gregorian_date.addText("mein Tag", "de");

        Element julian_date_xml = julian_date.createXML();
        Element gregorian_date_xml = gregorian_date.createXML();

        assertEquals(julian_date_xml.getChildText("text"), gregorian_date_xml.getChildText("text"));
        assertEquals(julian_date_xml.getChildText("ivon"), gregorian_date_xml.getChildText("ivon"));
        assertEquals(julian_date_xml.getChildText("ibis"), gregorian_date_xml.getChildText("ibis"));

        assertEquals(julian_date_xml.getChildText("calendar"), MCRCalendar.TAG_JULIAN);
        assertEquals(julian_date_xml.getChildText("von"), "1964-02-22 AD");
        assertEquals(julian_date_xml.getChildText("bis"), "1964-02-22 AD");

        assertEquals(gregorian_date_xml.getChildText("calendar"), MCRCalendar.TAG_GREGORIAN);
        assertEquals(gregorian_date_xml.getChildText("von"), "1964-03-06 AD");
        assertEquals(gregorian_date_xml.getChildText("bis"), "1964-03-06 AD");

        MCRMetaHistoryDate julian_date_read = new MCRMetaHistoryDate();
        julian_date_read.setFromDOM(julian_date_xml);
        MCRMetaHistoryDate gregorian_date_read = new MCRMetaHistoryDate();
        gregorian_date_read.setFromDOM(gregorian_date_xml);
        assertEquals("read objects from XML should be equal", julian_date_read, gregorian_date_read);

        MCRMetaHistoryDate gregorian_date_clone = gregorian_date_read.clone();
        assertEquals("cloned object should be equal with original", gregorian_date_read, gregorian_date_clone);
    }

}

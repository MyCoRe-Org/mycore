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

import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.TestCase;

import org.jdom.Element;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * @version $Revision$ $Date$
 */
public class MCRMetaDateFragmentTest extends TestCase {

    MCRMetaDateFragment df = new MCRMetaDateFragment();

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public final void testSetFromDOM() {
        //TODO setFromDOM() muss implementiert werden.
    }

    public final void testCreateTypedContent() {
        //TODO createTypedContent() muss implementiert werden.
    }

    public final void testCreateTextSearch() {
        //TODO createTextSearch() muss implementiert werden.
    }

    /*
     * Zu testende Klasse für Element createXML()
     */
    public final void testCreateXML() {
        df = new MCRMetaDateFragment();
        String subtag = "df";
        df.setFromDOM(new Element(subtag, MCRMetaDateFragment.getNs()).setAttribute("lang", "de", org.jdom.Namespace.XML_NAMESPACE));
        df.setYear(GregorianCalendar.getInstance().get(Calendar.YEAR));
        df.setMonth(GregorianCalendar.getInstance().get(Calendar.MONTH) + 1);
        df.setDay(GregorianCalendar.getInstance().get(Calendar.DAY_OF_MONTH));
        Element export = df.createXML();
        assertEquals(3, export.getChildren().size());
        df.setDay(MCRMetaDateFragment.defaultValue);
        export = df.createXML();
        assertEquals(2, export.getChildren().size());
        df.setMonth(MCRMetaDateFragment.defaultValue);
        export = df.createXML();
        assertEquals(1, export.getChildren().size());
    }

    public final void testIsValid() {
        df = new MCRMetaDateFragment();
        assertFalse("There is no Initialisation yet, so isValid should return 'false'", df.isValid());
        df.setFromDOM(new Element("df", MCRMetaDateFragment.getNs()).setAttribute("lang", "de", org.jdom.Namespace.XML_NAMESPACE));
        assertFalse("There is no complete Initialisation yet, so isValid should return 'false'", df.isValid());
        df.setYear(GregorianCalendar.getInstance().get(Calendar.YEAR));
        df.setMonth(GregorianCalendar.getInstance().get(Calendar.MONTH));
        df.setDay(GregorianCalendar.getInstance().get(Calendar.DAY_OF_MONTH));
        assertTrue("After initialisiation, isValid should return 'true'", df.isValid());
        df.setMonth(MCRMetaDateFragment.defaultValue);
        assertFalse("There is Month set but a day", df.isValid());
    }

    public final void testFormatInt() {
        String test = "0023";
        String result = df.formatInt(23, 4);
        assertEquals(test, result);
    }

}

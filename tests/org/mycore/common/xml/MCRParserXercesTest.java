/**
 * $RCSfile: MCRParserXercesTest.java,v $
 * $Revision: 1.0 $ $Date: 29.07.2008 07:27:14 $
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
package org.mycore.common.xml;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 *
 */
public class MCRParserXercesTest extends MCRTestCase {
    
    private String xmlResource=null;
    private String xmlResourceInvalid=null;
    private MCRParserXerces parser=null;

    /* (non-Javadoc)
     * @see org.mycore.common.MCRTestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        super.setProperty("MCR.XMLParser.ValidateSchema", "true", false);
        boolean setProperty=super.setProperty("log4j.logger.org.mycore.common.xml.MCRParserXerces", "FATAL", false);
        xmlResource=MCRParserXercesTest.class.getResource("/MCRParserXercesTest-valid.xml").toString();
        xmlResourceInvalid=MCRParserXercesTest.class.getResource("/MCRParserXercesTest-invalid.xml").toString();
        parser=new MCRParserXerces();
        if (setProperty) {
            MCRConfiguration.instance().configureLogging();
        }
    }

    /**
     * Test method for {@link org.mycore.common.xml.MCRParserXerces#parseURI(java.lang.String, boolean)}.
     */
    public void testParseURIStringBoolean() {
        try {
            parser.parseURI(xmlResourceInvalid, true);
            fail("MCRParserXerces accepts invalid XML content when validation is requested");
        } catch (Exception e) {
        }
        parser.parseURI(xmlResourceInvalid, false);
        parser.parseURI(xmlResource, true);
    }

}

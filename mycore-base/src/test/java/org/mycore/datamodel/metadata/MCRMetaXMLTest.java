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

import java.io.IOException;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMetaXML.
 * 
 * @author Thomas Scheffler
 * @version $Revision$ $Date$
 *
 */
public class MCRMetaXMLTest extends MCRTestCase {
    private static Logger LOGGER;

    protected void setUp() throws Exception {
        super.setUp();//org.mycore.datamodel.metadata.MCRMetaXML
        if (setProperty("log4j.logger.org.mycore.datamodel.metadata","INFO", false)){
            //DEBUG will print a Stacktrace if we test for errors, but that's O.K.
            MCRConfiguration.instance().configureLogging();
        }
        if (LOGGER == null) {
            LOGGER = Logger.getLogger(MCRMetaXMLTest.class);
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testXMLRoundrip() {
        MCRMetaXML mXml = new MCRMetaXML("metadata", "def.heading", "complete", 0);
        Element imported=new Element("heading");
        imported.setAttribute("lang", MCRMetaDefault.DEFAULT_LANGUAGE, Namespace.XML_NAMESPACE);
        imported.setAttribute("inherited", "0");
        imported.setAttribute("type", "complete");
        imported.addContent(new Text("This is a "));
        imported.addContent(new Element("span").setText("JUnit"));
        imported.addContent(new Text("test"));
        mXml.setFromDOM(imported);
        Element exported=mXml.createXML();
        if (LOGGER.isDebugEnabled()) {
            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            StringWriter sw = new StringWriter();
            StringWriter sw2 = new StringWriter();
            try {
                xout.output(imported, sw);
                LOGGER.info(sw.toString());
                xout.output(exported, sw2);
                LOGGER.info(sw2.toString());
            } catch (IOException e) {
                LOGGER.warn("Failure printing xml result", e);
            }
        }
        assertTrue(MCRXMLHelper.deepEqual(new Document(imported), new Document(exported)));
    }

}
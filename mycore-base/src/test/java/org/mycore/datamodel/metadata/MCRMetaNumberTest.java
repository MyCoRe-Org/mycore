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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMetaNumber.
 * It tests again the ENGLISH Locale
 * 
 * @author Jens Kupferschmidt
 * @version $Revision: 28698 $ $Date: 2013-12-19 15:22:40 +0100 (Do, 19. Dez 2013) $
 *
 */
public class MCRMetaNumberTest extends MCRTestCase {
    private static Logger LOGGER;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();//org.mycore.datamodel.metadata.MCRMetaXML
        if (LOGGER == null) {
            LOGGER = LogManager.getLogger(MCRMetaNumber.class);
        }
    }

    @Test
    public void numberTransformation() {
        MCRMetaNumber meta_number = new MCRMetaNumber("number", 0, null, null, "0,1");
        String number_string = meta_number.getNumberAsString();
        assertEquals("datamodel", "0.100", number_string);
        meta_number = new MCRMetaNumber("number", 0, null, null, "0.10");
        number_string = meta_number.getNumberAsString();
        assertEquals("datamodel", "0.100", number_string);
        meta_number = new MCRMetaNumber("number", 0, null, null, "12345,6789");
        number_string = meta_number.getNumberAsString();
        assertEquals("datamodel", "12345.679", number_string);
        // geo data
        MCRConfiguration.instance().set("MCR.Metadata.MetaNumber.FractionDigits", 8);
        meta_number = new MCRMetaNumber("number", 0, null, null, "123.45678999");
        number_string = meta_number.getNumberAsString();
        assertEquals("datamodel", "123.45678999", number_string);
        meta_number = new MCRMetaNumber("number", 0, null, null, "-123,45678999");
        number_string = meta_number.getNumberAsString();
        assertEquals("datamodel", "-123.45678999", number_string);
    }

    @Test
    public void xmlRoundrip() throws IOException {
        // test 0.100
        MCRMetaNumber meta_number = new MCRMetaNumber();
        Element imported = new Element("number");
        imported.setAttribute("inherited", "0");
        imported.setAttribute("dimension", "width");
        imported.setAttribute("measurement", "cm");
        imported.addContent(new Text("0.100"));
        meta_number.setFromDOM(imported);
        Element exported = meta_number.createXML();
        print_data(imported, exported);
        check_data(imported, exported);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("log4j.logger.org.mycore.datamodel.metadata", "INFO");
        return testProperties;
    }

    private void print_data(Element imported, Element exported) {
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
    }

    private void check_data(Element imported, Element exported) {
        try {
            assertTrue(MCRXMLHelper.deepEqual(new Document(imported), new Document(exported)));
        } catch (AssertionError e) {
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            try {
                out.output(imported, System.err);
            } catch (IOException e1) {
                LOGGER.error("Can't print imported for Test MCRMetaNumberTest.");
            }
            try {
                out.output(exported, System.err);
            } catch (IOException e1) {
                LOGGER.error("Can't print exported for Test MCRMetaNumberTest.");
            }
            throw e;
        }
    }

}

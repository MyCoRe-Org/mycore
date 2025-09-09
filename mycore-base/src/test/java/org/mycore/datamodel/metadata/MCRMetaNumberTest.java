/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.test.MyCoReTest;

/**
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMetaNumber.
 * It tests again the ENGLISH Locale
 * 
 * @author Jens Kupferschmidt
 *
 */
@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "log4j.logger.org.mycore.datamodel.metadata", string = "INFO")
})
public class MCRMetaNumberTest {

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void numberTransformation() {
        MCRMetaNumber meta_number = new MCRMetaNumber("number", 0, null, null, "0,1");
        String number_string = meta_number.getNumberAsString();
        assertEquals("0.100", number_string, "datamodel");
        meta_number = new MCRMetaNumber("number", 0, null, null, "0.10");
        number_string = meta_number.getNumberAsString();
        assertEquals("0.100", number_string, "datamodel");
        meta_number = new MCRMetaNumber("number", 0, null, null, "12345,6789");
        number_string = meta_number.getNumberAsString();
        assertEquals("12345.679", number_string, "datamodel");
        // geo data
        MCRConfiguration2.set("MCR.Metadata.MetaNumber.FractionDigits", String.valueOf(8));
        meta_number = new MCRMetaNumber("number", 0, null, null, "123.45678999");
        number_string = meta_number.getNumberAsString();
        assertEquals("123.45678999", number_string, "datamodel");
        meta_number = new MCRMetaNumber("number", 0, null, null, "-123,45678999");
        number_string = meta_number.getNumberAsString();
        assertEquals("-123.45678999", number_string, "datamodel");
    }

    @Test
    public void xmlRoundrip() {
        // test 0.100
        MCRMetaNumber meta_number = new MCRMetaNumber();
        Element imported = new Element("number");
        imported.setAttribute("inherited", "0");
        imported.setAttribute("dimension", "width");
        imported.setAttribute("measurement", "cm");
        imported.addContent(new Text("0.100"));
        meta_number.setFromDOM(imported);
        Element exported = meta_number.createXML();
        printData(imported, exported);
        checkData(imported, exported);
    }

    private void printData(Element imported, Element exported) {
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

    private void checkData(Element imported, Element exported) {
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

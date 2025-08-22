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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.test.MyCoReTest;

/**
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMetaXML.
 *
 * @author Thomas Scheffler
 *
 */
@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "log4j.logger.org.mycore.datamodel.metadata", string = "INFO")
})
public class MCRMetaXMLTest {
    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void xmlRoundrip() throws IOException {
        MCRMetaXML mXml = new MCRMetaXML("def.heading", "complete", 0);
        Element imported = new Element("heading");
        imported.setAttribute("lang", MCRMetaDefault.DEFAULT_LANGUAGE, Namespace.XML_NAMESPACE);
        imported.setAttribute("inherited", "0");
        imported.setAttribute("type", "complete");
        imported.addContent(new Text("This is a "));
        imported.addContent(new Element("span").setText("JUnit"));
        imported.addContent(new Text("test"));
        mXml.setFromDOM(imported);
        Element exported = mXml.createXML();
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
        try {
            assertTrue(MCRXMLHelper.deepEqual(new Document(imported), new Document(exported)));
        } catch (AssertionError e) {
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            out.output(imported, System.err);
            out.output(exported, System.err);
            throw e;
        }
    }

}

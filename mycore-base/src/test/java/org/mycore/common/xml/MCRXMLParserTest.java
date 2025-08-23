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

package org.mycore.common.xml;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.content.MCRURLContent;
import org.mycore.test.MyCoReTest;

/**
 * @author Thomas Scheffler (yagee)
 */
@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.XMLParser.ValidateSchema", string = "true"),
    @MCRTestProperty(key = "log4j.logger.org.mycore.common.xml.MCRParserXerces", string = "FATAL")
})
public class MCRXMLParserTest {

    private URL xmlResource;

    private Path xmlFile;

    private URL xmlResourceInvalid;

    private Path xmlFileInvalid;

    @TempDir
    public File testFolder;

    @BeforeEach
    public void setUp() throws Exception {
        xmlResource = MCRXMLParserTest.class.getResource("/MCRParserXercesTest-valid.xml");
        xmlResourceInvalid = MCRXMLParserTest.class.getResource("/MCRParserXercesTest-invalid.xml");
        //MCR-1069: create */../* URI
        new File(testFolder, "foo").mkdir();
        xmlFile = new File(testFolder, "foo/../MCRParserXercesTest-valid.xml").toPath();
        xmlFileInvalid = new File(testFolder, "foo/../MCRParserXercesTest-invalid.xml").toPath();
        Files.copy(xmlResource.openStream(), xmlFile, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(xmlResourceInvalid.openStream(), xmlFileInvalid, StandardCopyOption.REPLACE_EXISTING);
        System.out.println(xmlFile.toUri());
    }

    @Test
    public void testInvalidXML() throws IOException, JDOMException {
        try {
            MCRXMLParserFactory.getValidatingParser().parseXML(new MCRURLContent(xmlResourceInvalid));
            fail("MCRParserXerces accepts invalid XML content when validation is requested");
        } catch (Exception e) {
        }
        MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRURLContent(xmlResourceInvalid));
        MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRURLContent(xmlFileInvalid.toUri().toURL()));
    }

    @Test
    public void testValidXML() throws IOException, JDOMException {
        MCRXMLParserFactory.getValidatingParser().parseXML(new MCRURLContent(xmlResource));
        MCRXMLParserFactory.getValidatingParser().parseXML(new MCRURLContent(xmlFile.toUri().toURL()));
    }

}

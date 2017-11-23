package org.mycore.common.xml;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRURLContent;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRXMLParserTest extends MCRTestCase {

    private URL xmlResource = null;

    private Path xmlFile = null;

    private URL xmlResourceInvalid = null;

    private Path xmlFileInvalid = null;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        xmlResource = MCRXMLParserTest.class.getResource("/MCRParserXercesTest-valid.xml");
        xmlResourceInvalid = MCRXMLParserTest.class.getResource("/MCRParserXercesTest-invalid.xml");
        //MCR-1069: create */../* URI
        testFolder.newFolder("foo");
        xmlFile = new File(testFolder.getRoot(), "foo/../MCRParserXercesTest-valid.xml").toPath();
        xmlFileInvalid = new File(testFolder.getRoot(), "foo/../MCRParserXercesTest-invalid.xml").toPath();
        Files.copy(xmlResource.openStream(), xmlFile, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(xmlResourceInvalid.openStream(), xmlFileInvalid, StandardCopyOption.REPLACE_EXISTING);
        System.out.println(xmlFile.toUri());
    }

    @Test
    public void testInvalidXML() throws SAXParseException, IOException {
        try {
            MCRXMLParserFactory.getValidatingParser().parseXML(new MCRURLContent(xmlResourceInvalid));
            fail("MCRParserXerces accepts invalid XML content when validation is requested");
        } catch (Exception e) {
        }
        MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRURLContent(xmlResourceInvalid));
        MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRURLContent(xmlFileInvalid.toUri().toURL()));
    }

    @Test
    public void testValidXML() throws SAXParseException, IOException {
        MCRXMLParserFactory.getValidatingParser().parseXML(new MCRURLContent(xmlResource));
        MCRXMLParserFactory.getValidatingParser().parseXML(new MCRURLContent(xmlFile.toUri().toURL()));
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.XMLParser.ValidateSchema", "true");
        testProperties.put("log4j.logger.org.mycore.common.xml.MCRParserXerces", "FATAL");
        return testProperties;
    }

}

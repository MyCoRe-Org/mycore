package org.mycore.pi.doi;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfigurationException;
import org.xml.sax.SAXException;

public class MCRDOIServiceTest extends MCRTestCase {

    @Test
    public void testSchemaV3() {
        try {
            loadSchema(MCRDOIService.DATACITE_SCHEMA_V3);
        } catch (SAXException ignored) {
            Assert.fail();
        }
    }

    @Test
    public void testSchemaV4() {
        try {
            loadSchema(MCRDOIService.DATACITE_SCHEMA_V4);
        } catch (SAXException ignored) {
            Assert.fail();
        }
    }

    @Test
    public void testSchemaV41() {
        try {
            loadSchema(MCRDOIService.DATACITE_SCHEMA_V41);
        } catch (SAXException ignored) {
            Assert.fail();
        }
    }

    private void loadSchema(String schemaURL) throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false);

        URL localSchemaURL = MCRClassTools.getClassLoader().getResource(schemaURL);
        if (localSchemaURL == null) {
            throw new MCRConfigurationException(schemaURL + " was not found!");
        }
        schemaFactory.newSchema(localSchemaURL);
    }

}

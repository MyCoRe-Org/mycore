package org.mycore.mods.identifier;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.MCRPIService;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.doi.MCRDOIParser;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.xml.sax.SAXParseException;

public class MCRMODSDOIMetadataServiceTest extends MCRTestCase {

    public static final String TEST_DOI_METADATA_SERVICE_1 = "TestDOI_1";

    public static final String TEST_DOI_METADATA_SERVICE_2 = "TestDOI_2";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testInsert() throws URISyntaxException, SAXParseException, IOException,
        MCRPersistentIdentifierException {
        final MCRMODSDOIMetadataService service1 = new MCRMODSDOIMetadataService(TEST_DOI_METADATA_SERVICE_1);
        final MCRMODSDOIMetadataService service2 = new MCRMODSDOIMetadataService(TEST_DOI_METADATA_SERVICE_2);

        final URL modsURL = MCRMODSDOIMetadataService.class.getClassLoader()
            .getResource("MCRMODSDOIMetadataServiceTest/mods.xml");
        final MCRObject object = new MCRObject(modsURL.toURI());

        final MCRDOIParser parser = new MCRDOIParser();

        final MCRDigitalObjectIdentifier doi1 = parser.parse("10.1/doi").get();
        final MCRDigitalObjectIdentifier doi2 = parser.parse("10.2/doi").get();

        service1.insertIdentifier(doi1, object, "");
        final MCRPersistentIdentifier doi1_read = service1.getIdentifier(object, "").get();
        Assert.assertEquals("The dois should match!", doi1.asString(), doi1_read.asString());

        service2.insertIdentifier(doi2,object,"");
        final MCRPersistentIdentifier doi2_read = service2.getIdentifier(object, "").get();
        Assert.assertEquals("The dois should match!", doi2.asString(), doi2_read.asString());

        final MCRPersistentIdentifier doi1_read_2 = service1.getIdentifier(object, "").get();
        Assert.assertEquals("The dois should match!", doi1.asString(), doi1_read_2.asString());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();

        testProperties
            .put(MCRPIService.METADATA_SERVICE_CONFIG_PREFIX + TEST_DOI_METADATA_SERVICE_1 + ".Type", "doi");
        testProperties
            .put(MCRPIService.METADATA_SERVICE_CONFIG_PREFIX + TEST_DOI_METADATA_SERVICE_1 + ".Prefix", "10.1");

        testProperties
            .put(MCRPIService.METADATA_SERVICE_CONFIG_PREFIX + TEST_DOI_METADATA_SERVICE_2 + ".Type", "doi");
        testProperties
            .put(MCRPIService.METADATA_SERVICE_CONFIG_PREFIX + TEST_DOI_METADATA_SERVICE_2 + ".Prefix", "10.2");

        return testProperties;
    }
}
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

package org.mycore.mods.identifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.pi.MCRPIService;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.doi.MCRDOIParser;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = MCRPIService.METADATA_SERVICE_CONFIG_PREFIX
        + MCRMODSDOIMetadataServiceTest.TEST_DOI_METADATA_SERVICE_1 + ".Type", string = "doi"),
    @MCRTestProperty(key = MCRPIService.METADATA_SERVICE_CONFIG_PREFIX
        + MCRMODSDOIMetadataServiceTest.TEST_DOI_METADATA_SERVICE_1,
        classNameOf = MCRMODSDOIMetadataService.class),
    @MCRTestProperty(key = MCRPIService.METADATA_SERVICE_CONFIG_PREFIX
        + MCRMODSDOIMetadataServiceTest.TEST_DOI_METADATA_SERVICE_1 + ".Prefix", string = "10.1"),
    @MCRTestProperty(key = MCRPIService.METADATA_SERVICE_CONFIG_PREFIX
        + MCRMODSDOIMetadataServiceTest.TEST_DOI_METADATA_SERVICE_2,
        classNameOf = MCRMODSDOIMetadataService.class),
    @MCRTestProperty(key = MCRPIService.METADATA_SERVICE_CONFIG_PREFIX
        + MCRMODSDOIMetadataServiceTest.TEST_DOI_METADATA_SERVICE_2 + ".Type", string = "doi"),
    @MCRTestProperty(key = MCRPIService.METADATA_SERVICE_CONFIG_PREFIX
        + MCRMODSDOIMetadataServiceTest.TEST_DOI_METADATA_SERVICE_2 + ".Prefix", string = "10.2")
})
public class MCRMODSDOIMetadataServiceTest {

    public static final String TEST_DOI_METADATA_SERVICE_1 = "TestDOI_1";

    public static final String TEST_DOI_METADATA_SERVICE_2 = "TestDOI_2";

    @Test
    public void testInsert() throws URISyntaxException, IOException, MCRPersistentIdentifierException, JDOMException {

        final MCRMODSDOIMetadataService service1 = MCRConfiguration2.getInstanceOfOrThrow(
            MCRMODSDOIMetadataService.class,
            MCRPIService.METADATA_SERVICE_CONFIG_PREFIX + TEST_DOI_METADATA_SERVICE_1);
        final MCRMODSDOIMetadataService service2 = MCRConfiguration2.getInstanceOfOrThrow(
            MCRMODSDOIMetadataService.class,
            MCRPIService.METADATA_SERVICE_CONFIG_PREFIX + TEST_DOI_METADATA_SERVICE_2);

        final URL modsURL = MCRMODSDOIMetadataService.class.getClassLoader()
            .getResource("MCRMODSDOIMetadataServiceTest/mods.xml");
        final MCRObject object = new MCRObject(modsURL.toURI());

        final MCRDOIParser parser = new MCRDOIParser();

        final MCRDigitalObjectIdentifier doi1 = parser.parse("10.1/doi").get();
        final MCRDigitalObjectIdentifier doi2 = parser.parse("10.2/doi").get();

        service1.insertIdentifier(doi1, object, "");
        final MCRPersistentIdentifier doi1_read = service1.getIdentifier(object, "").get();
        assertEquals(doi1.asString(), doi1_read.asString(), "The dois should match!");

        service2.insertIdentifier(doi2, object, "");
        final MCRPersistentIdentifier doi2_read = service2.getIdentifier(object, "").get();
        assertEquals(doi2.asString(), doi2_read.asString(), "The dois should match!");

        final MCRPersistentIdentifier doi1_read_2 = service1.getIdentifier(object, "").get();
        assertEquals(doi1.asString(), doi1_read_2.asString(), "The dois should match!");
    }

}

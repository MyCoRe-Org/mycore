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

package org.mycore.pi.doi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mycore.pi.MCRMockMetadataService.TEST_PROPERTY;
import static org.mycore.pi.MCRMockMetadataService.TEST_PROPERTY_VALUE;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRMockIdentifierGenerator;
import org.mycore.pi.MCRMockIdentifierService;
import org.mycore.pi.MCRMockMetadataService;
import org.mycore.pi.MCRPIService;
import org.mycore.pi.MCRPIServiceManager;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.util.MCROtherPIValueExtractor;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class })
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessBaseImpl.class),
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
    @MCRTestProperty(key = "MCR.PI.Service.Mock.Class", classNameOf = MCRMockIdentifierService.class),
    @MCRTestProperty(key = "MCR.PI.Service.Mock.Generator", string = "Mock"),
    @MCRTestProperty(key = "MCR.PI.Service.Mock.MetadataService", string = "Mock"),
    @MCRTestProperty(key = "MCR.PI.Generator.Mock.Class", classNameOf = MCRMockIdentifierGenerator.class),
    @MCRTestProperty(key = "MCR.PI.MetadataService.Mock.Class", classNameOf = MCRMockMetadataService.class),
    @MCRTestProperty(key = "MCR.PI.MetadataService.Mock." + TEST_PROPERTY, string = TEST_PROPERTY_VALUE),
})
public class MCROtherPIDOIGeneratorTest {

    public static final String PREFIX = "10.1234";

    @Test
    public void generate()
        throws MCRPersistentIdentifierException, MCRAccessException, ExecutionException, InterruptedException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRPIServiceManager manager = MCRPIServiceManager.getInstance();
        MCRPIService<MCRPersistentIdentifier> mockService = manager.getRegistrationService("Mock");
        mockService.register(object, "", true);

        MCROtherPIValueExtractor extractor = new MCROtherPIValueExtractor("mock", "Mock", "MOCK:my_test_(.*):");
        MCROtherPIDOIGenerator generator = new MCROtherPIDOIGenerator(new MCRDOIParser(), PREFIX, extractor);
        String doi = generator.generate(object, "").asString();

        assertTrue(doi.startsWith(PREFIX));
        assertEquals('/', doi.charAt(PREFIX.length()));

        String value = doi.substring(PREFIX.length() + 1);

        assertEquals("00000123", value);

    }

}

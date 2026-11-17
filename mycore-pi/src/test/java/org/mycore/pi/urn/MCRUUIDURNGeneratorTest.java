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

package org.mycore.pi.urn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
})
public class MCRUUIDURNGeneratorTest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String NAMESPACE = "urn:nbn:de:gbv:xyz";

    @Test
    public void generateWithChecksum() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRUUIDURNGenerator generator = new MCRUUIDURNGenerator(NAMESPACE, true, "-");

        String urn = generator.generate(object, "").asString();
        LOGGER.info("URN: {}", urn);

        assertTrue(urn.startsWith(NAMESPACE));
        assertEquals('-', urn.charAt(NAMESPACE.length()));
        assertEquals('-', urn.charAt(urn.length() - 2));

        String uuid = urn.substring(NAMESPACE.length() + 1, urn.length() - 2);
        LOGGER.info("UUID: {}", uuid);

        MCRDNBURN referenceUrn = new MCRDNBURN("gbv:xyz", "-" + uuid + "-", true);
        char checksum = Character.forDigit(referenceUrn.calculateChecksum(), 10);
        LOGGER.info("Checksum: {}", checksum);

        try {
            UUID.fromString(uuid);
        } catch (Exception e) {
            fail("NISS is not a valid UUID", e);
        }
        assertEquals(checksum, urn.charAt(urn.length() - 1));

    }

    @Test
    public void generateWithoutChecksum() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRUUIDURNGenerator generator = new MCRUUIDURNGenerator(NAMESPACE, false, ":");

        String urn = generator.generate(object, "").asString();
        LOGGER.info("URN: {}", urn);

        assertTrue(urn.startsWith(NAMESPACE));
        assertEquals(':', urn.charAt(NAMESPACE.length()));

        String uuid = urn.substring(NAMESPACE.length() + 1);
        LOGGER.info("UUID: {}", uuid);

        try {
            UUID.fromString(uuid);
        } catch (Exception e) {
            fail("NISS is not a valid UUID", e);
        }

    }

    @Test
    public void generateMultiple() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRUUIDURNGenerator generator = new MCRUUIDURNGenerator(NAMESPACE, false, "");

        String urn1 = generator.generate(object, "").asString();
        LOGGER.info("URN 1: {}", urn1);

        String urn2 = generator.generate(object, "").asString();
        LOGGER.info("URN 2: {}", urn2);

        String urn3 = generator.generate(object, "").asString();
        LOGGER.info("URN 3: {}", urn3);

        assertNotEquals(urn1, urn2);
        assertNotEquals(urn2, urn3);
        assertNotEquals(urn3, urn1);

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRUUIDURNGenerator.class),
        @MCRTestProperty(key = "Test.Namespace", string = NAMESPACE),
        @MCRTestProperty(key = "Test.Delimiter", string = "-"),
        @MCRTestProperty(key = "Test.AppendChecksum", string = "true")
    })
    public void configurationWithChecksum() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRPIGenerator<?> generator = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, "Test");

        String pi = generator.generate(object, "").asString();
        LOGGER.info("PI: {}", pi);

        assertTrue(pi.startsWith(NAMESPACE));
        assertEquals('-', pi.charAt(NAMESPACE.length()));
        assertEquals('-', pi.charAt(pi.length() - 2));

        String uuid = pi.substring(NAMESPACE.length() + 1, pi.length() - 2);
        LOGGER.info("UUID: {}", uuid);

        MCRDNBURN referenceUrn = new MCRDNBURN("gbv:xyz", "-" + uuid + "-", true);
        char checksum = Character.forDigit(referenceUrn.calculateChecksum(), 10);
        LOGGER.info("Checksum: {}", checksum);

        try {
            UUID.fromString(uuid);
        } catch (Exception e) {
            fail("NISS is not a valid UUID", e);
        }
        assertEquals(checksum, pi.charAt(pi.length() - 1));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRUUIDURNGenerator.class),
        @MCRTestProperty(key = "Test.Namespace", string = NAMESPACE),
        @MCRTestProperty(key = "Test.Delimiter", string = ":"),
        @MCRTestProperty(key = "Test.AppendChecksum", string = "false")
    })
    public void configurationWithoutChecksum() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRPIGenerator<?> generator = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, "Test");

        String pi = generator.generate(object, "").asString();
        LOGGER.info("PI: {}", pi);

        assertTrue(pi.startsWith(NAMESPACE));
        assertEquals(':', pi.charAt(NAMESPACE.length()));

        String uuid = pi.substring(NAMESPACE.length() + 1);
        LOGGER.info("UUID: {}", uuid);

        try {
            UUID.fromString(uuid);
        } catch (Exception e) {
            fail("NISS is not a valid UUID", e);
        }

    }

}

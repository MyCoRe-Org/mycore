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

import org.junit.jupiter.api.Test;
import org.mycore.access.MCRAccessBaseImpl;
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
    @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessBaseImpl.class),
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
})
public class MCRFLURNGeneratorTest {

    public static final String NAMESPACE = "urn:nbn:de:gbv:xyz";

    @Test
    public void generate() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRFLURNGenerator generator = new MCRFLURNGenerator(NAMESPACE, "-");

        String urn = generator.generate(object, "").asString();

        assertTrue(urn.startsWith(NAMESPACE));
        assertEquals('-', urn.charAt(NAMESPACE.length()));
        assertEquals('-', urn.charAt(urn.length() - 2));

        String value = urn.substring(NAMESPACE.length() + 1, urn.length() - 2);
        char checksum = Character.forDigit(new MCRDNBURN("gbv:xyz", "-" + value + "-").calculateChecksum(), 10);

        assertEquals(checksum, urn.charAt(urn.length() - 1));

    }

    @Test
    public void generateMultiple() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRFLURNGenerator generator = new MCRFLURNGenerator(NAMESPACE, "-");

        String urn1 = generator.generate(object, "").asString();
        String urn2 = generator.generate(object, "").asString();
        String urn3 = generator.generate(object, "").asString();

        assertNotEquals(urn1, urn2);
        assertNotEquals(urn2, urn3);
        assertNotEquals(urn3, urn1);

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRFLURNGenerator.class),
        @MCRTestProperty(key = "Test.Namespace", string = NAMESPACE),
        @MCRTestProperty(key = "Test.Delimiter", string = "-")
    })
    public void configuration() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRPIGenerator<?> generator = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, "Test");

        String pi = generator.generate(object, "").asString();

        assertTrue(pi.startsWith(NAMESPACE));
        assertEquals('-', pi.charAt(NAMESPACE.length()));
        assertEquals('-', pi.charAt(pi.length() - 2));

        String value = pi.substring(NAMESPACE.length() + 1, pi.length() - 2);
        char checksum = Character.forDigit(new MCRDNBURN("gbv:xyz", "-" + value + "-").calculateChecksum(), 10);

        assertEquals(checksum, pi.charAt(pi.length() - 1));

    }

}

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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
})
public class MCRUUIDDOIGeneratorTest {

    public static final String PREFIX = "10.1234";

    @Test
    public void generate() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRUUIDDOIGenerator generator = new MCRUUIDDOIGenerator(new MCRDOIParser(), PREFIX);
        String doi = generator.generate(object, "").asString();

        assertTrue(doi.startsWith(PREFIX));
        assertEquals('/', doi.charAt(PREFIX.length()));

        String uuid = doi.substring(PREFIX.length() + 1);

        try {
            UUID.fromString(uuid);
        } catch (Exception e) {
            fail("NNIS is not a valid UUID", e);
        }

    }

    @Test
    public void generateMultiple() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRUUIDDOIGenerator generator = new MCRUUIDDOIGenerator(new MCRDOIParser(), PREFIX);
        String doi1 = generator.generate(object, "").asString();
        String doi2 = generator.generate(object, "").asString();
        String doi3 = generator.generate(object, "").asString();

        assertNotEquals(doi1, doi2);
        assertNotEquals(doi2, doi3);
        assertNotEquals(doi3, doi1);

    }

}

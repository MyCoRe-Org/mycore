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

package org.mycore.pi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Locale;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class })
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
    @MCRTestProperty(key = "MCR.DOI.Prefix", string = MCRDefaultPIGeneratorsTest.DOI_PREFIX)
})
public class MCRDefaultPIGeneratorsTest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String DATE_FORMAT = "yyyyMMdd-HHmmss";

    public static final String DOI_PREFIX = MCRDigitalObjectIdentifier.TEST_DOI_PREFIX;

    @Test
    public void uuidDoiConfiguration() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        String property = "MCR.PI.Generator.UUIDDOI";
        MCRPIGenerator<?> generator = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, property);

        String pi = generator.generate(object, "").asString();
        LOGGER.info("PI: {}", pi);

        assertTrue(pi.startsWith(DOI_PREFIX + "/"));
        try {
            UUID.fromString(pi.substring(DOI_PREFIX.length() + 1));
        } catch (Exception e) {
            fail("Suffix is not a valid UUID", e);
        }

    }

    @Test
    public void dateDoiConfiguration() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        String property = "MCR.PI.Generator.DateDOI";
        MCRPIGenerator<?> generator = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, property);

        String pi = generator.generate(object, "").asString();
        LOGGER.info("PI: {}", pi);

        assertTrue(pi.startsWith(DOI_PREFIX + "/"));
        assertEquals(formatCreateDate(object) + "-0", pi.substring(DOI_PREFIX.length() + 1));

    }

    private String formatCreateDate(MCRObject object) {

        MCRISO8601Date isoDate = new MCRISO8601Date();
        isoDate.setDate(object.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE));

        return isoDate.format(DATE_FORMAT, Locale.ROOT);

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.PI.Generator.MapObjectIDDOI.Prefix.my_test", string = DOI_PREFIX)
    })
    public void mapObjectIdConfiguration() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        String property = "MCR.PI.Generator.MapObjectIDDOI";
        MCRPIGenerator<?> generator = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, property);

        String pi = generator.generate(object, "").asString();
        LOGGER.info("PI: {}", pi);

        assertTrue(pi.startsWith(DOI_PREFIX + "/"));
        assertEquals("123", pi.substring(DOI_PREFIX.length() + 1));

    }

}

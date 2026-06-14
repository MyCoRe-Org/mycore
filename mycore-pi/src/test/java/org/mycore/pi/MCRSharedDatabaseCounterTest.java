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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.date.MCRSimpleDateFormatter;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.doi.MCRCreateDateDOIGenerator;
import org.mycore.pi.doi.MCRDOIParser;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.util.MCRPIGeneratorUtils;
import org.mycore.pi.util.MCRPIGeneratorUtils.CachingDatabaseCounter;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRSharedDatabaseCounterTest {

    public static final String DATE_FORMAT = "ddMMyyyy";

    public static final String PREFIX = "10.1234";

    /**
     * Tests that two otherwise independent PI generators that are configured to produce
     * similar PIs and are using the shared {@link CachingDatabaseCounter}
     * do indeed produce different PIs that only differ in the applied count value.
     */
    @Test
    public void test() throws MCRPersistentIdentifierException {

        MCRObject object1 = new MCRObject();
        object1.setSchema("http://www.w3.org/2001/XMLSchema");
        object1.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRCreateDateDOIGenerator generator1 = new MCRCreateDateDOIGenerator(
            new MCRDOIParser(),
            MCRPIGeneratorUtils.SHARED_COUNTER,
            DATE_FORMAT,
            PREFIX,
            3);

        String doi1 = generator1.generate(object1, "").asString();

        MCRObject object2 = new MCRObject();
        object2.setSchema("http://www.w3.org/2001/XMLSchema");
        object2.setId(MCRObjectID.getInstance("my_test_00000234"));

        MCRGenericPIGenerator generator2 = new MCRGenericPIGenerator(
            MCRPIGeneratorUtils.SHARED_COUNTER,
            PREFIX + "/$ObjectDate-$Count",
            new MCRSimpleDateFormatter(DATE_FORMAT),
            Map.of(),
            Map.of(),
            3,
            MCRDigitalObjectIdentifier.TYPE,
            List.of());

        String doi2 = generator2.generate(object2, "").asString();

        assertEquals(doi1.length(), doi2.length());
        assertEquals(doi1.substring(0, doi1.indexOf("-")), doi2.substring(0, doi2.indexOf("-")));
        assertNotEquals(doi1, doi2);

    }

}

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

import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRMockCounter;
import org.mycore.pi.MCRPIGenerator;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class })
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
})
public class MCRCreateDateDOIGeneratorTest {

    public static final String DATE_FORMAT = "yyyyMMdd-HHmmss";

    public static final String PREFIX = "10.1234";

    @Test
    public void generate() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRCreateDateDOIGenerator generator = new MCRCreateDateDOIGenerator(
            new MCRMockCounter(42), DATE_FORMAT, PREFIX, 3);

        String doi = generator.generate(object, "").asString();

        assertTrue(doi.startsWith(PREFIX));
        assertEquals('/', doi.charAt(PREFIX.length()));

        String value = doi.substring(PREFIX.length() + 1);

        assertTrue(value.startsWith(formatDate(new Date()) + "-"));
        assertTrue(value.endsWith("-042"));

    }

    private String formatDate(Date date) {

        MCRISO8601Date isoDate = new MCRISO8601Date();
        isoDate.setDate(date);

        return isoDate.format(DATE_FORMAT, Locale.ROOT);

    }

    @Test
    public void generateMultiple() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRCreateDateDOIGenerator generator = new MCRCreateDateDOIGenerator(
            new MCRMockCounter(42), DATE_FORMAT, PREFIX, -1);

        String doi1 = generator.generate(object, "").asString();
        String doi2 = generator.generate(object, "").asString();
        String doi3 = generator.generate(object, "").asString();

        assertNotEquals(doi1, doi2);
        assertNotEquals(doi2, doi3);
        assertNotEquals(doi3, doi1);

        assertTrue(doi1.endsWith("-42"));
        assertTrue(doi2.endsWith("-43"));
        assertTrue(doi3.endsWith("-44"));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRCreateDateDOIGenerator.class),
        @MCRTestProperty(key = "Test.DateFormat", string = DATE_FORMAT),
        @MCRTestProperty(key = "Test.Prefix", string = PREFIX),
        @MCRTestProperty(key = "Test.CountPrecision", string = "3"),
    })
    public void configuration() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRPIGenerator<?> generator = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, "Test");

        String pi = generator.generate(object, "").asString();

        assertTrue(pi.startsWith(PREFIX));
        assertEquals('/', pi.charAt(PREFIX.length()));

        String value = pi.substring(PREFIX.length() + 1);

        assertTrue(value.startsWith(formatDate(new Date()) + "-"));
        assertTrue(value.endsWith("-000"));

    }

}

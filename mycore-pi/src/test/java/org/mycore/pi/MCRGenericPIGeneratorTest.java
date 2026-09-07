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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.doi.MCRDigitalObjectIdentifier;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true")
})
public class MCRGenericPIGeneratorTest {

    public static final String DATE_FORMAT = "ddMMyyyy";

    public static final String GENERAL_PATTERN =
        "urn:nbn:de:gbv:xyz:$CurrentDate-$1-$2-$ObjectType-$ObjectProject-$ObjectNumber-$Count-";

    public static final String XPATH_1 = "/mycoreobject/metadata/test1/test2/text()";

    public static final String XPATH_2 = "/mycoreobject/metadata/test1/test3/text()";

    @Test
    public void testGenerate() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        Element testElement = new Element("test1");
        testElement.setAttribute("class", "MCRMetaXML");
        testElement.addContent(new Element("test2").setText("result1"));
        testElement.addContent(new Element("test3").setText("result2"));

        Element metadata = new Element("metadata");
        metadata.addContent(testElement);

        object.getMetadata().setFromDOM(metadata);

        MCRGenericPIGenerator generator = new MCRGenericPIGenerator(
            new MCRMockCounter(42),
            GENERAL_PATTERN,
            DATE_FORMAT,
            Map.of("my", "MY"),
            Map.of("test", "TEST"),
            3,
            MCRDNBURN.TYPE,
            List.of(XPATH_1, XPATH_2));

        String pi = generator.generate(object, "").asString();

        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.ROOT);
        String currenDate = dateFormatter.format(new Date());

        assertEquals("urn:nbn:de:gbv:xyz:" + currenDate + "-result1-result2-TEST-MY-00000123-042-",
            pi.substring(0, pi.length() - 1));

    }

    @Test
    public void generateMultiple() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        MCRGenericPIGenerator generator = new MCRGenericPIGenerator(
            new MCRMockCounter(42),
            "10.1234/$ObjectType-$Count",
            DATE_FORMAT,
            Map.of(),
            Map.of(),
            -1,
            MCRDigitalObjectIdentifier.TYPE,
            List.of());

        String doi1 = generator.generate(object, "").asString();
        String doi2 = generator.generate(object, "").asString();
        String doi3 = generator.generate(object, "").asString();

        assertNotEquals(doi1, doi2);
        assertNotEquals(doi2, doi3);
        assertNotEquals(doi3, doi1);

        assertTrue(doi1.startsWith("10.1234/test-"));
        assertTrue(doi2.startsWith("10.1234/test-"));
        assertTrue(doi3.startsWith("10.1234/test-"));

        assertTrue(doi1.endsWith("-42"));
        assertTrue(doi2.endsWith("-43"));
        assertTrue(doi3.endsWith("-44"));

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Test.Class", classNameOf = MCRGenericPIGenerator.class),
        @MCRTestProperty(key = "Test.GeneralPattern", string = GENERAL_PATTERN),
        @MCRTestProperty(key = "Test.DateFormat", string = DATE_FORMAT),
        @MCRTestProperty(key = "Test.ObjectProjectMapping.my", string = "MY"),
        @MCRTestProperty(key = "Test.ObjectTypeMapping.test", string = "TEST"),
        @MCRTestProperty(key = "Test.CountPrecision", string = "3"),
        @MCRTestProperty(key = "Test.Type", string = "dnbUrn"),
        @MCRTestProperty(key = "Test.XPath.1", string = XPATH_1),
        @MCRTestProperty(key = "Test.XPath.2", string = XPATH_2),
    })
    public void configuration() throws MCRPersistentIdentifierException {

        MCRObject object = new MCRObject();
        object.setSchema("http://www.w3.org/2001/XMLSchema");
        object.setId(MCRObjectID.getInstance("my_test_00000123"));

        Element testElement = new Element("test1");
        testElement.setAttribute("class", "MCRMetaXML");
        testElement.addContent(new Element("test2").setText("result1"));
        testElement.addContent(new Element("test3").setText("result2"));

        Element metadata = new Element("metadata");
        metadata.addContent(testElement);

        object.getMetadata().setFromDOM(metadata);

        MCRPIGenerator<?> generator = MCRConfiguration2.getInstanceOfOrThrow(MCRPIGenerator.class, "Test");

        String pi = generator.generate(object, "").asString();

        SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.ROOT);
        String currenDate = dateFormatter.format(new Date());

        assertEquals("urn:nbn:de:gbv:xyz:" + currenDate + "-result1-result2-TEST-MY-00000123-000-",
            pi.substring(0, pi.length() - 1));

    }

}

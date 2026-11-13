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

package org.mycore.common.xsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.util.MCRTestCaseXSLTUtil;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRPropertyFunctionsTests {

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.PropertyTest.Present", string = "present"),
        @MCRTestProperty(key = "MCR.PropertyTest.False", string = "FaLsE"),
        @MCRTestProperty(key = "MCR.PropertyTest.True", string = "TruE"),
        @MCRTestProperty(key = "MCR.PropertyTest.Sub.Foo", string = "foo"),
        @MCRTestProperty(key = "MCR.PropertyTest.Sub.Bar", string = "bar"),
        @MCRTestProperty(key = "MCR.PropertyTest.Sub.Baz", string = "baz")
    })
    public void test() throws TransformerException {
        Map<String, Object> parameters = new HashMap<>();
        String style = "/xslt/functions/property-test.xsl";
        Element results = MCRTestCaseXSLTUtil.transform(style, parameters).getRootElement();

        LOGGER.info(new XMLOutputter(Format.getPrettyFormat()).outputString(results));

        assertEquals("", results.getChildTextNormalize("get-absent"));
        assertEquals("fallback", results.getChildTextNormalize("get-absent-with-fallback"));

        assertEquals("present", results.getChildTextNormalize("get-present"));
        assertEquals("present", results.getChildTextNormalize("get-present-with-fallback"));

        assertEquals("false", results.getChildTextNormalize("get-boolean-absent"));
        assertEquals("false", results.getChildTextNormalize("get-boolean-present"));
        assertEquals("false", results.getChildTextNormalize("get-boolean-false"));
        assertEquals("true", results.getChildTextNormalize("get-boolean-true"));

        Element sub = results.getChild("sub");
        assertEquals(3, sub.getChildren().size());

        Set<String> values = new HashSet<>(List.of("foo", "bar", "baz"));
        for (Element item : sub.getChildren()) {

            // value is one of the values
            String value = item.getAttributeValue("value");
            assertTrue(values.remove(value));

            // correct key for the value
            String key = item.getAttributeValue("key");
            value = Character.toUpperCase(value.charAt(0)) + value.substring(1);
            assertEquals("MCR.PropertyTest.Sub." + value, key);

        }

    }
}

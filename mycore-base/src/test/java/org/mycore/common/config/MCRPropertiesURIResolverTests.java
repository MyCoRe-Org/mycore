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

package org.mycore.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.xsl.uriresolver.MCRURIResolver;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRPropertiesURIResolverTests {

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.PropertyTest.Present", string = "present"),
        @MCRTestProperty(key = "MCR.PropertyTest.Sub.Foo", string = "foo"),
        @MCRTestProperty(key = "MCR.PropertyTest.Sub.Bar", string = "bar"),
        @MCRTestProperty(key = "MCR.PropertyTest.Sub.Baz", string = "baz")
    })
    public void test() throws TransformerException {

        Element absentEntryElement = MCRURIResolver.obtainInstance().resolve("property:MCR.PropertyTest.Absent");
        LOGGER.info("Absent\n{}", new XMLOutputter(Format.getPrettyFormat()).outputString(absentEntryElement));

        assertEquals("entry", absentEntryElement.getName());
        assertTrue(absentEntryElement.getContent().isEmpty());

        Element presentEntryElement = MCRURIResolver.obtainInstance().resolve("property:MCR.PropertyTest.Present");
        LOGGER.info("Present\n{}", new XMLOutputter(Format.getPrettyFormat()).outputString(presentEntryElement));

        assertEquals("entry", presentEntryElement.getName());
        assertFalse(presentEntryElement.getContent().isEmpty());
        assertEquals(1, presentEntryElement.getContent().size());
        assertEquals(Text.class, presentEntryElement.getContent().getFirst().getClass());
        assertEquals("present", ((Text) presentEntryElement.getContent().getFirst()).getText());

        Element subPropertiesElement = MCRURIResolver.obtainInstance().resolve("property:MCR.PropertyTest.Sub*");
        LOGGER.info("Sub*\n{}", new XMLOutputter(Format.getPrettyFormat()).outputString(subPropertiesElement));

        assertEquals("properties", subPropertiesElement.getName());
        assertFalse(subPropertiesElement.getContent().isEmpty());
        assertEquals(3, subPropertiesElement.getContent().size());

        Set<String> values = new HashSet<>(List.of("foo", "bar", "baz"));
        for (Content entryContent : subPropertiesElement.getContent()) {

            assertEquals(Element.class, entryContent.getClass());
            Element entryElement = (Element) entryContent;

            assertEquals("present", ((Text) presentEntryElement.getContent().getFirst()).getText());

            // value is one of the values
            String value = entryElement.getText();
            assertTrue(values.remove(value));

            // correct key for the value
            String key = entryElement.getAttributeValue("key");
            value = Character.toUpperCase(value.charAt(0)) + value.substring(1);
            assertEquals("MCR.PropertyTest.Sub." + value, key);

        }

    }

}

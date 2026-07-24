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
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.ofClass;
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.ofName;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRInstanceConfigurationTest {

    private static final String TEST_CLASS = TestClass.class.getName();

    @Test
    public void configuration() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", TEST_CLASS);

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar", properties);

        assertEquals("Foo.Bar.Class", configuration.name().actual());
        assertEquals("Foo.Bar", configuration.name().canonical());
        assertEquals(TestClass.class, configuration.valueClass());
        assertEquals(properties, configuration.fullProperties());

    }

    @Test
    public void configurationMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", TEST_CLASS);
        properties.put("Foo.Bar.Key1", "Value1");
        properties.put("Foo.Bar.Key2", "Value2");

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar", properties);

        assertEquals("Value1", configuration.properties().get("Key1"));
        assertEquals("Value2", configuration.properties().get("Key2"));
        assertEquals(2, configuration.properties().size());

    }

    @Test
    public void directConfiguration() {

        Map<String, String> properties = new HashMap<>();

        MCRInstanceConfiguration<?> configuration =
            ofClass(Object.class, TestClass.class, "Instance", properties);

        assertEquals("Instance.Class", configuration.name().actual());
        assertEquals("Instance", configuration.name().canonical());
        assertEquals(TestClass.class, configuration.valueClass());
        assertEquals(properties, configuration.fullProperties());

    }

    @Test
    public void directConfigurationRemovesClassEntry() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Instance.Class", "ClassValue");
        properties.put("Instance.class", "ClassValue");
        properties.put("Instance", "ClassValue");

        MCRInstanceConfiguration<?> configuration =
            ofClass(Object.class, TestClass.class, "Instance", properties);

        assertFalse(configuration.properties().containsKey("Class"));
        assertEquals("ClassValue", configuration.properties().get("class"));
        assertEquals("ClassValue", configuration.properties().get(""));
        assertEquals(2, configuration.properties().size());

    }

    public static class TestClass {
    }

}

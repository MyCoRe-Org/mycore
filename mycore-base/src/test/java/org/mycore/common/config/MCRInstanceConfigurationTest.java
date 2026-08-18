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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRInstanceConfigurationTest {

    private static final String TEST_CLASS = TestClass.class.getName();

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo.Bar.Class", classNameOf = TestClass.class)
    })
    public void configuration() {

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar");

        assertEquals("Foo.Bar.Class", configuration.name().actual());
        assertEquals("Foo.Bar", configuration.name().canonical());
        assertEquals(TestClass.class, configuration.valueClass());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo.Bar.Class", classNameOf = TestClass.class),
        @MCRTestProperty(key = "Foo.Bar.Key1", string = "Value1"),
        @MCRTestProperty(key = "Foo.Bar.Key2", string = "Value2")
    })
    public void configurationMovesEntries() {

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar");

        assertEquals("Value1", configuration.properties().get("Key1"));
        assertEquals("Value2", configuration.properties().get("Key2"));
        assertEquals(2, configuration.properties().size());

    }

    @Test
    public void directConfiguration() {

        MCRInstanceConfiguration<?> configuration = ofClass(Object.class, TestClass.class, "Instance");

        assertEquals("Instance.Class", configuration.name().actual());
        assertEquals("Instance", configuration.name().canonical());
        assertEquals(TestClass.class, configuration.valueClass());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Instance.Class", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.class", string = "ClassValue"),
        @MCRTestProperty(key = "Instance", string = "ClassValue")
    })
    public void directConfigurationRemovesClassEntry() {

        MCRInstanceConfiguration<?> configuration =
            ofClass(Object.class, TestClass.class, "Instance");

        assertFalse(configuration.properties().containsKey("Class"));
        assertEquals("ClassValue", configuration.properties().get("class"));
        assertEquals("ClassValue", configuration.properties().get(""));
        assertEquals(2, configuration.properties().size());

    }

    public static class TestClass {
    }

}

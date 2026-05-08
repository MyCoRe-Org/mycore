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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRInstanceConfigurationTest {

    @Test
    public void configurationWithoutSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);

        assertEquals("Foo.Bar", configuration.name().actual());
        assertEquals("Foo.Bar", configuration.name().canonical());
        assertEquals("foo.bar.TestClass", configuration.className());
        assertEquals(properties, configuration.fullProperties());

    }

    @Test
    public void configurationWithoutSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.Key1", "Value1");
        properties.put("Foo.Bar.Key2", "Value2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);

        assertEquals("Value1", configuration.properties().get("Key1"));
        assertEquals("Value2", configuration.properties().get("Key2"));
        assertEquals(2, configuration.properties().size());

    }

    @Test
    public void configurationWithoutSuffixKeepsClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.Class", "ClassValue");
        properties.put("Foo.Bar.class", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);

        assertFalse(configuration.properties().containsKey(""));
        assertEquals("ClassValue", configuration.properties().get("Class"));
        assertEquals("ClassValue", configuration.properties().get("class"));
        assertEquals(2, configuration.properties().size());

    }

    @Test
    public void configurationWithUpperCaseSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);

        assertEquals("Foo.Bar.Class", configuration.name().actual());
        assertEquals("Foo.Bar", configuration.name().canonical());
        assertEquals("foo.bar.TestClass", configuration.className());
        assertEquals(properties, configuration.fullProperties());

    }

    @Test
    public void configurationWithUpperCaseSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Key1", "Value1");
        properties.put("Foo.Bar.Key2", "Value2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);

        assertEquals("Value1", configuration.properties().get("Key1"));
        assertEquals("Value2", configuration.properties().get("Key2"));
        assertEquals(2, configuration.properties().size());

    }

    @Test
    public void configurationWithUpperCaseSuffixRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.class", "ClassValue");
        properties.put("Foo.Bar", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);

        assertFalse(configuration.properties().containsKey(""));
        assertFalse(configuration.properties().containsKey("Class"));
        assertFalse(configuration.properties().containsKey("class"));
        assertEquals(0, configuration.properties().size());

    }

    @Test
    public void configurationWithLowerCaseSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.class", properties);

        assertEquals("Foo.Bar.class", configuration.name().actual());
        assertEquals("Foo.Bar", configuration.name().canonical());
        assertEquals("foo.bar.TestClass", configuration.className());
        assertEquals(properties, configuration.fullProperties());

    }

    @Test
    public void configurationWithLowerCaseSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Key1", "Value1");
        properties.put("Foo.Bar.Key2", "Value2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.class", properties);

        assertEquals("Value1", configuration.properties().get("Key1"));
        assertEquals("Value2", configuration.properties().get("Key2"));
        assertEquals(2, configuration.properties().size());

    }

    @Test
    public void configurationWithLowerCaseSuffixRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Class", "ClassValue");
        properties.put("Foo.Bar", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.class", properties);

        assertFalse(configuration.properties().containsKey(""));
        assertFalse(configuration.properties().containsKey("Class"));
        assertFalse(configuration.properties().containsKey("class"));
        assertEquals(0, configuration.properties().size());

    }

    @Test
    public void nestedConfigurationWithoutSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz", "foo.bar.NestedTestClass");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);
        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration("Baz");

        assertEquals("Foo.Bar.Baz", nestedConfiguration.name().actual());
        assertEquals("Foo.Bar.Baz", nestedConfiguration.name().canonical());
        assertEquals("foo.bar.NestedTestClass", nestedConfiguration.className());
        assertEquals(properties, nestedConfiguration.fullProperties());

    }

    @Test
    public void nestedConfigurationWithoutSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz", "foo.bar.NestedTestClass");
        properties.put("Foo.Bar.Baz.Key1", "Value1");
        properties.put("Foo.Bar.Baz.Key2", "Value2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);
        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration("Baz");

        assertEquals("Value1", nestedConfiguration.properties().get("Key1"));
        assertEquals("Value2", nestedConfiguration.properties().get("Key2"));
        assertEquals(2, nestedConfiguration.properties().size());

    }

    @Test
    public void nestedConfigurationWithoutSuffixKeepsClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz", "foo.bar.NestedTestClass");
        properties.put("Foo.Bar.Baz.Class", "ClassValue");
        properties.put("Foo.Bar.Baz.class", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);
        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration("Baz");

        assertFalse(nestedConfiguration.properties().containsKey(""));
        assertEquals("ClassValue", nestedConfiguration.properties().get("Class"));
        assertEquals("ClassValue", nestedConfiguration.properties().get("class"));
        assertEquals(2, nestedConfiguration.properties().size());

    }

    @Test
    public void nestedConfigurationWithUpperCaseSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.Class", "foo.bar.NestedTestClass");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration("Baz");

        assertEquals("Foo.Bar.Baz.Class", nestedConfiguration.name().actual());
        assertEquals("Foo.Bar.Baz", nestedConfiguration.name().canonical());
        assertEquals("foo.bar.NestedTestClass", nestedConfiguration.className());
        assertEquals(properties, nestedConfiguration.fullProperties());

    }

    @Test
    public void nestedConfigurationWithUpperCaseSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.Class", "foo.bar.NestedTestClass");
        properties.put("Foo.Bar.Baz.Key1", "Value1");
        properties.put("Foo.Bar.Baz.Key2", "Value2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration("Baz");

        assertEquals("Value1", nestedConfiguration.properties().get("Key1"));
        assertEquals("Value2", nestedConfiguration.properties().get("Key2"));
        assertEquals(2, nestedConfiguration.properties().size());

    }

    @Test
    public void nestedConfigurationWithUpperCaseSuffixRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.Class", "foo.bar.NestedTestClass");
        properties.put("Foo.Bar.Baz.class", "ClassValue");
        properties.put("Foo.Bar.Baz", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration("Baz");

        assertFalse(nestedConfiguration.properties().containsKey(""));
        assertFalse(nestedConfiguration.properties().containsKey("Class"));
        assertFalse(nestedConfiguration.properties().containsKey("class"));
        assertEquals(0, nestedConfiguration.properties().size());

    }

    @Test
    public void nestedConfigurationWithLowerCaseSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.class", "foo.bar.NestedTestClass");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.class", properties);
        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration("Baz");

        assertEquals("Foo.Bar.Baz.class", nestedConfiguration.name().actual());
        assertEquals("Foo.Bar.Baz", nestedConfiguration.name().canonical());
        assertEquals("foo.bar.NestedTestClass", nestedConfiguration.className());
        assertEquals(properties, nestedConfiguration.fullProperties());

    }

    @Test
    public void nestedConfigurationWithLowerCaseSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.class", "foo.bar.NestedTestClass");
        properties.put("Foo.Bar.Baz.Key1", "Value1");
        properties.put("Foo.Bar.Baz.Key2", "Value2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.class", properties);
        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration("Baz");

        assertEquals("Value1", nestedConfiguration.properties().get("Key1"));
        assertEquals("Value2", nestedConfiguration.properties().get("Key2"));
        assertEquals(2, nestedConfiguration.properties().size());

    }

    @Test
    public void nestedConfigurationWithLowerCaseSuffixRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.class", "foo.bar.NestedTestClass");
        properties.put("Foo.Bar.Baz.Class", "ClassValue");
        properties.put("Foo.Bar.Baz", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.class", properties);
        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration("Baz");

        assertFalse(nestedConfiguration.properties().containsKey(""));
        assertFalse(nestedConfiguration.properties().containsKey("Class"));
        assertFalse(nestedConfiguration.properties().containsKey("class"));
        assertEquals(0, nestedConfiguration.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithoutSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.A", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.B", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.C", "foo.bar.NestedTestClassC");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("Foo.Bar.A", nestedConfigurationA.name().actual());
        assertEquals("Foo.Bar.A", nestedConfigurationA.name().canonical());
        assertEquals("foo.bar.NestedTestClassA", nestedConfigurationA.className());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Foo.Bar.B", nestedConfigurationB.name().actual());
        assertEquals("Foo.Bar.B", nestedConfigurationB.name().canonical());
        assertEquals("foo.bar.NestedTestClassB", nestedConfigurationB.className());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Foo.Bar.C", nestedConfigurationC.name().actual());
        assertEquals("Foo.Bar.C", nestedConfigurationC.name().canonical());
        assertEquals("foo.bar.NestedTestClassC", nestedConfigurationC.className());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedConfigurationMapWithoutSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.A", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.A.Key1", "ValueA1");
        properties.put("Foo.Bar.A.Key2", "ValueA2");
        properties.put("Foo.Bar.B", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.B.Key1", "ValueB1");
        properties.put("Foo.Bar.B.Key2", "ValueB2");
        properties.put("Foo.Bar.C", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.C.Key1", "ValueC1");
        properties.put("Foo.Bar.C.Key2", "ValueC2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("ValueA1", nestedConfigurationA.properties().get("Key1"));
        assertEquals("ValueA2", nestedConfigurationA.properties().get("Key2"));
        assertEquals(2, nestedConfigurationA.properties().size());

        assertEquals("ValueB1", nestedConfigurationB.properties().get("Key1"));
        assertEquals("ValueB2", nestedConfigurationB.properties().get("Key2"));
        assertEquals(2, nestedConfigurationB.properties().size());

        assertEquals("ValueC1", nestedConfigurationC.properties().get("Key1"));
        assertEquals("ValueC2", nestedConfigurationC.properties().get("Key2"));
        assertEquals(2, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithoutSuffixKeepsClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.A", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.A.Class", "ClassValue");
        properties.put("Foo.Bar.A.class", "ClassValue");
        properties.put("Foo.Bar.B", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.B.Class", "ClassValue");
        properties.put("Foo.Bar.B.class", "ClassValue");
        properties.put("Foo.Bar.C", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.C.Class", "ClassValue");
        properties.put("Foo.Bar.C.class", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertFalse(nestedConfigurationA.properties().containsKey(""));
        assertEquals("ClassValue", nestedConfigurationA.properties().get("Class"));
        assertEquals("ClassValue", nestedConfigurationA.properties().get("class"));
        assertEquals(2, nestedConfigurationA.properties().size());

        assertFalse(nestedConfigurationB.properties().containsKey(""));
        assertEquals("ClassValue", nestedConfigurationB.properties().get("Class"));
        assertEquals("ClassValue", nestedConfigurationB.properties().get("class"));
        assertEquals(2, nestedConfigurationB.properties().size());

        assertFalse(nestedConfigurationC.properties().containsKey(""));
        assertEquals("ClassValue", nestedConfigurationC.properties().get("Class"));
        assertEquals("ClassValue", nestedConfigurationC.properties().get("class"));
        assertEquals(2, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithUpperCaseSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.A.Class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.B.Class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.C.Class", "foo.bar.NestedTestClassC");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("Foo.Bar.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Foo.Bar.A", nestedConfigurationA.name().canonical());
        assertEquals("foo.bar.NestedTestClassA", nestedConfigurationA.className());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Foo.Bar.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Foo.Bar.B", nestedConfigurationB.name().canonical());
        assertEquals("foo.bar.NestedTestClassB", nestedConfigurationB.className());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Foo.Bar.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Foo.Bar.C", nestedConfigurationC.name().canonical());
        assertEquals("foo.bar.NestedTestClassC", nestedConfigurationC.className());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedConfigurationMapWithUpperCaseSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.A.Class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.A.Key1", "ValueA1");
        properties.put("Foo.Bar.A.Key2", "ValueA2");
        properties.put("Foo.Bar.B.Class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.B.Key1", "ValueB1");
        properties.put("Foo.Bar.B.Key2", "ValueB2");
        properties.put("Foo.Bar.C.Class", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.C.Key1", "ValueC1");
        properties.put("Foo.Bar.C.Key2", "ValueC2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("ValueA1", nestedConfigurationA.properties().get("Key1"));
        assertEquals("ValueA2", nestedConfigurationA.properties().get("Key2"));
        assertEquals(2, nestedConfigurationA.properties().size());

        assertEquals("ValueB1", nestedConfigurationB.properties().get("Key1"));
        assertEquals("ValueB2", nestedConfigurationB.properties().get("Key2"));
        assertEquals(2, nestedConfigurationB.properties().size());

        assertEquals("ValueC1", nestedConfigurationC.properties().get("Key1"));
        assertEquals("ValueC2", nestedConfigurationC.properties().get("Key2"));
        assertEquals(2, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithUpperCaseSuffixRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.A.Class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.A.class", "ClassValue");
        properties.put("Foo.Bar.A", "ClassValue");
        properties.put("Foo.Bar.B.Class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.B.class", "ClassValue");
        properties.put("Foo.Bar.B", "ClassValue");
        properties.put("Foo.Bar.C.Class", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.C.class", "ClassValue");
        properties.put("Foo.Bar.C", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertFalse(nestedConfigurationA.properties().containsKey(""));
        assertFalse(nestedConfigurationA.properties().containsKey("Class"));
        assertFalse(nestedConfigurationA.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationA.properties().size());

        assertFalse(nestedConfigurationB.properties().containsKey(""));
        assertFalse(nestedConfigurationB.properties().containsKey("Class"));
        assertFalse(nestedConfigurationB.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationB.properties().size());

        assertFalse(nestedConfigurationC.properties().containsKey(""));
        assertFalse(nestedConfigurationC.properties().containsKey("Class"));
        assertFalse(nestedConfigurationC.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithLowerCaseSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.A.class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.B.class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.C.class", "foo.bar.NestedTestClassC");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("Foo.Bar.A.class", nestedConfigurationA.name().actual());
        assertEquals("Foo.Bar.A", nestedConfigurationA.name().canonical());
        assertEquals("foo.bar.NestedTestClassA", nestedConfigurationA.className());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Foo.Bar.B.class", nestedConfigurationB.name().actual());
        assertEquals("Foo.Bar.B", nestedConfigurationB.name().canonical());
        assertEquals("foo.bar.NestedTestClassB", nestedConfigurationB.className());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Foo.Bar.C.class", nestedConfigurationC.name().actual());
        assertEquals("Foo.Bar.C", nestedConfigurationC.name().canonical());
        assertEquals("foo.bar.NestedTestClassC", nestedConfigurationC.className());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedConfigurationMapWithLowerCaseSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.A.class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.A.Key1", "ValueA1");
        properties.put("Foo.Bar.A.Key2", "ValueA2");
        properties.put("Foo.Bar.B.class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.B.Key1", "ValueB1");
        properties.put("Foo.Bar.B.Key2", "ValueB2");
        properties.put("Foo.Bar.C.class", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.C.Key1", "ValueC1");
        properties.put("Foo.Bar.C.Key2", "ValueC2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("ValueA1", nestedConfigurationA.properties().get("Key1"));
        assertEquals("ValueA2", nestedConfigurationA.properties().get("Key2"));
        assertEquals(2, nestedConfigurationA.properties().size());

        assertEquals("ValueB1", nestedConfigurationB.properties().get("Key1"));
        assertEquals("ValueB2", nestedConfigurationB.properties().get("Key2"));
        assertEquals(2, nestedConfigurationB.properties().size());

        assertEquals("ValueC1", nestedConfigurationC.properties().get("Key1"));
        assertEquals("ValueC2", nestedConfigurationC.properties().get("Key2"));
        assertEquals(2, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithLowerCaseSuffixRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.A.class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.A.Class", "ClassValue");
        properties.put("Foo.Bar.A", "ClassValue");
        properties.put("Foo.Bar.B.class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.B.Class", "ClassValue");
        properties.put("Foo.Bar.B", "ClassValue");
        properties.put("Foo.Bar.C.class", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.C.Class", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertFalse(nestedConfigurationA.properties().containsKey(""));
        assertFalse(nestedConfigurationA.properties().containsKey("Class"));
        assertFalse(nestedConfigurationA.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationA.properties().size());

        assertFalse(nestedConfigurationB.properties().containsKey(""));
        assertFalse(nestedConfigurationB.properties().containsKey("Class"));
        assertFalse(nestedConfigurationB.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationB.properties().size());

        assertFalse(nestedConfigurationC.properties().containsKey(""));
        assertFalse(nestedConfigurationC.properties().containsKey("Class"));
        assertFalse(nestedConfigurationC.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithPrefixWithoutSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.A", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.Baz.B", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.Baz.C", "foo.bar.NestedTestClassC");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("Foo.Bar.Baz.A", nestedConfigurationA.name().actual());
        assertEquals("Foo.Bar.Baz.A", nestedConfigurationA.name().canonical());
        assertEquals("foo.bar.NestedTestClassA", nestedConfigurationA.className());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Foo.Bar.Baz.B", nestedConfigurationB.name().actual());
        assertEquals("Foo.Bar.Baz.B", nestedConfigurationB.name().canonical());
        assertEquals("foo.bar.NestedTestClassB", nestedConfigurationB.className());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Foo.Bar.Baz.C", nestedConfigurationC.name().actual());
        assertEquals("Foo.Bar.Baz.C", nestedConfigurationC.name().canonical());
        assertEquals("foo.bar.NestedTestClassC", nestedConfigurationC.className());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedConfigurationMapWithPrefixWithoutSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.A", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.Baz.A.Key1", "ValueA1");
        properties.put("Foo.Bar.Baz.A.Key2", "ValueA2");
        properties.put("Foo.Bar.Baz.B", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.Baz.B.Key1", "ValueB1");
        properties.put("Foo.Bar.Baz.B.Key2", "ValueB2");
        properties.put("Foo.Bar.Baz.C", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.Baz.C.Key1", "ValueC1");
        properties.put("Foo.Bar.Baz.C.Key2", "ValueC2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("ValueA1", nestedConfigurationA.properties().get("Key1"));
        assertEquals("ValueA2", nestedConfigurationA.properties().get("Key2"));
        assertEquals(2, nestedConfigurationA.properties().size());

        assertEquals("ValueB1", nestedConfigurationB.properties().get("Key1"));
        assertEquals("ValueB2", nestedConfigurationB.properties().get("Key2"));
        assertEquals(2, nestedConfigurationB.properties().size());

        assertEquals("ValueC1", nestedConfigurationC.properties().get("Key1"));
        assertEquals("ValueC2", nestedConfigurationC.properties().get("Key2"));
        assertEquals(2, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithPrefixWithoutSuffixKeepsClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.A", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.Baz.A.Class", "ClassValue");
        properties.put("Foo.Bar.Baz.A.class", "ClassValue");
        properties.put("Foo.Bar.Baz.B", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.Baz.B.Class", "ClassValue");
        properties.put("Foo.Bar.Baz.B.class", "ClassValue");
        properties.put("Foo.Bar.Baz.C", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.Baz.C.Class", "ClassValue");
        properties.put("Foo.Bar.Baz.C.class", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertFalse(nestedConfigurationA.properties().containsKey(""));
        assertEquals("ClassValue", nestedConfigurationA.properties().get("Class"));
        assertEquals("ClassValue", nestedConfigurationA.properties().get("class"));
        assertEquals(2, nestedConfigurationA.properties().size());

        assertFalse(nestedConfigurationB.properties().containsKey(""));
        assertEquals("ClassValue", nestedConfigurationB.properties().get("Class"));
        assertEquals("ClassValue", nestedConfigurationB.properties().get("class"));
        assertEquals(2, nestedConfigurationB.properties().size());

        assertFalse(nestedConfigurationC.properties().containsKey(""));
        assertEquals("ClassValue", nestedConfigurationC.properties().get("Class"));
        assertEquals("ClassValue", nestedConfigurationC.properties().get("class"));
        assertEquals(2, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithPrefixWithUpperCaseSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.A.Class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.Baz.B.Class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.Baz.C.Class", "foo.bar.NestedTestClassC");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("Foo.Bar.Baz.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Foo.Bar.Baz.A", nestedConfigurationA.name().canonical());
        assertEquals("foo.bar.NestedTestClassA", nestedConfigurationA.className());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Foo.Bar.Baz.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Foo.Bar.Baz.B", nestedConfigurationB.name().canonical());
        assertEquals("foo.bar.NestedTestClassB", nestedConfigurationB.className());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Foo.Bar.Baz.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Foo.Bar.Baz.C", nestedConfigurationC.name().canonical());
        assertEquals("foo.bar.NestedTestClassC", nestedConfigurationC.className());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedConfigurationMapWithPrefixWithUpperCaseSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.A.Class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.Baz.A.Key1", "ValueA1");
        properties.put("Foo.Bar.Baz.A.Key2", "ValueA2");
        properties.put("Foo.Bar.Baz.B.Class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.Baz.B.Key1", "ValueB1");
        properties.put("Foo.Bar.Baz.B.Key2", "ValueB2");
        properties.put("Foo.Bar.Baz.C.Class", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.Baz.C.Key1", "ValueC1");
        properties.put("Foo.Bar.Baz.C.Key2", "ValueC2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("ValueA1", nestedConfigurationA.properties().get("Key1"));
        assertEquals("ValueA2", nestedConfigurationA.properties().get("Key2"));
        assertEquals(2, nestedConfigurationA.properties().size());

        assertEquals("ValueB1", nestedConfigurationB.properties().get("Key1"));
        assertEquals("ValueB2", nestedConfigurationB.properties().get("Key2"));
        assertEquals(2, nestedConfigurationB.properties().size());

        assertEquals("ValueC1", nestedConfigurationC.properties().get("Key1"));
        assertEquals("ValueC2", nestedConfigurationC.properties().get("Key2"));
        assertEquals(2, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithPrefixWithUpperCaseSuffixRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.A.Class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.Baz.A.class", "ClassValue");
        properties.put("Foo.Bar.Baz.A", "ClassValue");
        properties.put("Foo.Bar.Baz.B.Class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.Baz.B.class", "ClassValue");
        properties.put("Foo.Bar.Baz.B", "ClassValue");
        properties.put("Foo.Bar.Baz.C.Class", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.Baz.C.class", "ClassValue");
        properties.put("Foo.Bar.Baz.C", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertFalse(nestedConfigurationA.properties().containsKey(""));
        assertFalse(nestedConfigurationA.properties().containsKey("Class"));
        assertFalse(nestedConfigurationA.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationA.properties().size());

        assertFalse(nestedConfigurationB.properties().containsKey(""));
        assertFalse(nestedConfigurationB.properties().containsKey("Class"));
        assertFalse(nestedConfigurationB.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationB.properties().size());

        assertFalse(nestedConfigurationC.properties().containsKey(""));
        assertFalse(nestedConfigurationC.properties().containsKey("Class"));
        assertFalse(nestedConfigurationC.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithPrefixWithLowerCaseSuffix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.A.class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.Baz.B.class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.Baz.C.class", "foo.bar.NestedTestClassC");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("Foo.Bar.Baz.A.class", nestedConfigurationA.name().actual());
        assertEquals("Foo.Bar.Baz.A", nestedConfigurationA.name().canonical());
        assertEquals("foo.bar.NestedTestClassA", nestedConfigurationA.className());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Foo.Bar.Baz.B.class", nestedConfigurationB.name().actual());
        assertEquals("Foo.Bar.Baz.B", nestedConfigurationB.name().canonical());
        assertEquals("foo.bar.NestedTestClassB", nestedConfigurationB.className());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Foo.Bar.Baz.C.class", nestedConfigurationC.name().actual());
        assertEquals("Foo.Bar.Baz.C", nestedConfigurationC.name().canonical());
        assertEquals("foo.bar.NestedTestClassC", nestedConfigurationC.className());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedConfigurationMapWithPrefixWithLowerCaseSuffixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.A.class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.Baz.A.Key1", "ValueA1");
        properties.put("Foo.Bar.Baz.A.Key2", "ValueA2");
        properties.put("Foo.Bar.Baz.B.class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.Baz.B.Key1", "ValueB1");
        properties.put("Foo.Bar.Baz.B.Key2", "ValueB2");
        properties.put("Foo.Bar.Baz.C.class", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.Baz.C.Key1", "ValueC1");
        properties.put("Foo.Bar.Baz.C.Key2", "ValueC2");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("ValueA1", nestedConfigurationA.properties().get("Key1"));
        assertEquals("ValueA2", nestedConfigurationA.properties().get("Key2"));
        assertEquals(2, nestedConfigurationA.properties().size());

        assertEquals("ValueB1", nestedConfigurationB.properties().get("Key1"));
        assertEquals("ValueB2", nestedConfigurationB.properties().get("Key2"));
        assertEquals(2, nestedConfigurationB.properties().size());

        assertEquals("ValueC1", nestedConfigurationC.properties().get("Key1"));
        assertEquals("ValueC2", nestedConfigurationC.properties().get("Key2"));
        assertEquals(2, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedConfigurationMapWithPrefixWithLowerCaseSuffixRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.class", "foo.bar.TestClass");
        properties.put("Foo.Bar.Baz.A.class", "foo.bar.NestedTestClassA");
        properties.put("Foo.Bar.Baz.A.Class", "ClassValue");
        properties.put("Foo.Bar.Baz.A", "ClassValue");
        properties.put("Foo.Bar.Baz.B.class", "foo.bar.NestedTestClassB");
        properties.put("Foo.Bar.Baz.B.Class", "ClassValue");
        properties.put("Foo.Bar.Baz.B", "ClassValue");
        properties.put("Foo.Bar.Baz.C.class", "foo.bar.NestedTestClassC");
        properties.put("Foo.Bar.Baz.C.Class", "ClassValue");
        properties.put("Foo.Bar.Baz.C", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo.Bar.Class", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertFalse(nestedConfigurationA.properties().containsKey(""));
        assertFalse(nestedConfigurationA.properties().containsKey("Class"));
        assertFalse(nestedConfigurationA.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationA.properties().size());

        assertFalse(nestedConfigurationB.properties().containsKey(""));
        assertFalse(nestedConfigurationB.properties().containsKey("Class"));
        assertFalse(nestedConfigurationB.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationB.properties().size());

        assertFalse(nestedConfigurationC.properties().containsKey(""));
        assertFalse(nestedConfigurationC.properties().containsKey("Class"));
        assertFalse(nestedConfigurationC.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationC.properties().size());

    }

    @Test
    public void directConfiguration() {

        Map<String, String> properties = new HashMap<>();

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofClass("foo.bar.TestClass", properties);

        assertEquals("Class", configuration.name().actual());
        assertEquals("", configuration.name().canonical());
        assertEquals("foo.bar.TestClass", configuration.className());
        assertEquals(properties, configuration.fullProperties());

    }

    @Test
    public void directConfigurationRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Class", "ClassValue");
        properties.put("class", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofClass("foo.bar.TestClass", properties);

        assertFalse(configuration.properties().containsKey(""));
        assertFalse(configuration.properties().containsKey("Class"));
        assertFalse(configuration.properties().containsKey("class"));
        assertEquals(0, configuration.properties().size());

    }

    @Test
    public void nestedDirectConfiguration() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Baz.Class", "foo.bar.NestedTestClass");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofClass("foo.bar.TestClass", properties);
        MCRInstanceConfiguration nestedConfiguration = configuration.nestedConfiguration("Baz");

        assertEquals("Baz.Class", nestedConfiguration.name().actual());
        assertEquals("Baz", nestedConfiguration.name().canonical());
        assertEquals("foo.bar.NestedTestClass", nestedConfiguration.className());
        assertEquals(properties, nestedConfiguration.fullProperties());

    }

    @Test
    public void nestedDirectConfigurationMap() {

        Map<String, String> properties = new HashMap<>();
        properties.put("A.Class", "foo.bar.NestedTestClassA");
        properties.put("B.Class", "foo.bar.NestedTestClassB");
        properties.put("C.Class", "foo.bar.NestedTestClassC");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofClass("foo.bar.TestClass", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("A.Class", nestedConfigurationA.name().actual());
        assertEquals("A", nestedConfigurationA.name().canonical());
        assertEquals("foo.bar.NestedTestClassA", nestedConfigurationA.className());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("B.Class", nestedConfigurationB.name().actual());
        assertEquals("B", nestedConfigurationB.name().canonical());
        assertEquals("foo.bar.NestedTestClassB", nestedConfigurationB.className());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("C.Class", nestedConfigurationC.name().actual());
        assertEquals("C", nestedConfigurationC.name().canonical());
        assertEquals("foo.bar.NestedTestClassC", nestedConfigurationC.className());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedDirectConfigurationMapRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("A", "foo.bar.NestedTestClassA");
        properties.put("A.Class", "ClassValue");
        properties.put("A.class", "ClassValue");
        properties.put("B", "foo.bar.NestedTestClassB");
        properties.put("B.Class", "ClassValue");
        properties.put("B.class", "ClassValue");
        properties.put("C", "foo.bar.NestedTestClassC");
        properties.put("C.Class", "ClassValue");
        properties.put("C.class", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofClass("foo.bar.TestClass", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap();
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertFalse(nestedConfigurationA.properties().containsKey(""));
        assertFalse(nestedConfigurationA.properties().containsKey("Class"));
        assertFalse(nestedConfigurationA.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationA.properties().size());

        assertFalse(nestedConfigurationB.properties().containsKey(""));
        assertFalse(nestedConfigurationB.properties().containsKey("Class"));
        assertFalse(nestedConfigurationB.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationB.properties().size());

        assertFalse(nestedConfigurationC.properties().containsKey(""));
        assertFalse(nestedConfigurationC.properties().containsKey("Class"));
        assertFalse(nestedConfigurationC.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedDirectConfigurationMapWithPrefix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Baz.A.Class", "foo.bar.NestedTestClassA");
        properties.put("Baz.B.Class", "foo.bar.NestedTestClassB");
        properties.put("Baz.C.Class", "foo.bar.NestedTestClassC");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofClass("foo.bar.TestClass", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("Baz.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Baz.A", nestedConfigurationA.name().canonical());
        assertEquals("foo.bar.NestedTestClassA", nestedConfigurationA.className());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Baz.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Baz.B", nestedConfigurationB.name().canonical());
        assertEquals("foo.bar.NestedTestClassB", nestedConfigurationB.className());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Baz.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Baz.C", nestedConfigurationC.name().canonical());
        assertEquals("foo.bar.NestedTestClassC", nestedConfigurationC.className());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedDirectConfigurationMapWithPrefixRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Baz.A", "foo.bar.NestedTestClassA");
        properties.put("Baz.A.Class", "ClassValue");
        properties.put("Baz.A.class", "ClassValue");
        properties.put("Baz.B", "foo.bar.NestedTestClassB");
        properties.put("Baz.B.Class", "ClassValue");
        properties.put("Baz.B.class", "ClassValue");
        properties.put("Baz.C", "foo.bar.NestedTestClassC");
        properties.put("Baz.C.Class", "ClassValue");
        properties.put("Baz.C.class", "ClassValue");

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofClass("foo.bar.TestClass", properties);
        Map<String, MCRInstanceConfiguration> nestedConfigurations = configuration.nestedConfigurationMap("Baz");
        MCRInstanceConfiguration nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertFalse(nestedConfigurationA.properties().containsKey(""));
        assertFalse(nestedConfigurationA.properties().containsKey("Class"));
        assertFalse(nestedConfigurationA.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationA.properties().size());

        assertFalse(nestedConfigurationB.properties().containsKey(""));
        assertFalse(nestedConfigurationB.properties().containsKey("Class"));
        assertFalse(nestedConfigurationB.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationB.properties().size());

        assertFalse(nestedConfigurationC.properties().containsKey(""));
        assertFalse(nestedConfigurationC.properties().containsKey("Class"));
        assertFalse(nestedConfigurationC.properties().containsKey("class"));
        assertEquals(0, nestedConfigurationC.properties().size());

    }

}

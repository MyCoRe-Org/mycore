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

    private static final String NESTED_TEST_CLASS = NestedTestClass.class.getName();

    private static final String NESTED_TEST_CLASS_A = NestedTestClassA.class.getName();

    private static final String NESTED_TEST_CLASS_B = NestedTestClassB.class.getName();

    private static final String NESTED_TEST_CLASS_C = NestedTestClassC.class.getName();

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
    public void nestedConfiguration() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", TEST_CLASS);
        properties.put("Foo.Bar.Baz.Class", NESTED_TEST_CLASS);

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar", properties);
        MCRInstanceConfiguration<?> nestedConfiguration = configuration.nested(Object.class, "Baz");

        System.out.println(configuration);
        System.out.println(nestedConfiguration);

        assertEquals("Foo.Bar.Baz.Class", nestedConfiguration.name().actual());
        assertEquals("Foo.Bar.Baz", nestedConfiguration.name().canonical());
        assertEquals(NestedTestClass.class, nestedConfiguration.valueClass());
        assertEquals(properties, nestedConfiguration.fullProperties());

    }

    @Test
    public void nestedConfigurationMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", TEST_CLASS);
        properties.put("Foo.Bar.Baz.Class", NESTED_TEST_CLASS);
        properties.put("Foo.Bar.Baz.Key1", "Value1");
        properties.put("Foo.Bar.Baz.Key2", "Value2");

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar", properties);
        MCRInstanceConfiguration<?> nestedConfiguration = configuration.nested(Object.class, "Baz");

        assertEquals("Value1", nestedConfiguration.properties().get("Key1"));
        assertEquals("Value2", nestedConfiguration.properties().get("Key2"));
        assertEquals(2, nestedConfiguration.properties().size());

    }

    @Test
    public void nestedMap() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", TEST_CLASS);
        properties.put("Foo.Bar.A.Class", NESTED_TEST_CLASS_A);
        properties.put("Foo.Bar.B.Class", NESTED_TEST_CLASS_B);
        properties.put("Foo.Bar.C.Class", NESTED_TEST_CLASS_C);

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar", properties);
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations = configuration.nestedMap(Object.class);
        MCRInstanceConfiguration<?> nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("Foo.Bar.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Foo.Bar.A", nestedConfigurationA.name().canonical());
        assertEquals(NestedTestClassA.class, nestedConfigurationA.valueClass());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Foo.Bar.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Foo.Bar.B", nestedConfigurationB.name().canonical());
        assertEquals(NestedTestClassB.class, nestedConfigurationB.valueClass());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Foo.Bar.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Foo.Bar.C", nestedConfigurationC.name().canonical());
        assertEquals(NestedTestClassC.class, nestedConfigurationC.valueClass());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedMapMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", TEST_CLASS);
        properties.put("Foo.Bar.A.Class", NESTED_TEST_CLASS_A);
        properties.put("Foo.Bar.A.Key1", "ValueA1");
        properties.put("Foo.Bar.A.Key2", "ValueA2");
        properties.put("Foo.Bar.B.Class", NESTED_TEST_CLASS_B);
        properties.put("Foo.Bar.B.Key1", "ValueB1");
        properties.put("Foo.Bar.B.Key2", "ValueB2");
        properties.put("Foo.Bar.C.Class", NESTED_TEST_CLASS_C);
        properties.put("Foo.Bar.C.Key1", "ValueC1");
        properties.put("Foo.Bar.C.Key2", "ValueC2");

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar", properties);
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations = configuration.nestedMap(Object.class);
        MCRInstanceConfiguration<?> nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC = nestedConfigurations.get("C");
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
    public void nestedMapWithPrefix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", TEST_CLASS);
        properties.put("Foo.Bar.Baz.A.Class", NESTED_TEST_CLASS_A);
        properties.put("Foo.Bar.Baz.B.Class", NESTED_TEST_CLASS_B);
        properties.put("Foo.Bar.Baz.C.Class", NESTED_TEST_CLASS_C);

        MCRInstanceConfiguration<?> configuration =
            ofName(Object.class, "Foo.Bar", properties);
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class, "Baz");
        MCRInstanceConfiguration<?> nestedConfigurationA =
            nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB =
            nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC =
            nestedConfigurations.get("C");

        assertEquals(3, nestedConfigurations.size());

        assertEquals("Foo.Bar.Baz.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Foo.Bar.Baz.A", nestedConfigurationA.name().canonical());
        assertEquals(NestedTestClassA.class, nestedConfigurationA.valueClass());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Foo.Bar.Baz.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Foo.Bar.Baz.B", nestedConfigurationB.name().canonical());
        assertEquals(NestedTestClassB.class, nestedConfigurationB.valueClass());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Foo.Bar.Baz.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Foo.Bar.Baz.C", nestedConfigurationC.name().canonical());
        assertEquals(NestedTestClassC.class, nestedConfigurationC.valueClass());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedMapWithPrefixMovesEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Foo.Bar.Class", TEST_CLASS);
        properties.put("Foo.Bar.Baz.A.Class", NESTED_TEST_CLASS_A);
        properties.put("Foo.Bar.Baz.A.Key1", "ValueA1");
        properties.put("Foo.Bar.Baz.A.Key2", "ValueA2");
        properties.put("Foo.Bar.Baz.B.Class", NESTED_TEST_CLASS_B);
        properties.put("Foo.Bar.Baz.B.Key1", "ValueB1");
        properties.put("Foo.Bar.Baz.B.Key2", "ValueB2");
        properties.put("Foo.Bar.Baz.C.Class", NESTED_TEST_CLASS_C);
        properties.put("Foo.Bar.Baz.C.Key1", "ValueC1");
        properties.put("Foo.Bar.Baz.C.Key2", "ValueC2");

        MCRInstanceConfiguration<?> configuration =
            ofName(Object.class, "Foo.Bar", properties);
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class, "Baz");
        MCRInstanceConfiguration<?> nestedConfigurationA =
            nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB =
            nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC =
            nestedConfigurations.get("C");

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
    public void directConfiguration() {

        Map<String, String> properties = new HashMap<>();

        MCRInstanceConfiguration<?> configuration =
            ofClass(Object.class, TestClass.class, "Instance", properties, properties);

        assertEquals("Instance.Class", configuration.name().actual());
        assertEquals("Instance", configuration.name().canonical());
        assertEquals(TestClass.class, configuration.valueClass());
        assertEquals(properties, configuration.fullProperties());

    }

    @Test
    public void directConfigurationRemovesClassEntry() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Class", "ClassValue");
        properties.put("class", "ClassValue");
        properties.put("", "ClassValue");

        MCRInstanceConfiguration<?> configuration =
            ofClass(Object.class, TestClass.class, "Instance", properties, properties);

        assertFalse(configuration.properties().containsKey("Class"));
        assertEquals("ClassValue", configuration.properties().get("class"));
        assertEquals("ClassValue", configuration.properties().get(""));
        assertEquals(2, configuration.properties().size());

    }

    @Test
    public void nestedDirectConfiguration() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Baz.Class", NESTED_TEST_CLASS);

        MCRInstanceConfiguration<?> configuration =
            ofClass(Object.class, TestClass.class, "Instance", properties, properties);
        MCRInstanceConfiguration<?> nestedConfiguration =
            configuration.nested(Object.class, "Baz");

        assertEquals("Instance.Baz.Class", nestedConfiguration.name().actual());
        assertEquals("Instance.Baz", nestedConfiguration.name().canonical());
        assertEquals(NestedTestClass.class, nestedConfiguration.valueClass());
        assertEquals(properties, nestedConfiguration.fullProperties());

    }

    @Test
    public void nestedDirectConfigurationMap() {

        Map<String, String> properties = new HashMap<>();
        properties.put("A.Class", NESTED_TEST_CLASS_A);
        properties.put("B.Class", NESTED_TEST_CLASS_B);
        properties.put("C.Class", NESTED_TEST_CLASS_C);

        MCRInstanceConfiguration<?> configuration =
            ofClass(Object.class, TestClass.class, "Instance", properties, properties);
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class);
        MCRInstanceConfiguration<?> nestedConfigurationA =
            nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB =
            nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC =
            nestedConfigurations.get("C");

        assertEquals(3, nestedConfigurations.size());

        assertEquals("Instance.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Instance.A", nestedConfigurationA.name().canonical());
        assertEquals(NestedTestClassA.class, nestedConfigurationA.valueClass());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Instance.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Instance.B", nestedConfigurationB.name().canonical());
        assertEquals(NestedTestClassB.class, nestedConfigurationB.valueClass());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Instance.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Instance.C", nestedConfigurationC.name().canonical());
        assertEquals(NestedTestClassC.class, nestedConfigurationC.valueClass());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedDirectConfigurationMapRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("A.Class", NESTED_TEST_CLASS_A);
        properties.put("A.class", "ClassValue");
        properties.put("A", "ClassValue");
        properties.put("B.Class", NESTED_TEST_CLASS_B);
        properties.put("B.class", "ClassValue");
        properties.put("B", "ClassValue");
        properties.put("C.Class", NESTED_TEST_CLASS_C);
        properties.put("C.class", "ClassValue");
        properties.put("C", "ClassValue");

        MCRInstanceConfiguration<?> configuration =
            ofClass(Object.class, TestClass.class, "Instance", properties, properties);
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class);
        MCRInstanceConfiguration<?> nestedConfigurationA =
            nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB =
            nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC =
            nestedConfigurations.get("C");

        assertEquals(3, nestedConfigurations.size());

        assertFalse(nestedConfigurationA.properties().containsKey("Class"));
        assertEquals("ClassValue", nestedConfigurationA.properties().get("class"));
        assertEquals("ClassValue", nestedConfigurationA.properties().get(""));
        assertEquals(2, nestedConfigurationA.properties().size());

        assertFalse(nestedConfigurationB.properties().containsKey("Class"));
        assertEquals("ClassValue", nestedConfigurationB.properties().get("class"));
        assertEquals("ClassValue", nestedConfigurationB.properties().get(""));
        assertEquals(2, nestedConfigurationB.properties().size());

        assertFalse(nestedConfigurationC.properties().containsKey("Class"));
        assertEquals("ClassValue", nestedConfigurationC.properties().get("class"));
        assertEquals("ClassValue", nestedConfigurationC.properties().get(""));
        assertEquals(2, nestedConfigurationC.properties().size());

    }

    @Test
    public void nestedDirectConfigurationMapWithPrefix() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Baz.A.Class", NESTED_TEST_CLASS_A);
        properties.put("Baz.B.Class", NESTED_TEST_CLASS_B);
        properties.put("Baz.C.Class", NESTED_TEST_CLASS_C);

        MCRInstanceConfiguration<?> configuration =
            ofClass(Object.class, TestClass.class, "Instance", properties, properties);
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class, "Baz");
        MCRInstanceConfiguration<?> nestedConfigurationA =
            nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB =
            nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC =
            nestedConfigurations.get("C");

        assertEquals(3, nestedConfigurations.size());

        assertEquals("Instance.Baz.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Instance.Baz.A", nestedConfigurationA.name().canonical());
        assertEquals(NestedTestClassA.class, nestedConfigurationA.valueClass());
        assertEquals(properties, nestedConfigurationA.fullProperties());

        assertEquals("Instance.Baz.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Instance.Baz.B", nestedConfigurationB.name().canonical());
        assertEquals(NestedTestClassB.class, nestedConfigurationB.valueClass());
        assertEquals(properties, nestedConfigurationB.fullProperties());

        assertEquals("Instance.Baz.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Instance.Baz.C", nestedConfigurationC.name().canonical());
        assertEquals(NestedTestClassC.class, nestedConfigurationC.valueClass());
        assertEquals(properties, nestedConfigurationC.fullProperties());

    }

    @Test
    public void nestedDirectConfigurationMapWithPrefixRemovesClassEntries() {

        Map<String, String> properties = new HashMap<>();
        properties.put("Baz.A.Class", NESTED_TEST_CLASS_A);
        properties.put("Baz.A.class", "ClassValue");
        properties.put("Baz.A", "ClassValue");
        properties.put("Baz.B.Class", NESTED_TEST_CLASS_B);
        properties.put("Baz.B.class", "ClassValue");
        properties.put("Baz.B", "ClassValue");
        properties.put("Baz.C.Class", NESTED_TEST_CLASS_C);
        properties.put("Baz.C.class", "ClassValue");
        properties.put("Baz.C", "ClassValue");

        MCRInstanceConfiguration<?> configuration =
            ofClass(Object.class, TestClass.class, "Instance", properties, properties);
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class, "Baz");
        MCRInstanceConfiguration<?> nestedConfigurationA =
            nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB =
            nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC =
            nestedConfigurations.get("C");

        assertEquals(3, nestedConfigurations.size());

        assertFalse(nestedConfigurationA.properties().containsKey("Class"));
        assertEquals("ClassValue", nestedConfigurationA.properties().get("class"));
        assertEquals("ClassValue", nestedConfigurationA.properties().get(""));
        assertEquals(2, nestedConfigurationA.properties().size());

        assertFalse(nestedConfigurationB.properties().containsKey("Class"));
        assertEquals("ClassValue", nestedConfigurationB.properties().get("class"));
        assertEquals("ClassValue", nestedConfigurationB.properties().get(""));
        assertEquals(2, nestedConfigurationB.properties().size());

        assertFalse(nestedConfigurationC.properties().containsKey("Class"));
        assertEquals("ClassValue", nestedConfigurationC.properties().get("class"));
        assertEquals("ClassValue", nestedConfigurationC.properties().get(""));
        assertEquals(2, nestedConfigurationC.properties().size());

    }

    public static class TestClass {

    }

    public static class NestedTestClass {

    }

    public static class NestedTestClassA {

    }

    public static class NestedTestClassB {

    }

    public static class NestedTestClassC {

    }

}

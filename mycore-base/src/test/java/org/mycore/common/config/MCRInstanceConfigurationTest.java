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

    private static final String NESTED_TEST_CLASS = NestedTestClass.class.getName();

    private static final String NESTED_TEST_CLASS_A = NestedTestClassA.class.getName();

    private static final String NESTED_TEST_CLASS_B = NestedTestClassB.class.getName();

    private static final String NESTED_TEST_CLASS_C = NestedTestClassC.class.getName();

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
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo.Bar.Class", classNameOf = TestClass.class),
        @MCRTestProperty(key = "Foo.Bar.Baz.Class", classNameOf = NestedTestClass.class)
    })
    public void nestedConfiguration() {

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar");
        MCRInstanceConfiguration<?> nestedConfiguration = configuration.nested(Object.class, "Baz");

        assertEquals("Foo.Bar.Baz.Class", nestedConfiguration.name().actual());
        assertEquals("Foo.Bar.Baz", nestedConfiguration.name().canonical());
        assertEquals(NestedTestClass.class, nestedConfiguration.valueClass());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo.Bar.Class", classNameOf = TestClass.class),
        @MCRTestProperty(key = "Foo.Bar.Baz.Class", classNameOf = NestedTestClass.class),
        @MCRTestProperty(key = "Foo.Bar.Baz.Key1", string = "Value1"),
        @MCRTestProperty(key = "Foo.Bar.Baz.Key2", string = "Value2")
    })
    public void nestedConfigurationMovesEntries() {

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar");
        MCRInstanceConfiguration<?> nestedConfiguration = configuration.nested(Object.class, "Baz");

        assertEquals("Value1", nestedConfiguration.properties().get("Key1"));
        assertEquals("Value2", nestedConfiguration.properties().get("Key2"));
        assertEquals(2, nestedConfiguration.properties().size());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo.Bar.Class", classNameOf = TestClass.class),
        @MCRTestProperty(key = "Foo.Bar.A.Class", classNameOf = NestedTestClassA.class),
        @MCRTestProperty(key = "Foo.Bar.B.Class", classNameOf = NestedTestClassB.class),
        @MCRTestProperty(key = "Foo.Bar.C.Class", classNameOf = NestedTestClassC.class)
    })
    public void nestedMap() {

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar");
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations = configuration.nestedMap(Object.class);
        MCRInstanceConfiguration<?> nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC = nestedConfigurations.get("C");
        assertEquals(3, nestedConfigurations.size());

        assertEquals("Foo.Bar.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Foo.Bar.A", nestedConfigurationA.name().canonical());
        assertEquals(NestedTestClassA.class, nestedConfigurationA.valueClass());

        assertEquals("Foo.Bar.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Foo.Bar.B", nestedConfigurationB.name().canonical());
        assertEquals(NestedTestClassB.class, nestedConfigurationB.valueClass());

        assertEquals("Foo.Bar.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Foo.Bar.C", nestedConfigurationC.name().canonical());
        assertEquals(NestedTestClassC.class, nestedConfigurationC.valueClass());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo.Bar.Class", classNameOf = TestClass.class),
        @MCRTestProperty(key = "Foo.Bar.A.Class", classNameOf = NestedTestClassA.class),
        @MCRTestProperty(key = "Foo.Bar.A.Key1", string = "ValueA1"),
        @MCRTestProperty(key = "Foo.Bar.A.Key2", string = "ValueA2"),
        @MCRTestProperty(key = "Foo.Bar.B.Class", classNameOf = NestedTestClassB.class),
        @MCRTestProperty(key = "Foo.Bar.B.Key1", string = "ValueB1"),
        @MCRTestProperty(key = "Foo.Bar.B.Key2", string = "ValueB2"),
        @MCRTestProperty(key = "Foo.Bar.C.Class", classNameOf = NestedTestClassC.class),
        @MCRTestProperty(key = "Foo.Bar.C.Key1", string = "ValueC1"),
        @MCRTestProperty(key = "Foo.Bar.C.Key2", string = "ValueC2")
    })
    public void nestedMapMovesEntries() {

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar");
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
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo.Bar.Class", classNameOf = TestClass.class),
        @MCRTestProperty(key = "Foo.Bar.Baz.A.Class", classNameOf = NestedTestClassA.class),
        @MCRTestProperty(key = "Foo.Bar.Baz.B.Class", classNameOf = NestedTestClassB.class),
        @MCRTestProperty(key = "Foo.Bar.Baz.C.Class", classNameOf = NestedTestClassC.class)
    })
    public void nestedMapWithPrefix() {

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar");
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class, "Baz");
        MCRInstanceConfiguration<?> nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC = nestedConfigurations.get("C");

        assertEquals(3, nestedConfigurations.size());

        assertEquals("Foo.Bar.Baz.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Foo.Bar.Baz.A", nestedConfigurationA.name().canonical());
        assertEquals(NestedTestClassA.class, nestedConfigurationA.valueClass());

        assertEquals("Foo.Bar.Baz.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Foo.Bar.Baz.B", nestedConfigurationB.name().canonical());
        assertEquals(NestedTestClassB.class, nestedConfigurationB.valueClass());

        assertEquals("Foo.Bar.Baz.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Foo.Bar.Baz.C", nestedConfigurationC.name().canonical());
        assertEquals(NestedTestClassC.class, nestedConfigurationC.valueClass());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo.Bar.Class", classNameOf = TestClass.class),
        @MCRTestProperty(key = "Foo.Bar.Baz.A.Class", classNameOf = NestedTestClassA.class),
        @MCRTestProperty(key = "Foo.Bar.Baz.A.Key1", string = "ValueA1"),
        @MCRTestProperty(key = "Foo.Bar.Baz.A.Key2", string = "ValueA2"),
        @MCRTestProperty(key = "Foo.Bar.Baz.B.Class", classNameOf = NestedTestClassB.class),
        @MCRTestProperty(key = "Foo.Bar.Baz.B.Key1", string = "ValueB1"),
        @MCRTestProperty(key = "Foo.Bar.Baz.B.Key2", string = "ValueB2"),
        @MCRTestProperty(key = "Foo.Bar.Baz.C.Class", classNameOf = NestedTestClassC.class),
        @MCRTestProperty(key = "Foo.Bar.Baz.C.Key1", string = "ValueC1"),
        @MCRTestProperty(key = "Foo.Bar.Baz.C.Key2", string = "ValueC2")
    })
    public void nestedMapWithPrefixMovesEntries() {

        MCRInstanceConfiguration<?> configuration = ofName(Object.class, "Foo.Bar");
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class, "Baz");
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

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Instance.Baz.Class", classNameOf = NestedTestClass.class)
    })
    public void nestedDirectConfiguration() {

        MCRInstanceConfiguration<?> configuration = ofClass(Object.class, TestClass.class, "Instance");
        MCRInstanceConfiguration<?> nestedConfiguration = configuration.nested(Object.class, "Baz");

        assertEquals("Instance.Baz.Class", nestedConfiguration.name().actual());
        assertEquals("Instance.Baz", nestedConfiguration.name().canonical());
        assertEquals(NestedTestClass.class, nestedConfiguration.valueClass());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Instance.A.Class", classNameOf = NestedTestClassA.class),
        @MCRTestProperty(key = "Instance.B.Class", classNameOf = NestedTestClassB.class),
        @MCRTestProperty(key = "Instance.C.Class", classNameOf = NestedTestClassC.class)
    })
    public void nestedDirectConfigurationMap() {

        MCRInstanceConfiguration<?> configuration = ofClass(Object.class, TestClass.class, "Instance");
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class);
        MCRInstanceConfiguration<?> nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC = nestedConfigurations.get("C");

        assertEquals(3, nestedConfigurations.size());

        assertEquals("Instance.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Instance.A", nestedConfigurationA.name().canonical());
        assertEquals(NestedTestClassA.class, nestedConfigurationA.valueClass());

        assertEquals("Instance.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Instance.B", nestedConfigurationB.name().canonical());
        assertEquals(NestedTestClassB.class, nestedConfigurationB.valueClass());

        assertEquals("Instance.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Instance.C", nestedConfigurationC.name().canonical());
        assertEquals(NestedTestClassC.class, nestedConfigurationC.valueClass());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Instance.A.Class", classNameOf = NestedTestClassA.class),
        @MCRTestProperty(key = "Instance.A.class", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.A", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.B.Class", classNameOf = NestedTestClassB.class),
        @MCRTestProperty(key = "Instance.B.class", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.B", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.C.Class", classNameOf = NestedTestClassC.class),
        @MCRTestProperty(key = "Instance.C.class", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.C", string = "ClassValue")
    })
    public void nestedDirectConfigurationMapRemovesClassEntries() {

        MCRInstanceConfiguration<?> configuration = ofClass(Object.class, TestClass.class, "Instance");
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class);
        MCRInstanceConfiguration<?> nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC = nestedConfigurations.get("C");

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
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Instance.Baz.A.Class", classNameOf = NestedTestClassA.class),
        @MCRTestProperty(key = "Instance.Baz.B.Class", classNameOf = NestedTestClassB.class),
        @MCRTestProperty(key = "Instance.Baz.C.Class", classNameOf = NestedTestClassC.class)
    })
    public void nestedDirectConfigurationMapWithPrefix() {

        MCRInstanceConfiguration<?> configuration = ofClass(Object.class, TestClass.class, "Instance");
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class, "Baz");
        MCRInstanceConfiguration<?> nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC = nestedConfigurations.get("C");

        assertEquals(3, nestedConfigurations.size());

        assertEquals("Instance.Baz.A.Class", nestedConfigurationA.name().actual());
        assertEquals("Instance.Baz.A", nestedConfigurationA.name().canonical());
        assertEquals(NestedTestClassA.class, nestedConfigurationA.valueClass());

        assertEquals("Instance.Baz.B.Class", nestedConfigurationB.name().actual());
        assertEquals("Instance.Baz.B", nestedConfigurationB.name().canonical());
        assertEquals(NestedTestClassB.class, nestedConfigurationB.valueClass());

        assertEquals("Instance.Baz.C.Class", nestedConfigurationC.name().actual());
        assertEquals("Instance.Baz.C", nestedConfigurationC.name().canonical());
        assertEquals(NestedTestClassC.class, nestedConfigurationC.valueClass());

    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Instance.A.Class", classNameOf = NestedTestClassA.class),
        @MCRTestProperty(key = "Instance.Baz.A.class", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.Baz.A", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.Baz.B.Class", classNameOf = NestedTestClassB.class),
        @MCRTestProperty(key = "Instance.Baz.B.class", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.Baz.B", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.Baz.C.Class", classNameOf = NestedTestClassC.class),
        @MCRTestProperty(key = "Instance.Baz.C.class", string = "ClassValue"),
        @MCRTestProperty(key = "Instance.Baz.C", string = "ClassValue")
    })
    public void nestedDirectConfigurationMapWithPrefixRemovesClassEntries() {

        MCRInstanceConfiguration<?> configuration = ofClass(Object.class, TestClass.class, "Instance");
        Map<String, ? extends MCRInstanceConfiguration<?>> nestedConfigurations =
            configuration.nestedMap(Object.class, "Baz");
        MCRInstanceConfiguration<?> nestedConfigurationA = nestedConfigurations.get("A");
        MCRInstanceConfiguration<?> nestedConfigurationB = nestedConfigurations.get("B");
        MCRInstanceConfiguration<?> nestedConfigurationC = nestedConfigurations.get("C");

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

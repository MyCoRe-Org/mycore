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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.ofName;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRClassProperty;
import org.mycore.common.config.annotation.MCRClassPropertyList;
import org.mycore.common.config.annotation.MCRClassPropertyMap;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRInstantiatorClassPropertyBasicTest {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = TestClass.class),
            @MCRTestProperty(key = "Foo.Value", classNameOf = FooValue.class),
        })
    public void classValue() {

        TestClass instance = ofName(TestClass.class, "Foo").instantiate();

        assertNotNull(instance);
        assertEquals(FooValue.class, instance.value);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = TestClass.class),
            @MCRTestProperty(key = "Foo.Value", classNameOf = IncompatibleValue.class),
        })
    public void incompatibleClassValue() {

        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> MCRInstanceConfiguration.ofName(TestClass.class, "Foo").instantiate());

        assertEquals("Class, configured in Foo.Value (and sub-properties thereof),"
            + " for target field 'value' in configured class " + TestClass.class.getName()
            + " has a class (" + IncompatibleValue.class.getName() + ") that is incompatible"
            + " with the annotated class (" + Value.class.getName() + ")", exception.getMessage());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = TestClassWithMap.class),
            @MCRTestProperty(key = "Foo.Values.foo", classNameOf = FooValue.class),
            @MCRTestProperty(key = "Foo.Values.foo.Extra", string = "FOO"),
            @MCRTestProperty(key = "Foo.Values.bar", classNameOf = BarValue.class),
            @MCRTestProperty(key = "Foo.Values.bar.Extra", string = "BAR"),
        })
    public void classValueMap() {

        TestClassWithMap instance = ofName(TestClassWithMap.class, "Foo").instantiate();

        assertNotNull(instance);
        assertEquals(FooValue.class, instance.values.get("foo"));
        assertEquals(BarValue.class, instance.values.get("bar"));

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = TestClassWithList.class),
            @MCRTestProperty(key = "Foo.Values.10", classNameOf = FooValue.class),
            @MCRTestProperty(key = "Foo.Values.10.Extra", string = "FOO"),
            @MCRTestProperty(key = "Foo.Values.20", classNameOf = BarValue.class),
            @MCRTestProperty(key = "Foo.Values.20.Extra", string = "BAR"),
        })
    public void classValueList() {

        TestClassWithList instance = ofName(TestClassWithList.class, "Foo").instantiate();

        assertNotNull(instance);
        assertEquals(FooValue.class, instance.values.getFirst());
        assertEquals(BarValue.class, instance.values.getLast());

    }

    public static class TestClass {

        @MCRClassProperty(name = "Value", valueClass = Value.class)
        public Class<? extends Value> value;

    }

    public static class TestClassWithMap {

        @MCRClassPropertyMap(name = "Values", valueClass = Value.class)
        public Map<String, Class<? extends Value>> values;

    }

    public static class TestClassWithList {

        @MCRClassPropertyList(name = "Values", valueClass = Value.class)
        public List<Class<? extends Value>> values;

    }

    public interface Value {

    }

    public static final class FooValue implements Value {

    }

    public static final class BarValue implements Value {

    }

    public static final class IncompatibleValue {

    }

}

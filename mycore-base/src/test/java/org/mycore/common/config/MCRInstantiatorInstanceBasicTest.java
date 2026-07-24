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
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRInstantiatorInstanceBasicTest {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = TestClass.class),
            @MCRTestProperty(key = "Foo.Value.Class", classNameOf = FooValue.class),
        })
    public void instanceValue() {

        TestClass instance = ofName(TestClass.class, "Foo").instantiate();

        assertNotNull(instance);
        assertEquals(FooValue.class, instance.value.getClass());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = TestClass.class),
            @MCRTestProperty(key = "Foo.Value.Class", classNameOf = IncompatibleValue.class),
        })
    public void incompatibleInstanceValue() {

        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> MCRInstanceConfiguration.ofName(TestClass.class, "Foo").instantiate());

        assertEquals("Instance, configured in Foo.Value (and sub-properties thereof),"
            + " for target field 'value' in configured class " + TestClass.class.getName()
            + " has a class (" + IncompatibleValue.class.getName() + ") that is incompatible"
            + " with the annotated class (" + Value.class.getName() + ")", exception.getMessage());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = TestClassWithMap.class),
            @MCRTestProperty(key = "Foo.Values.foo.Class", classNameOf = FooValue.class),
            @MCRTestProperty(key = "Foo.Values.foo.Extra", string = "FOO"),
            @MCRTestProperty(key = "Foo.Values.bar.Class", classNameOf = BarValue.class),
            @MCRTestProperty(key = "Foo.Values.bar.Extra", string = "BAR"),
        })
    public void instanceValueMap() {

        TestClassWithMap instance = ofName(TestClassWithMap.class, "Foo").instantiate();

        assertNotNull(instance);
        assertEquals(FooValue.class, instance.values.get("foo").getClass());
        assertEquals(BarValue.class, instance.values.get("bar").getClass());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = TestClassWithList.class),
            @MCRTestProperty(key = "Foo.Values.10.Class", classNameOf = FooValue.class),
            @MCRTestProperty(key = "Foo.Values.10.Extra", string = "FOO"),
            @MCRTestProperty(key = "Foo.Values.20.Class", classNameOf = BarValue.class),
            @MCRTestProperty(key = "Foo.Values.20.Extra", string = "BAR"),
        })
    public void instanceValueList() {

        TestClassWithList instance = ofName(TestClassWithList.class, "Foo").instantiate();

        assertNotNull(instance);
        assertEquals(FooValue.class, instance.values.getFirst().getClass());
        assertEquals(BarValue.class, instance.values.getLast().getClass());

    }

    public static class TestClass {

        @MCRInstance(name = "Value", valueClass = Value.class)
        public Value value;

    }

    public static class TestClassWithMap {

        @MCRInstanceMap(name = "Values", valueClass = Value.class)
        public Map<String, Value> values;

    }

    public static class TestClassWithList {

        @MCRInstanceList(name = "Values", valueClass = Value.class)
        public List<Value> values;

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

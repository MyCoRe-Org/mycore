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

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRProperty;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@MCRTestConfiguration(
    properties = {
        @MCRTestProperty(key = "Default.Property1", string = "Value1"),
        @MCRTestProperty(key = "Default.Property2", string = "Value2"),
        @MCRTestProperty(key = "Default.OneProperty", string = "OneValue"),
        @MCRTestProperty(key = "Default.OtherProperty", string = "OtherValue")
    })
public class MCRConfigurableInstanceHelperDefaultTest extends MCRTestCase {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class)
        })
    public void nested() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedClass instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedClass.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("Value1", instance.nested.string1);
        assertEquals("Value2", instance.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithImplicitNestedClass.class)
        })
    public void nestedImplicit() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithImplicitNestedClass instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithImplicitNestedClass.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("Value1", instance.nested.string1);
        assertEquals("Value2", instance.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = TestClassWithNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Nested", classNameOf = NestedClass.class)
        })
    public void nestedNested() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedNestedClass instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedNestedClass.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertNotNull(instance.nested.nested);
        assertEquals("Value1", instance.nested.nested.string1);
        assertEquals("Value2", instance.nested.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMap.class),
            @MCRTestProperty(key = "Foo.EntryA", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryB", classNameOf = OtherKindOfEntry.class)
        })
    public void nestedMap() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedMap instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedMap.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        Entry oneEntry = instance.map.get("EntryA");
        assertNotNull(oneEntry);
        assertEquals(OneKindOfEntry.class, oneEntry.getClass());
        assertEquals("OneValue", oneEntry.get());

        Entry otherEntry = instance.map.get("EntryB");
        assertNotNull(otherEntry);
        assertEquals(OtherKindOfEntry.class, otherEntry.getClass());
        assertEquals("OtherValue", otherEntry.get());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMapOfImplicitEntries.class),
            @MCRTestProperty(key = "Foo.EntryA.Property1", string = "Custom1"),
            @MCRTestProperty(key = "Foo.EntryB.Property2", string = "Custom2"),
        })
    public void nestedMapImplicit() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedMapOfImplicitEntries instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedMapOfImplicitEntries.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        ImplicitEntry oneEntry = instance.map.get("EntryA");
        assertNotNull(oneEntry);
        assertEquals(ImplicitEntry.class, oneEntry.getClass());
        assertEquals("Custom1", oneEntry.string1);
        assertEquals("Value2", oneEntry.string2);

        ImplicitEntry otherEntry = instance.map.get("EntryB");
        assertNotNull(otherEntry);
        assertEquals(ImplicitEntry.class, otherEntry.getClass());
        assertEquals("Value1", otherEntry.string1);
        assertEquals("Custom2", otherEntry.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedList.class),
            @MCRTestProperty(key = "Foo.23", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.42", classNameOf = OtherKindOfEntry.class)
        })
    public void nestedList() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedList instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedList.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        Entry oneEntry = instance.list.getFirst();
        assertNotNull(oneEntry);
        assertEquals(OneKindOfEntry.class, oneEntry.getClass());
        assertEquals("OneValue", oneEntry.get());

        Entry otherEntry = instance.list.get(1);
        assertNotNull(otherEntry);
        assertEquals(OtherKindOfEntry.class, otherEntry.getClass());
        assertEquals("OtherValue", otherEntry.get());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedListOfImplicitEntries.class),
            @MCRTestProperty(key = "Foo.23.Property1", string = "Custom1"),
            @MCRTestProperty(key = "Foo.42.Property2", string = "Custom2"),
        })
    public void nestedListImplicit() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedListOfImplicitEntries instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedListOfImplicitEntries.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        ImplicitEntry oneEntry = instance.list.getFirst();
        assertNotNull(oneEntry);
        assertEquals(ImplicitEntry.class, oneEntry.getClass());
        assertEquals("Custom1", oneEntry.string1);
        assertEquals("Value2", oneEntry.string2);

        ImplicitEntry otherEntry = instance.list.get(1);
        assertNotNull(otherEntry);
        assertEquals(ImplicitEntry.class, otherEntry.getClass());
        assertEquals("Value1", otherEntry.string1);
        assertEquals("Custom2", otherEntry.string2);

    }

    public static class NestedClass {

        @MCRProperty(name = "Property1", defaultName = "Default.Property1")
        public String string1;

        @MCRProperty(name = "Property2", defaultName = "Default.Property2")
        public String string2;

    }

    public static final class ImplicitNestedClass {

        @MCRProperty(name = "Property1", defaultName = "Default.Property1")
        public String string1;

        @MCRProperty(name = "Property2", defaultName = "Default.Property2")
        public String string2;

    }

    public static class TestClassWithNestedClass {

        @MCRInstance(name = "Nested", valueClass = NestedClass.class)
        public NestedClass nested;

    }

    public static class TestClassWithImplicitNestedClass {

        @MCRInstance(name = "Nested", valueClass = ImplicitNestedClass.class)
        public ImplicitNestedClass nested;

    }

    public static class TestClassWithNestedNestedClass {

        @MCRInstance(name = "Nested", valueClass = TestClassWithNestedClass.class)
        public TestClassWithNestedClass nested;

    }

    public static class SimpleEntry {

        @MCRProperty(name = "Property")
        public String string;

    }

    public static final class ImplicitEntry {

        @MCRProperty(name = "Property1", defaultName = "Default.Property1")
        public String string1;

        @MCRProperty(name = "Property2", defaultName = "Default.Property2")
        public String string2;

    }

    public static abstract class Entry {

        public abstract String get();

    }

    public static class OneKindOfEntry extends Entry {

        @MCRProperty(name = "OneProperty", defaultName = "Default.OneProperty")
        public String string;

        @Override
        public String get() {
            return string;
        }

    }

    public static class OtherKindOfEntry extends Entry {

        @MCRProperty(name = "OtherProperty", defaultName = "Default.OtherProperty")
        public String string;

        @Override
        public String get() {
            return string;
        }

    }

    public static class TestClassWithNestedMap {

        @MCRInstanceMap(valueClass = Entry.class)
        public Map<String, Entry> map;

    }

    public static class TestClassWithNestedMapOfImplicitEntries {

        @MCRInstanceMap(valueClass = ImplicitEntry.class)
        public Map<String, ImplicitEntry> map;

    }

    public static class TestClassWithNestedList {

        @MCRInstanceList(valueClass = Entry.class)
        public List<Entry> list;

    }

    public static class TestClassWithNestedListOfImplicitEntries {

        @MCRInstanceList(valueClass = ImplicitEntry.class)
        public List<ImplicitEntry> list;

    }

}

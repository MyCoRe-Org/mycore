/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRProperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MCRConfigurableInstanceHelperNestedTest extends MCRTestCase {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Nested.Property2", string = "Value2")
        }
    )
    public void nested() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedClass instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("Value1", instance.nested.string1);
        assertEquals("Value2", instance.nested.string2);

    }

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedClass.class),
        }
    )
    public void nestedNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(configuration);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Nested.Property2", string = "Value2")
        }
    )
    public void nestedOptional() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedClass instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("Value1", instance.nested.string1);
        assertEquals("Value2", instance.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedClass.class),
        }
    )
    public void nestedOptionalNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedClass instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);
        assertNull(instance.nested);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = TestClassWithNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Nested.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Nested.Nested.Property2", string = "Value2")
        }
    )
    public void nestedNested() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedNestedClass instance = MCRConfigurableInstanceHelper.getInstance(configuration);

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
            @MCRTestProperty(key = "Foo.EntryA.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.EntryB", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryB.OtherProperty", string = "OtherValue"),
        }
    )
    public void nestedMap() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedMap instance = MCRConfigurableInstanceHelper.getInstance(configuration);

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

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMap.class),
        }
    )
    public void nestedMapNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(configuration);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedMap.class),
            @MCRTestProperty(key = "Foo.EntryA", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryA.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.EntryB", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryB.OtherProperty", string = "OtherValue"),
        }
    )
    public void nestedOptionalMap() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedMap instance = MCRConfigurableInstanceHelper.getInstance(configuration);

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
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedMap.class),
        }
    )
    public void nestedOptionalMapNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedMap instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(0, instance.map.size());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMapWithPrefix.class),
            @MCRTestProperty(key = "Foo.Map1.Entry", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.Map1.Entry.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.Map2.Entry", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.Map2.Entry.OtherProperty", string = "OtherValue"),
        }
    )
    public void nestedMapWithPrefix() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedMapWithPrefix instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);
        assertNotNull(instance.map1);
        assertNotNull(instance.map2);
        assertEquals(1, instance.map1.size());
        assertEquals(1, instance.map2.size());

        Entry oneEntry = instance.map1.get("Entry");
        assertNotNull(oneEntry);
        assertEquals(OneKindOfEntry.class, oneEntry.getClass());
        assertEquals("OneValue", oneEntry.get());

        Entry otherEntry = instance.map2.get("Entry");
        assertNotNull(otherEntry);
        assertEquals(OtherKindOfEntry.class, otherEntry.getClass());
        assertEquals("OtherValue", otherEntry.get());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedList.class),
            @MCRTestProperty(key = "Foo.23", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.23.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.42", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.42.OtherProperty", string = "OtherValue"),
        }
    )
    public void nestedList() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedList instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        Entry oneEntry = instance.list.get(0);
        assertNotNull(oneEntry);
        assertEquals(OneKindOfEntry.class, oneEntry.getClass());
        assertEquals("OneValue", oneEntry.get());

        Entry otherEntry = instance.list.get(1);
        assertNotNull(otherEntry);
        assertEquals(OtherKindOfEntry.class, otherEntry.getClass());
        assertEquals("OtherValue", otherEntry.get());

    }

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedList.class),
        }
    )
    public void nestedListNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(configuration);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedList.class),
            @MCRTestProperty(key = "Foo.23", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.23.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.42", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.42.OtherProperty", string = "OtherValue"),
        }
    )
    public void nestedOptionalList() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedList instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        Entry oneEntry = instance.list.get(0);
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
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedList.class),
        }
    )
    public void nestedOptionalListNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedList instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(0, instance.list.size());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedListWithPrefix.class),
            @MCRTestProperty(key = "Foo.List1.23", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.List1.23.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.List2.23", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.List2.23.OtherProperty", string = "OtherValue"),
        }
    )
    public void nestedListWithPrefix() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedListWithPrefix instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);
        assertNotNull(instance.list1);
        assertNotNull(instance.list2);
        assertEquals(1, instance.list1.size());
        assertEquals(1, instance.list2.size());

        Entry oneEntry = instance.list1.get(0);
        assertNotNull(oneEntry);
        assertEquals(OneKindOfEntry.class, oneEntry.getClass());
        assertEquals("OneValue", oneEntry.get());

        Entry otherEntry = instance.list2.get(0);
        assertNotNull(otherEntry);
        assertEquals(OtherKindOfEntry.class, otherEntry.getClass());
        assertEquals("OtherValue", otherEntry.get());

    }

    public static class NestedClass {

        @MCRProperty(name = "Property1")
        public String string1;

        @MCRProperty(name = "Property2")
        public String string2;

    }

    public static class TestClassWithNestedClass {

        @MCRInstance(name = "Nested", valueClass = NestedClass.class)
        public NestedClass nested;

    }

    public static class TestClassWithOptionalNestedClass {

        @MCRInstance(name = "Nested", valueClass = NestedClass.class, required = false)
        public NestedClass nested;

    }

    public static class TestClassWithNestedNestedClass {

        @MCRInstance(name = "Nested", valueClass = TestClassWithNestedClass.class)
        public TestClassWithNestedClass nested;

    }

    public static abstract class Entry {

        public abstract String get();

    }

    public static class OneKindOfEntry extends Entry {

        @MCRProperty(name = "OneProperty")
        public String string;


        @Override
        public String get() {
            return string;
        }

    }

    public static class OtherKindOfEntry extends Entry {

        @MCRProperty(name = "OtherProperty")
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

    public static class TestClassWithOptionalNestedMap {

        @MCRInstanceMap(valueClass = Entry.class, required = false)
        public Map<String, Entry> map;

    }

    public static class TestClassWithNestedMapWithPrefix {

        @MCRInstanceMap(name = "Map1", valueClass = Entry.class)
        public Map<String, Entry> map1;

        @MCRInstanceMap(name = "Map2", valueClass = Entry.class)
        public Map<String, Entry> map2;

    }

    public static class TestClassWithNestedList {

        @MCRInstanceList(valueClass = Entry.class)
        public List<Entry> list;

    }

    public static class TestClassWithOptionalNestedList {

        @MCRInstanceList(valueClass = Entry.class, required = false)
        public List<Entry> list;

    }

    public static class TestClassWithNestedListWithPrefix {

        @MCRInstanceList(name = "List1", valueClass = Entry.class)
        public List<Entry> list1;


        @MCRInstanceList(name = "List2", valueClass = Entry.class)
        public List<Entry> list2;

    }

}

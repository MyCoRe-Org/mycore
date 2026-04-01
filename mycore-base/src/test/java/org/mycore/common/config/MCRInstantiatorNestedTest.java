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
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRInstantiatorNestedTest {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.PropertyA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Nested.PropertyB", string = "ValueB")
        })
    public void nested() {

        TestClassWithNestedClass instance = ofName(TestClassWithNestedClass.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("ValueA", instance.nested.string1);
        assertEquals("ValueB", instance.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedClass.class),
        })
    public void nestedNotPresent() {

        assertThrows(MCRConfigurationException.class,
            () -> MCRInstanceConfiguration.ofName(Object.class, "Foo").instantiate());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithImplicitNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.PropertyA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Nested.PropertyB", string = "ValueB")
        })
    public void nestedImplicit() {

        TestClassWithImplicitNestedClass instance = ofName(TestClassWithImplicitNestedClass.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("ValueA", instance.nested.string1);
        assertEquals("ValueB", instance.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.PropertyA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Nested.PropertyB", string = "ValueB")
        })
    public void nestedOptional() {

        TestClassWithOptionalNestedClass instance = ofName(TestClassWithOptionalNestedClass.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("ValueA", instance.nested.string1);
        assertEquals("ValueB", instance.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedClass.class),
        })
    public void nestedOptionalNotPresent() {

        TestClassWithOptionalNestedClass instance = ofName(TestClassWithOptionalNestedClass.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNull(instance.nested);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = TestClassWithNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Nested.PropertyA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Nested.Nested.PropertyB", string = "ValueB")
        })
    public void nestedNested() {

        TestClassWithNestedNestedClass instance = ofName(TestClassWithNestedNestedClass.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertNotNull(instance.nested.nested);
        assertEquals("ValueA", instance.nested.nested.string1);
        assertEquals("ValueB", instance.nested.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedClassAndSentinel.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.Nested.PropertyA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Nested.PropertyB", string = "ValueB")
        })
    public void nestedOptionalAndSentinel() {

        TestClassWithOptionalNestedClassAndSentinel instance =
            ofName(TestClassWithOptionalNestedClassAndSentinel.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("ValueA", instance.nested.string1);
        assertEquals("ValueB", instance.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedClassAndSentinel.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.Nested.PropertyA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Nested.PropertyB", string = "ValueB")
        })
    public void nestedOptionalAndSentinelNotPresent() {

        TestClassWithOptionalNestedClassAndSentinel instance =
            ofName(TestClassWithOptionalNestedClassAndSentinel.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNull(instance.nested);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMap.class),
            @MCRTestProperty(key = "Foo.EntryA", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryA.PropertyA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.EntryB", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryB.PropertyB", string = "ValueB"),
        })
    public void nestedMap() {

        TestClassWithNestedMap instance = ofName(TestClassWithNestedMap.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        Entry entryA = instance.map.get("EntryA");
        assertNotNull(entryA);
        assertEquals(OneKindOfEntry.class, entryA.getClass());
        assertEquals("ValueA", entryA.get());

        Entry entryB = instance.map.get("EntryB");
        assertNotNull(entryB);
        assertEquals(OtherKindOfEntry.class, entryB.getClass());
        assertEquals("ValueB", entryB.get());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMap.class),
        })
    public void nestedMapNotPresent() {

        assertThrows(MCRConfigurationException.class,
            () -> MCRInstanceConfiguration.ofName(Object.class, "Foo").instantiate());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMapOfImplicitEntries.class),
            @MCRTestProperty(key = "Foo.EntryA.Property", string = "ValueA"),
            @MCRTestProperty(key = "Foo.EntryB.Property", string = "ValueB"),
        })
    public void nestedMapImplicit() {

        TestClassWithNestedMapOfImplicitEntries instance =
            ofName(TestClassWithNestedMapOfImplicitEntries.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        ImplicitEntry entryA = instance.map.get("EntryA");
        assertNotNull(entryA);
        assertEquals(ImplicitEntry.class, entryA.getClass());
        assertEquals("ValueA", entryA.string);

        ImplicitEntry entryB = instance.map.get("EntryB");
        assertNotNull(entryB);
        assertEquals(ImplicitEntry.class, entryB.getClass());
        assertEquals("ValueB", entryB.string);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedMap.class),
            @MCRTestProperty(key = "Foo.EntryA", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryA.PropertyA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.EntryB", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryB.PropertyB", string = "ValueB"),
        })
    public void nestedOptionalMap() {

        TestClassWithOptionalNestedMap instance = ofName(TestClassWithOptionalNestedMap.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        Entry entryA = instance.map.get("EntryA");
        assertNotNull(entryA);
        assertEquals(OneKindOfEntry.class, entryA.getClass());
        assertEquals("ValueA", entryA.get());

        Entry entryB = instance.map.get("EntryB");
        assertNotNull(entryB);
        assertEquals(OtherKindOfEntry.class, entryB.getClass());
        assertEquals("ValueB", entryB.get());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedMap.class),
        })
    public void nestedOptionalMapNotPresent() {

        TestClassWithOptionalNestedMap instance = ofName(TestClassWithOptionalNestedMap.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(0, instance.map.size());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMapWithSentinel.class),
            @MCRTestProperty(key = "Foo.EntryA", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.EntryA.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.EntryA.Property", string = "ValueA"),
            @MCRTestProperty(key = "Foo.EntryB", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.EntryB.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.EntryB.Property", string = "ValueB"),
            @MCRTestProperty(key = "Foo.EntryC", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.EntryC.Property", string = "ValueC"),

        })
    public void nestedMapWithSentinel() {

        TestClassWithNestedMapWithSentinel instance =
            ofName(TestClassWithNestedMapWithSentinel.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        assertNull(instance.map.get("EntryA"));
        assertNotNull(instance.map.get("EntryB"));
        assertEquals("ValueB", instance.map.get("EntryB").string);
        assertNotNull(instance.map.get("EntryC"));
        assertEquals("ValueC", instance.map.get("EntryC").string);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMapsWithPrefix.class),
            @MCRTestProperty(key = "Foo.Map1.Entry", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.Map1.Entry.PropertyA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Map2.Entry", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.Map2.Entry.PropertyB", string = "ValueB"),
        })
    public void nestedMapWithPrefix() {

        TestClassWithNestedMapsWithPrefix instance =
            ofName(TestClassWithNestedMapsWithPrefix.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map1);
        assertNotNull(instance.map2);
        assertEquals(1, instance.map1.size());
        assertEquals(1, instance.map2.size());

        Entry entryA = instance.map1.get("Entry");
        assertNotNull(entryA);
        assertEquals(OneKindOfEntry.class, entryA.getClass());
        assertEquals("ValueA", entryA.get());

        Entry entryB = instance.map2.get("Entry");
        assertNotNull(entryB);
        assertEquals(OtherKindOfEntry.class, entryB.getClass());
        assertEquals("ValueB", entryB.get());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMapWithPrefixAndSentinel.class),
            @MCRTestProperty(key = "Foo.Map.EntryA", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.Map.EntryA.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.Map.EntryA.Property", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Map.EntryB", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.Map.EntryB.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.Map.EntryB.Property", string = "ValueB"),
            @MCRTestProperty(key = "Foo.Map.EntryC", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.Map.EntryC.Property", string = "ValueC"),

        })
    public void nestedMapWithPrefixAndSentinel() {

        TestClassWithNestedMapWithPrefixAndSentinel instance =
            ofName(TestClassWithNestedMapWithPrefixAndSentinel.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        assertNull(instance.map.get("EntryA"));
        assertNotNull(instance.map.get("EntryB"));
        assertEquals("ValueB", instance.map.get("EntryB").string);
        assertNotNull(instance.map.get("EntryC"));
        assertEquals("ValueC", instance.map.get("EntryC").string);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedList.class),
            @MCRTestProperty(key = "Foo.23", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.23.PropertyA", string = "Value23"),
            @MCRTestProperty(key = "Foo.42", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.42.PropertyB", string = "Value42"),
        })
    public void nestedList() {

        TestClassWithNestedList instance = ofName(TestClassWithNestedList.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        Entry entry23 = instance.list.get(0);
        assertNotNull(entry23);
        assertEquals(OneKindOfEntry.class, entry23.getClass());
        assertEquals("Value23", entry23.get());

        Entry entry42 = instance.list.get(1);
        assertNotNull(entry42);
        assertEquals(OtherKindOfEntry.class, entry42.getClass());
        assertEquals("Value42", entry42.get());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedList.class),
        })
    public void nestedListNotPresent() {

        assertThrows(MCRConfigurationException.class,
            () -> MCRInstanceConfiguration.ofName(Object.class, "Foo").instantiate());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedListOfImplicitEntries.class),
            @MCRTestProperty(key = "Foo.23.Property", string = "Value23"),
            @MCRTestProperty(key = "Foo.42.Property", string = "Value42"),
        })
    public void nestedListImplicit() {

        TestClassWithNestedListOfImplicitEntries instance =
            ofName(TestClassWithNestedListOfImplicitEntries.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        ImplicitEntry entry23 = instance.list.get(0);
        assertNotNull(entry23);
        assertEquals(ImplicitEntry.class, entry23.getClass());
        assertEquals("Value23", entry23.string);

        ImplicitEntry entry42 = instance.list.get(1);
        assertNotNull(entry42);
        assertEquals(ImplicitEntry.class, entry42.getClass());
        assertEquals("Value42", entry42.string);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedList.class),
            @MCRTestProperty(key = "Foo.23", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.23.PropertyA", string = "Value23"),
            @MCRTestProperty(key = "Foo.42", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.42.PropertyB", string = "Value42"),
        })
    public void nestedOptionalList() {

        TestClassWithOptionalNestedList instance = ofName(TestClassWithOptionalNestedList.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        Entry entry23 = instance.list.get(0);
        assertNotNull(entry23);
        assertEquals(OneKindOfEntry.class, entry23.getClass());
        assertEquals("Value23", entry23.get());

        Entry entry42 = instance.list.get(1);
        assertNotNull(entry42);
        assertEquals(OtherKindOfEntry.class, entry42.getClass());
        assertEquals("Value42", entry42.get());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedList.class),
        })
    public void nestedOptionalListNotPresent() {

        TestClassWithOptionalNestedList instance = ofName(TestClassWithOptionalNestedList.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(0, instance.list.size());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedListAndSentinel.class),
            @MCRTestProperty(key = "Foo.7", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.7.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.7.Property", string = "Value7"),
            @MCRTestProperty(key = "Foo.23", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.23.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.23.Property", string = "Value23"),
            @MCRTestProperty(key = "Foo.42", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.42.Property", string = "Value42"),

        })
    public void nestedListWithSentinel() {

        TestClassWithNestedListAndSentinel instance =
            ofName(TestClassWithNestedListAndSentinel.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        assertEquals("Value23", instance.list.get(0).string);
        assertEquals("Value42", instance.list.get(1).string);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedListsWithPrefix.class),
            @MCRTestProperty(key = "Foo.List1.23", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.List1.23.PropertyA", string = "Value23"),
            @MCRTestProperty(key = "Foo.List2.42", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.List2.42.PropertyB", string = "Value42"),
        })
    public void nestedListWithPrefix() {

        TestClassWithNestedListsWithPrefix instance =
            ofName(TestClassWithNestedListsWithPrefix.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list1);
        assertNotNull(instance.list2);
        assertEquals(1, instance.list1.size());
        assertEquals(1, instance.list2.size());

        Entry entry23 = instance.list1.get(0);
        assertNotNull(entry23);
        assertEquals(OneKindOfEntry.class, entry23.getClass());
        assertEquals("Value23", entry23.get());

        Entry entry42 = instance.list2.get(0);
        assertNotNull(entry42);
        assertEquals(OtherKindOfEntry.class, entry42.getClass());
        assertEquals("Value42", entry42.get());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedListWithPrefixAndSentinel.class),
            @MCRTestProperty(key = "Foo.List.7", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.List.7.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.List.7.Property", string = "Value7"),
            @MCRTestProperty(key = "Foo.List.23", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.List.23.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.List.23.Property", string = "Value23"),
            @MCRTestProperty(key = "Foo.List.42", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.List.42.Property", string = "Value42"),
        })
    public void nestedListWithPrefixAndSentinel() {

        TestClassWithNestedListWithPrefixAndSentinel instance =
            ofName(TestClassWithNestedListWithPrefixAndSentinel.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        assertEquals("Value23", instance.list.get(0).string);
        assertEquals("Value42", instance.list.get(1).string);

    }

    public static class NestedClass {

        @MCRProperty(name = "PropertyA")
        public String string1;

        @MCRProperty(name = "PropertyB")
        public String string2;

    }

    public static final class ImplicitNestedClass {

        @MCRProperty(name = "PropertyA")
        public String string1;

        @MCRProperty(name = "PropertyB")
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

    public static class TestClassWithOptionalNestedClass {

        @MCRInstance(name = "Nested", valueClass = NestedClass.class, required = false)
        public NestedClass nested;

    }

    public static class TestClassWithNestedNestedClass {

        @MCRInstance(name = "Nested", valueClass = TestClassWithNestedClass.class)
        public TestClassWithNestedClass nested;

    }

    public static class TestClassWithOptionalNestedClassAndSentinel {

        @MCRInstance(name = "Nested", valueClass = NestedClass.class, required = false,
            sentinel = @MCRSentinel(name = "Sentinel"))
        public NestedClass nested;

    }

    public static class SimpleEntry {

        @MCRProperty(name = "Property")
        public String string;

    }

    public static final class ImplicitEntry {

        @MCRProperty(name = "Property")
        public String string;

    }

    public static abstract class Entry {

        public abstract String get();

    }

    public static class OneKindOfEntry extends Entry {

        @MCRProperty(name = "PropertyA")
        public String string;

        @Override
        public String get() {
            return string;
        }

    }

    public static class OtherKindOfEntry extends Entry {

        @MCRProperty(name = "PropertyB")
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

    public static class TestClassWithOptionalNestedMap {

        @MCRInstanceMap(valueClass = Entry.class, required = false)
        public Map<String, Entry> map;

    }

    public static class TestClassWithNestedMapWithSentinel {

        @MCRInstanceMap(valueClass = SimpleEntry.class, sentinel = @MCRSentinel(name = "Sentinel"))
        public Map<String, SimpleEntry> map;

    }

    public static class TestClassWithNestedMapsWithPrefix {

        @MCRInstanceMap(name = "Map1", valueClass = Entry.class)
        public Map<String, Entry> map1;

        @MCRInstanceMap(name = "Map2", valueClass = Entry.class)
        public Map<String, Entry> map2;

    }

    public static class TestClassWithNestedMapWithPrefixAndSentinel {

        @MCRInstanceMap(name = "Map", valueClass = SimpleEntry.class, sentinel = @MCRSentinel(name = "Sentinel"))
        public Map<String, SimpleEntry> map;

    }

    public static class TestClassWithNestedList {

        @MCRInstanceList(valueClass = Entry.class)
        public List<Entry> list;

    }

    public static class TestClassWithNestedListOfImplicitEntries {

        @MCRInstanceList(valueClass = ImplicitEntry.class)
        public List<ImplicitEntry> list;

    }

    public static class TestClassWithOptionalNestedList {

        @MCRInstanceList(valueClass = Entry.class, required = false)
        public List<Entry> list;

    }

    public static class TestClassWithNestedListAndSentinel {

        @MCRInstanceList(valueClass = SimpleEntry.class, sentinel = @MCRSentinel(name = "Sentinel"))
        public List<SimpleEntry> list;

    }

    public static class TestClassWithNestedListsWithPrefix {

        @MCRInstanceList(name = "List1", valueClass = Entry.class)
        public List<Entry> list1;

        @MCRInstanceList(name = "List2", valueClass = Entry.class)
        public List<Entry> list2;

    }

    public static class TestClassWithNestedListWithPrefixAndSentinel {

        @MCRInstanceList(name = "List", valueClass = SimpleEntry.class, sentinel = @MCRSentinel(name = "Sentinel"))
        public List<SimpleEntry> list;

    }

}

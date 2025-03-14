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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import org.mycore.common.config.annotation.MCRSentinel;

public class MCRConfigurableInstanceHelperNestedTest extends MCRTestCase {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Nested.Property2", string = "Value2")
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

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedClass.class),
        })
    public void nestedNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(Object.class, configuration);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithFinalNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Nested.Property2", string = "Value2")
        })
    public void nestedFinal() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithFinalNestedClass instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithFinalNestedClass.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("Value1", instance.nested.string1);
        assertEquals("Value2", instance.nested.string2);

    }


    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedClass.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Nested.Property2", string = "Value2")
        })
    public void nestedOptional() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedClass instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithOptionalNestedClass.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("Value1", instance.nested.string1);
        assertEquals("Value2", instance.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedClass.class),
        })
    public void nestedOptionalNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedClass instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithOptionalNestedClass.class, configuration);

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
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedClassAndSentinel.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.Nested.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Nested.Property2", string = "Value2")
        })
    public void nestedOptionalAndSentinel() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedClassAndSentinel instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithOptionalNestedClassAndSentinel.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.nested);
        assertEquals("Value1", instance.nested.string1);
        assertEquals("Value2", instance.nested.string2);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedClassAndSentinel.class),
            @MCRTestProperty(key = "Foo.Nested", classNameOf = NestedClass.class),
            @MCRTestProperty(key = "Foo.Nested.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.Nested.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Nested.Property2", string = "Value2")
        })
    public void nestedOptionalAndSentinelNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedClassAndSentinel instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithOptionalNestedClassAndSentinel.class, configuration);

        assertNotNull(instance);
        assertNull(instance.nested);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMap.class),
            @MCRTestProperty(key = "Foo.EntryA", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryA.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.EntryB", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryB.OtherProperty", string = "OtherValue"),
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

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMap.class),
        })
    public void nestedMapNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(Object.class, configuration);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMapOfFinalEntries.class),
            @MCRTestProperty(key = "Foo.EntryA.Property", string = "A"),
            @MCRTestProperty(key = "Foo.EntryB.Property", string = "B"),
        })
    public void nestedMapFinal() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedMapOfFinalEntries instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedMapOfFinalEntries.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        FinalEntry oneEntry = instance.map.get("EntryA");
        assertNotNull(oneEntry);
        assertEquals(FinalEntry.class, oneEntry.getClass());
        assertEquals("A", oneEntry.string);

        FinalEntry otherEntry = instance.map.get("EntryB");
        assertNotNull(otherEntry);
        assertEquals(FinalEntry.class, otherEntry.getClass());
        assertEquals("B", otherEntry.string);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedMap.class),
            @MCRTestProperty(key = "Foo.EntryA", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryA.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.EntryB", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.EntryB.OtherProperty", string = "OtherValue"),
        })
    public void nestedOptionalMap() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedMap instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithOptionalNestedMap.class, configuration);

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
        })
    public void nestedOptionalMapNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedMap instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithOptionalNestedMap.class, configuration);

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
            @MCRTestProperty(key = "Foo.EntryA.Property", string = "A"),
            @MCRTestProperty(key = "Foo.EntryB", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.EntryB.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.EntryB.Property", string = "B"),
            @MCRTestProperty(key = "Foo.EntryC", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.EntryC.Property", string = "C"),

        })
    public void nestedMapWithSentinel() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedMapWithSentinel instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedMapWithSentinel.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        assertNull(instance.map.get("EntryA"));
        assertNotNull(instance.map.get("EntryB"));
        assertEquals("B", instance.map.get("EntryB").string);
        assertNotNull(instance.map.get("EntryC"));
        assertEquals("C", instance.map.get("EntryC").string);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMapWithPrefix.class),
            @MCRTestProperty(key = "Foo.Map1.Entry", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.Map1.Entry.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.Map2.Entry", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.Map2.Entry.OtherProperty", string = "OtherValue"),
        })
    public void nestedMapWithPrefix() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedMapWithPrefix instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedMapWithPrefix.class, configuration);

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
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedMapWithPrefixAndSentinel.class),
            @MCRTestProperty(key = "Foo.Map1.EntryA", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.Map1.EntryA.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.Map1.EntryA.Property", string = "A"),
            @MCRTestProperty(key = "Foo.Map1.EntryB", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.Map1.EntryB.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.Map1.EntryB.Property", string = "B"),
            @MCRTestProperty(key = "Foo.Map1.EntryC", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.Map1.EntryC.Property", string = "C"),

        })
    public void nestedMapWithPrefixAndSentinel() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedMapWithPrefixAndSentinel instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedMapWithPrefixAndSentinel.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        assertNull(instance.map.get("EntryA"));
        assertNotNull(instance.map.get("EntryB"));
        assertEquals("B", instance.map.get("EntryB").string);
        assertNotNull(instance.map.get("EntryC"));
        assertEquals("C", instance.map.get("EntryC").string);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedList.class),
            @MCRTestProperty(key = "Foo.23", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.23.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.42", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.42.OtherProperty", string = "OtherValue"),
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

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedList.class),
        })
    public void nestedListNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(Object.class, configuration);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedListOfFinalEntries.class),
            @MCRTestProperty(key = "Foo.23.Property", string = "23"),
            @MCRTestProperty(key = "Foo.42.Property", string = "42"),
        })
    public void nestedListFinal() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedListOfFinalEntries instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedListOfFinalEntries.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        FinalEntry oneEntry = instance.list.getFirst();
        assertNotNull(oneEntry);
        assertEquals(FinalEntry.class, oneEntry.getClass());
        assertEquals("23", oneEntry.string);

        FinalEntry otherEntry = instance.list.get(1);
        assertNotNull(otherEntry);
        assertEquals(FinalEntry.class, otherEntry.getClass());
        assertEquals("42", otherEntry.string);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedList.class),
            @MCRTestProperty(key = "Foo.23", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.23.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.42", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.42.OtherProperty", string = "OtherValue"),
        })
    public void nestedOptionalList() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedList instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithOptionalNestedList.class, configuration);

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
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalNestedList.class),
        })
    public void nestedOptionalListNotPresent() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithOptionalNestedList instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithOptionalNestedList.class, configuration);

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
            @MCRTestProperty(key = "Foo.7.Property", string = "7"),
            @MCRTestProperty(key = "Foo.23", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.23.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.23.Property", string = "23"),
            @MCRTestProperty(key = "Foo.42", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.42.Property", string = "42"),

        })
    public void nestedListWithSentinel() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedListAndSentinel instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedListAndSentinel.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        assertEquals("23", instance.list.get(0).string);
        assertEquals("42", instance.list.get(1).string);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedListWithPrefix.class),
            @MCRTestProperty(key = "Foo.List1.23", classNameOf = OneKindOfEntry.class),
            @MCRTestProperty(key = "Foo.List1.23.OneProperty", string = "OneValue"),
            @MCRTestProperty(key = "Foo.List2.23", classNameOf = OtherKindOfEntry.class),
            @MCRTestProperty(key = "Foo.List2.23.OtherProperty", string = "OtherValue"),
        })
    public void nestedListWithPrefix() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedListWithPrefix instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedListWithPrefix.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.list1);
        assertNotNull(instance.list2);
        assertEquals(1, instance.list1.size());
        assertEquals(1, instance.list2.size());

        Entry oneEntry = instance.list1.getFirst();
        assertNotNull(oneEntry);
        assertEquals(OneKindOfEntry.class, oneEntry.getClass());
        assertEquals("OneValue", oneEntry.get());

        Entry otherEntry = instance.list2.getFirst();
        assertNotNull(otherEntry);
        assertEquals(OtherKindOfEntry.class, otherEntry.getClass());
        assertEquals("OtherValue", otherEntry.get());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithNestedListWithPrefixAndSentinel.class),
            @MCRTestProperty(key = "Foo.List1.7", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.List1.7.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.List1.7.Property", string = "7"),
            @MCRTestProperty(key = "Foo.List1.23", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.List1.23.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.List1.23.Property", string = "23"),
            @MCRTestProperty(key = "Foo.List1.42", classNameOf = SimpleEntry.class),
            @MCRTestProperty(key = "Foo.List1.42.Property", string = "42"),
        })
    public void nestedListWithPrefixAndSentinel() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithNestedListWithPrefixAndSentinel instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithNestedListWithPrefixAndSentinel.class, configuration);

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        assertEquals("23", instance.list.get(0).string);
        assertEquals("42", instance.list.get(1).string);

    }

    public static class NestedClass {

        @MCRProperty(name = "Property1")
        public String string1;

        @MCRProperty(name = "Property2")
        public String string2;

    }

    public static final class FinalNestedClass {

        @MCRProperty(name = "Property1")
        public String string1;

        @MCRProperty(name = "Property2")
        public String string2;

    }

    public static class TestClassWithNestedClass {

        @MCRInstance(name = "Nested", valueClass = NestedClass.class)
        public NestedClass nested;

    }

    public static class TestClassWithFinalNestedClass {

        @MCRInstance(name = "Nested", valueClass = FinalNestedClass.class)
        public FinalNestedClass nested;

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

    public static final class FinalEntry {

        @MCRProperty(name = "Property")
        public String string;

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

    public static class TestClassWithNestedMapOfFinalEntries {

        @MCRInstanceMap(valueClass = FinalEntry.class)
        public Map<String, FinalEntry> map;

    }

    public static class TestClassWithOptionalNestedMap {

        @MCRInstanceMap(valueClass = Entry.class, required = false)
        public Map<String, Entry> map;

    }

    public static class TestClassWithNestedMapWithSentinel {

        @MCRInstanceMap(valueClass = SimpleEntry.class, sentinel = @MCRSentinel(name = "Sentinel"))
        public Map<String, SimpleEntry> map;

    }

    public static class TestClassWithNestedMapWithPrefix {

        @MCRInstanceMap(name = "Map1", valueClass = Entry.class)
        public Map<String, Entry> map1;

        @MCRInstanceMap(name = "Map2", valueClass = Entry.class)
        public Map<String, Entry> map2;

    }

    public static class TestClassWithNestedMapWithPrefixAndSentinel {

        @MCRInstanceMap(name = "Map1", valueClass = SimpleEntry.class, sentinel = @MCRSentinel(name = "Sentinel"))
        public Map<String, SimpleEntry> map;

    }

    public static class TestClassWithNestedList {

        @MCRInstanceList(valueClass = Entry.class)
        public List<Entry> list;

    }

    public static class TestClassWithNestedListOfFinalEntries {

        @MCRInstanceList(valueClass = FinalEntry.class)
        public List<FinalEntry> list;

    }

    public static class TestClassWithOptionalNestedList {

        @MCRInstanceList(valueClass = Entry.class, required = false)
        public List<Entry> list;

    }

    public static class TestClassWithNestedListAndSentinel {

        @MCRInstanceList(valueClass = SimpleEntry.class, sentinel = @MCRSentinel(name = "Sentinel"))
        public List<SimpleEntry> list;

    }

    public static class TestClassWithNestedListWithPrefix {

        @MCRInstanceList(name = "List1", valueClass = Entry.class)
        public List<Entry> list1;

        @MCRInstanceList(name = "List2", valueClass = Entry.class)
        public List<Entry> list2;

    }

    public static class TestClassWithNestedListWithPrefixAndSentinel {

        @MCRInstanceList(name = "List1", valueClass = SimpleEntry.class, sentinel = @MCRSentinel(name = "Sentinel"))
        public List<SimpleEntry> list;

    }

}

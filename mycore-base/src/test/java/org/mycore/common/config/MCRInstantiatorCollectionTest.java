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
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.IMPLICIT_OPTION;
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.ofName;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRPropertyList;
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRInstantiatorCollectionTest {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMap.class),
            @MCRTestProperty(key = "Foo.EntryA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.EntryB", string = "ValueB"),
        })
    public void map() {

        TestClassWithMap instance = ofName(TestClassWithMap.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        String valueA = instance.map.get("EntryA");
        assertNotNull(valueA);
        assertEquals("ValueA", valueA);

        String valueB = instance.map.get("EntryB");
        assertNotNull(valueB);
        assertEquals("ValueB", valueB);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMap.class),
        })
    public void mapNotPresent() {

        assertThrows(MCRConfigurationException.class,
            () -> MCRInstanceConfiguration.ofName(Object.class, "Foo").instantiate());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalMap.class),
            @MCRTestProperty(key = "Foo.EntryA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.EntryB", string = "ValueB"),
        })
    public void optionalMap() {

        TestClassWithOptionalMap instance = ofName(TestClassWithOptionalMap.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        String valueA = instance.map.get("EntryA");
        assertNotNull(valueA);
        assertEquals("ValueA", valueA);

        String valueB = instance.map.get("EntryB");
        assertNotNull(valueB);
        assertEquals("ValueB", valueB);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalMap.class),
        })
    public void optionalMapNotPresent() {

        TestClassWithOptionalMap instance = ofName(TestClassWithOptionalMap.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(0, instance.map.size());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMapWithSentinel.class),
            @MCRTestProperty(key = "Foo.EntryA.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.EntryA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.EntryB.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.EntryB", string = "ValueB"),
            @MCRTestProperty(key = "Foo.EntryC", string = "ValueC"),
        })
    public void mapWithSentinel() {

        TestClassWithMapWithSentinel instance = ofName(TestClassWithMapWithSentinel.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        assertNull(instance.map.get("EntryA"));
        assertNotNull(instance.map.get("EntryB"));
        assertEquals("ValueB", instance.map.get("EntryB"));
        assertNotNull(instance.map.get("EntryC"));
        assertEquals("ValueC", instance.map.get("EntryC"));

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMapsWithPrefix.class),
            @MCRTestProperty(key = "Foo.Map1.Entry", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Map2.Entry", string = "ValueB"),
        })
    public void mapWithPrefix() {

        TestClassWithMapsWithPrefix instance = ofName(TestClassWithMapsWithPrefix.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map1);
        assertNotNull(instance.map2);
        assertEquals(1, instance.map1.size());
        assertEquals(1, instance.map2.size());

        String valueA = instance.map1.get("Entry");
        assertNotNull(valueA);
        assertEquals("ValueA", valueA);

        String valueB = instance.map2.get("Entry");
        assertNotNull(valueB);
        assertEquals("ValueB", valueB);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMapWithPrefix.class),
            @MCRTestProperty(key = "Foo.Map", string = "EntryA:ValueA,EntryB:ValueB"),
        })
    public void shortFormMapWithPrefix() {

        TestClassWithMapWithPrefix instance = ofName(TestClassWithMapWithPrefix.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        String valueA = instance.map.get("EntryA");
        assertNotNull(valueA);
        assertEquals("ValueA", valueA);

        String valueB = instance.map.get("EntryB");
        assertNotNull(valueB);
        assertEquals("ValueB", valueB);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMapWithPrefix.class),
            @MCRTestProperty(key = "Foo.Map", string = "A:ShortValueA,B:ShortValueB"),
            @MCRTestProperty(key = "Foo.Map.EntryA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Map.EntryB", string = "ValueB"),
        })
    public void mixedFormMapWithPrefix() {

        TestClassWithMapWithPrefix instance = ofName(TestClassWithMapWithPrefix.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(4, instance.map.size());

        String valueA = instance.map.get("EntryA");
        assertNotNull(valueA);
        assertEquals("ValueA", valueA);

        String shortValueA = instance.map.get("A");
        assertNotNull(shortValueA);
        assertEquals("ShortValueA", shortValueA);

        String shortValueB = instance.map.get("B");
        assertNotNull(shortValueB);
        assertEquals("ShortValueB", shortValueB);

        String valueB = instance.map.get("EntryB");
        assertNotNull(valueB);
        assertEquals("ValueB", valueB);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", string = "EntryA:ValueA,EntryB:ValueB"),
        })
    public void shortFormMapWithoutPrefix() {

        TestClassWithMapWithoutPrefix instance = ofName(TestClassWithMapWithoutPrefix.class, "Foo.Class",
            IMPLICIT_OPTION).instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        String valueA = instance.map.get("EntryA");
        assertNotNull(valueA);
        assertEquals("ValueA", valueA);

        String valueB = instance.map.get("EntryB");
        assertNotNull(valueB);
        assertEquals("ValueB", valueB);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMapWithPrefix.class),
            @MCRTestProperty(key = "Foo.Map", string = "A:ShortValueA,B"),
            @MCRTestProperty(key = "Foo.Map.EntryA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Map.EntryB", empty = true),
        })
    public void mapWithEmptyMapValues() {

        TestClassWithMapWithPrefix instance = ofName(TestClassWithMapWithPrefix.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        String valueA = instance.map.get("EntryA");
        assertNotNull(valueA);
        assertEquals("ValueA", valueA);

        String shortValueA = instance.map.get("A");
        assertNotNull(shortValueA);
        assertEquals("ShortValueA", shortValueA);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMapWithPrefixAndSentinel.class),
            @MCRTestProperty(key = "Foo.Map.EntryA.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.Map.EntryA", string = "ValueA"),
            @MCRTestProperty(key = "Foo.Map.EntryB.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.Map.EntryB", string = "ValueB"),
            @MCRTestProperty(key = "Foo.Map.EntryC", string = "ValueC"),
        })
    public void mapWithPrefixAndSentinel() {

        TestClassWithMapWithPrefixAndSentinel instance =
            ofName(TestClassWithMapWithPrefixAndSentinel.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.map);
        assertEquals(2, instance.map.size());

        assertNull(instance.map.get("EntryA"));
        assertNotNull(instance.map.get("EntryB"));
        assertEquals("ValueB", instance.map.get("EntryB"));
        assertNotNull(instance.map.get("EntryC"));
        assertEquals("ValueC", instance.map.get("EntryC"));

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithList.class),
            @MCRTestProperty(key = "Foo.23", string = "Value23"),
            @MCRTestProperty(key = "Foo.42", string = "Value42"),
        })
    public void list() {

        TestClassWithList instance = ofName(TestClassWithList.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        String value23 = instance.list.get(0);
        assertNotNull(value23);
        assertEquals("Value23", value23);

        String value42 = instance.list.get(1);
        assertNotNull(value42);
        assertEquals("Value42", value42);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithList.class),
        })
    public void listNotPresent() {

        assertThrows(MCRConfigurationException.class,
            () -> MCRInstanceConfiguration.ofName(Object.class, "Foo").instantiate());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalList.class),
            @MCRTestProperty(key = "Foo.23", string = "Value23"),
            @MCRTestProperty(key = "Foo.42", string = "Value42"),
        })
    public void optionalList() {

        TestClassWithOptionalList instance = ofName(TestClassWithOptionalList.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        String value23 = instance.list.get(0);
        assertNotNull(value23);
        assertEquals("Value23", value23);

        String value42 = instance.list.get(1);
        assertNotNull(value42);
        assertEquals("Value42", value42);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithOptionalList.class),
        })
    public void optionalListNotPresent() {

        TestClassWithOptionalList instance = ofName(TestClassWithOptionalList.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(0, instance.list.size());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithListAndSentinel.class),
            @MCRTestProperty(key = "Foo.7.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.7", string = "Value7"),
            @MCRTestProperty(key = "Foo.23.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.23", string = "Value23"),
            @MCRTestProperty(key = "Foo.42", string = "Value42"),

        })
    public void listWithSentinel() {

        TestClassWithListAndSentinel instance = ofName(TestClassWithListAndSentinel.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        assertEquals("Value23", instance.list.get(0));
        assertEquals("Value42", instance.list.get(1));

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithListsWithPrefix.class),
            @MCRTestProperty(key = "Foo.List1.23", string = "Value23"),
            @MCRTestProperty(key = "Foo.List2.42", string = "Value42"),
        })
    public void listWithPrefix() {

        TestClassWithListsWithPrefix instance = ofName(TestClassWithListsWithPrefix.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list1);
        assertNotNull(instance.list2);
        assertEquals(1, instance.list1.size());
        assertEquals(1, instance.list2.size());

        String value23 = instance.list1.get(0);
        assertNotNull(value23);
        assertEquals("Value23", value23);

        String value42 = instance.list2.get(0);
        assertNotNull(value42);
        assertEquals("Value42", value42);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithListWithPrefix.class),
            @MCRTestProperty(key = "Foo.List", string = "ShortValue23,ShortValue42"),
        })
    public void shortFormListWithPrefix() {

        TestClassWithListWithPrefix instance = ofName(TestClassWithListWithPrefix.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        String shortValue23 = instance.list.get(0);
        assertNotNull(shortValue23);
        assertEquals("ShortValue23", shortValue23);

        String shortValue42 = instance.list.get(1);
        assertNotNull(shortValue42);
        assertEquals("ShortValue42", shortValue42);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithListWithPrefix.class),
            @MCRTestProperty(key = "Foo.List", string = "ShortValue23,ShortValue42"),
            @MCRTestProperty(key = "Foo.List.42", string = "AfterValue42"),
            @MCRTestProperty(key = "Foo.List.-23", string = "BeforeValue23"),
        })
    public void mixedFormListWithPrefix() {

        TestClassWithListWithPrefix instance = ofName(TestClassWithListWithPrefix.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(4, instance.list.size());

        String beforeValue23 = instance.list.get(0);
        assertNotNull(beforeValue23);
        assertEquals("BeforeValue23", beforeValue23);

        String shortValue23 = instance.list.get(1);
        assertNotNull(shortValue23);
        assertEquals("ShortValue23", shortValue23);

        String shortValue42 = instance.list.get(2);
        assertNotNull(shortValue42);
        assertEquals("ShortValue42", shortValue42);

        String afterValue42 = instance.list.get(3);
        assertNotNull(afterValue42);
        assertEquals("AfterValue42", afterValue42);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", string = "ShortValue23,ShortValue42"),
        })
    public void shortFormListWithoutPrefix() {

        TestClassWithListWithoutPrefix instance = ofName(TestClassWithListWithoutPrefix.class, "Foo.Class",
            IMPLICIT_OPTION).instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        String shortValue23 = instance.list.get(0);
        assertNotNull(shortValue23);
        assertEquals("ShortValue23", shortValue23);

        String shortValue42 = instance.list.get(1);
        assertNotNull(shortValue42);
        assertEquals("ShortValue42", shortValue42);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithListWithPrefix.class),
            @MCRTestProperty(key = "Foo.List", string = "ShortValue23,"),
            @MCRTestProperty(key = "Foo.List.42", string = "AfterValue42"),
            @MCRTestProperty(key = "Foo.List.-23", empty = true),
        })
    public void listWithEmptyValues() {

        TestClassWithListWithPrefix instance = ofName(TestClassWithListWithPrefix.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        String beforeValue23 = instance.list.get(0);
        assertNotNull(beforeValue23);
        assertEquals("ShortValue23", beforeValue23);

        String shortValue23 = instance.list.get(1);
        assertNotNull(shortValue23);
        assertEquals("AfterValue42", shortValue23);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithListWithPrefixAndSentinel.class),
            @MCRTestProperty(key = "Foo.List.7.Sentinel", string = "false"),
            @MCRTestProperty(key = "Foo.List.7", string = "Value7"),
            @MCRTestProperty(key = "Foo.List.23.Sentinel", string = "true"),
            @MCRTestProperty(key = "Foo.List.23", string = "Value23"),
            @MCRTestProperty(key = "Foo.List.42", string = "Value42"),
        })
    public void listWithPrefixAndSentinel() {

        TestClassWithListWithPrefixAndSentinel instance =
            ofName(TestClassWithListWithPrefixAndSentinel.class, "Foo").instantiate();

        assertNotNull(instance);
        assertNotNull(instance.list);
        assertEquals(2, instance.list.size());

        assertEquals("Value23", instance.list.get(0));
        assertEquals("Value42", instance.list.get(1));

    }

    public static class TestClassWithMap {

        @MCRPropertyMap
        public Map<String, String> map;

    }

    public static class TestClassWithOptionalMap {

        @MCRPropertyMap(required = false)
        public Map<String, String> map;

    }

    public static class TestClassWithMapWithSentinel {

        @MCRPropertyMap(sentinel = @MCRSentinel(name = "Sentinel"))
        public Map<String, String> map;

    }

    public static class TestClassWithMapsWithPrefix {

        @MCRPropertyMap(name = "Map1")
        public Map<String, String> map1;

        @MCRPropertyMap(name = "Map2")
        public Map<String, String> map2;

    }

    public static class TestClassWithMapWithPrefix {

        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;

    }

    public static class TestClassWithMapWithPrefixAndSentinel {

        @MCRPropertyMap(name = "Map", sentinel = @MCRSentinel(name = "Sentinel"))
        public Map<String, String> map;

    }

    public static final class TestClassWithMapWithoutPrefix {

        @MCRPropertyMap
        public Map<String, String> map;

    }

    public static class TestClassWithList {

        @MCRPropertyList
        public List<String> list;

    }

    public static class TestClassWithOptionalList {

        @MCRPropertyList(required = false)
        public List<String> list;

    }

    public static class TestClassWithListAndSentinel {

        @MCRPropertyList(sentinel = @MCRSentinel(name = "Sentinel"))
        public List<String> list;

    }

    public static class TestClassWithListsWithPrefix {

        @MCRPropertyList(name = "List1")
        public List<String> list1;

        @MCRPropertyList(name = "List2")
        public List<String> list2;

    }

    public static class TestClassWithListWithPrefix {

        @MCRPropertyList(name = "List")
        public List<String> list;

    }

    public static class TestClassWithListWithPrefixAndSentinel {

        @MCRPropertyList(name = "List", sentinel = @MCRSentinel(name = "Sentinel"))
        public List<String> list;

    }

    public static final class TestClassWithListWithoutPrefix {

        @MCRPropertyList
        public List<String> list;

    }

}

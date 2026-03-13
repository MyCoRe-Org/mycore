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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRPropertyList;
import org.mycore.test.MyCoReTest;

/**
 * Programmatically created exhaustive list of tests for the following conditions:
 * <ol>
 *   <li>Annotation has <code>required = false</code> or not</li>
 *   <li>Configuration property for instance value is not set, set empty in short form,
 *   set non-empty in short form or set non-empty in long form</li>
 *   <li>Annotation has <code>defaultName = "..."</code> or not</li>
 *   <li>Configuration property for default value is not set, set empty in short form,
 *   set non-empty in short form or set non-empty in long form </li>
 * </ol>
 * <table style="border-collapse: collapse;">
 *   <tr>
 *     <th style="border: 1px solid;">Is Required</th>
 *     <th style="border: 1px solid;">Property</th>
 *     <th style="border: 1px solid;">Has Default</th>
 *     <th style="border: 1px solid;">Default Property</th>
 *     <th style="border: 1px solid;">Expected Result</th>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X=</code></td>
 *     <td style="border: 1px solid;"><code>[]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X=y,z</code></td>
 *     <td style="border: 1px solid;"><code>[y, z]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X.1=y</code>, <code>X.2=z</code></td>
 *     <td style="border: 1px solid;"><code>[y, z]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;"><code>A=</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;"><code>A=b,c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[b, c]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;"><code>A.1=b</code>, <code>A.2=c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[b, c]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X=</code></td>
 *     <td style="border: 1px solid;"><code>[]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X=y,z</code></td>
 *     <td style="border: 1px solid;"><code>[y, z]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X.1=y</code>, <code>X.2=z</code></td>
 *     <td style="border: 1px solid;"><code>[y, z]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>A=</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>A=b,c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[b, c]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>A.1=b</code>, <code>A.2=c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[b, c]</code></td>
 *   </tr>
 * </table>
 */
@MyCoReTest
public class MCRConfigurableInstancePropertyListTest {

    private static final List<String> EMPTY = List.of();

    private static final List<String> DEFAULT_MAP = List.of("DefaultValue");

    private static final List<String> MAP = List.of("Value");

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing.class),
    })
    public void notRequired_PropertyMissing_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequired_PropertyMissing_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequired_PropertyMissing_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequired_PropertyMissing_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultSetDefaultPropertyMissing.class),
    })
    public void notRequired_PropertyMissing_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultSetDefaultPropertyMissing.class, configuration);
        });
        assertEquals("Missing default configuration entries like: MCR.List, MCR.List.1, MCR.List.2, ...",
            exception.getMessage());
    }

    public static class NotRequiredPropertyMissingDefaultSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequired_PropertyMissing_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequired_PropertyMissing_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(DEFAULT_MAP, instance.list);
    }

    public static class NotRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequired_PropertyMissing_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(DEFAULT_MAP, instance.list);
    }

    public static class NotRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequired_PropertyShortEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequired_PropertyShortEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultNotSetDefaultPropertyMissing.class),
    })
    public void required_PropertyMissing_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultNotSetDefaultPropertyMissing.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.List, Foo.List.1, Foo.List.2, ...",
            exception.getMessage());
    }

    public static class RequiredPropertyMissingDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void required_PropertyMissing_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.List, Foo.List.1, Foo.List.2, ...",
            exception.getMessage());
    }

    public static class RequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void required_PropertyMissing_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.List, Foo.List.1, Foo.List.2, ...",
            exception.getMessage());
    }

    public static class RequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void required_PropertyMissing_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.List, Foo.List.1, Foo.List.2, ...",
            exception.getMessage());
    }

    public static class RequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultSetDefaultPropertyMissing.class),
    })
    public void required_PropertyMissing_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultSetDefaultPropertyMissing.class, configuration);
        });
        assertEquals("Missing default configuration entries like: MCR.List, MCR.List.1, MCR.List.2, ...",
            exception.getMessage());
    }

    public static class RequiredPropertyMissingDefaultSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void required_PropertyMissing_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class RequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void required_PropertyMissing_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(DEFAULT_MAP, instance.list);
    }

    public static class RequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void required_PropertyMissing_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(DEFAULT_MAP, instance.list);
    }

    public static class RequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void required_PropertyShortEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class RequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void required_PropertyShortEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    public static class RequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void required_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void required_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void required_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void required_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void required_PropertyShortNotEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void required_PropertyShortNotEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void required_PropertyShortNotEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void required_PropertyShortNotEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void required_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void required_PropertyLongNotEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

}

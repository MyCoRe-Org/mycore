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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRPropertyMap;
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
 *     <td style="border: 1px solid;"><code>{}</code></td>
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
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X=Y:y,Z:z</code></td>
 *     <td style="border: 1px solid;"><code>{Y=y, Z=z}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X.Y=y</code>, <code>X.Z=z</code></td>
 *     <td style="border: 1px solid;"><code>{Y=y, Z=z}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;"><code>A=</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;"><code>A=B:b,C:c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{A=a, B=b}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;"><code>A.B=b</code>, <code>A.C=c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{A=a, B=b}</code></td>
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
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X=Y:y,Z:z</code></td>
 *     <td style="border: 1px solid;"><code>{Y=y, Z=z}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X.Y=y</code>, <code>X.Z=z</code></td>
 *     <td style="border: 1px solid;"><code>{Y=y, Z=z}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>A=</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>A=B:b,C:c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{A=a, B=b}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>A.B=b</code>, <code>A.C=c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{A=a, B=b}</code></td>
 *   </tr>
 * </table>
 */
@MyCoReTest
public class MCRConfigurableInstancePropertyMapTest {

    private static final Map<String, String> EMPTY = Map.of();

    private static final Map<String, String> DEFAULT_LIST = Map.of("NonEmpty", "DefaultValue");

    private static final Map<String, String> MAP = Map.of("NonEmpty", "Value");

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing.class),
    })
    public void notRequired_PropertyMissing_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequired_PropertyMissing_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequired_PropertyMissing_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequired_PropertyMissing_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
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
        assertEquals("Missing default configuration entries like: MCR.Map, MCR.Map.A, MCR.Map.B, ...",
            exception.getMessage());
    }

    public static class NotRequiredPropertyMissingDefaultSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequired_PropertyMissing_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequired_PropertyMissing_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(DEFAULT_LIST, instance.map);
    }

    public static class NotRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequired_PropertyMissing_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(DEFAULT_LIST, instance.map);
    }

    public static class NotRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequired_PropertyShortEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequired_PropertyShortEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequired_PropertyShortEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class NotRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequired_PropertyShortNotEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequired_PropertyLongNotEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;
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
        assertEquals("Missing configuration entries like: Foo.Map, Foo.Map.A, Foo.Map.B, ...",
            exception.getMessage());
    }

    public static class RequiredPropertyMissingDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void required_PropertyMissing_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.Map, Foo.Map.A, Foo.Map.B, ...",
            exception.getMessage());
    }

    public static class RequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void required_PropertyMissing_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.Map, Foo.Map.A, Foo.Map.B, ...",
            exception.getMessage());
    }

    public static class RequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void required_PropertyMissing_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.Map, Foo.Map.A, Foo.Map.B, ...",
            exception.getMessage());
    }

    public static class RequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
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
        assertEquals("Missing default configuration entries like: MCR.Map, MCR.Map.A, MCR.Map.B, ...",
            exception.getMessage());
    }

    public static class RequiredPropertyMissingDefaultSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void required_PropertyMissing_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class RequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void required_PropertyMissing_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(DEFAULT_LIST, instance.map);
    }

    public static class RequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void required_PropertyMissing_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(DEFAULT_LIST, instance.map);
    }

    public static class RequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void required_PropertyShortEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class RequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class RequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void required_PropertyShortEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class RequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void required_PropertyShortEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(EMPTY, instance.map);
    }

    public static class RequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
    })
    public void required_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void required_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void required_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void required_PropertyShortNotEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
    })
    public void required_PropertyShortNotEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void required_PropertyShortNotEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void required_PropertyShortNotEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void required_PropertyShortNotEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void required_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultNotSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultSet_DefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultSet_DefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void required_PropertyLongNotEmpty_DefaultSet_DefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void required_PropertyLongNotEmpty_DefaultSet_DefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty.class, configuration);
        assertEquals(MAP, instance.map);
    }

    public static class RequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty {
        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;
    }

}

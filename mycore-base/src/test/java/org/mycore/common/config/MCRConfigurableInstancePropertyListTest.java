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
        classNameOf = NotRequiredDefaultNotSet.class),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDefaultSet.class, configuration);
        });
        assertEquals("Missing default configuration entries like: MCR.List, MCR.List.1, MCR.List.2, ...",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(DEFAULT_MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(DEFAULT_MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDefaultNotSet.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.List, Foo.List.1, Foo.List.2, ...",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDefaultNotSet.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.List, Foo.List.1, Foo.List.2, ...",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDefaultNotSet.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.List, Foo.List.1, Foo.List.2, ...",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDefaultNotSet.class, configuration);
        });
        assertEquals("Missing configuration entries like: Foo.List, Foo.List.1, Foo.List.2, ...",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDefaultSet.class, configuration);
        });
        assertEquals("Missing default configuration entries like: MCR.List, MCR.List.1, MCR.List.2, ...",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(DEFAULT_MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(DEFAULT_MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals(MAP, instance.list);
    }

    public static class NotRequiredDefaultNotSet {
        @MCRPropertyList(name = "List", required = false)
        public List<String> list;
    }

    public static class NotRequiredDefaultSet {
        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;
    }

    public static class RequiredDefaultSet {
        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;
    }

    public static class RequiredDefaultNotSet {
        @MCRPropertyList(name = "List")
        public List<String> list;
    }

}

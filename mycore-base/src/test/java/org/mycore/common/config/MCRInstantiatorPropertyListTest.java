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
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.ofName;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRPropertyList;
import org.mycore.test.MyCoReTest;

/**
 * Exhaustive list of tests for the following conditions:
 * <ol>
 *   <li>Annotation has <code>required = false</code> or not</li>
 *   <li>Property value (for a single-element map) is not set, set empty in short form,
 *   set non-empty in short form or set non-empty in long form</li>
 *   <li>Annotation has <code>defaultName = "..."</code> or not</li>
 *   <li>Default property value (for a single-element map) is not set, set empty in short form,
 *   set non-empty in short form or set non-empty in long form </li>
 * </ol>
 * <table style="border-collapse: collapse;">
 *   <caption><strong>Expected results for different conditions</strong></caption>
 *   <tr>
 *     <th style="border: 1px solid;">Property</th>
 *     <th style="border: 1px solid;">Has Default</th>
 *     <th style="border: 1px solid;">Default Property</th>
 *     <th style="border: 1px solid;">Expected Optional Result</th>
 *     <th style="border: 1px solid;">Expected Required Result</th>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[]</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">Exception</td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X=</code></td>
 *     <td style="border: 1px solid;"><code>[]</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X=y,z</code></td>
 *     <td style="border: 1px solid;"><code>[y, z]</code></td>
 *     <td style="border: 1px solid;"><code>[y, z]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X.1=y</code>, <code>X.2=z</code></td>
 *     <td style="border: 1px solid;"><code>[y, z]</code></td>
 *     <td style="border: 1px solid;"><code>[y, z]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;"><code>A=</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[]</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;"><code>A=b,c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[b, c]</code></td>
 *     <td style="border: 1px solid;"><code>[b, c]</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;"><code>A.1=b</code>, <code>A.2=c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>[b, c]</code></td>
 *     <td style="border: 1px solid;"><code>[b, c]</code></td>
 *   </tr>
 * </table>
 */
@MyCoReTest
public class MCRInstantiatorPropertyListTest {

    private static final List<String> EMPTY = List.of();

    private static final List<String> DEFAULT_MAP = List.of("DefaultValue");

    private static final List<String> MAP = List.of("Value");

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate());
        assertEquals("Default property list, configured in MCR.List (and its sub-properties)," +
            " for target field 'list' in configured class " + NotRequiredDefaultSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(DEFAULT_MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(DEFAULT_MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(EMPTY, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Default property list, configured in MCR.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyShortEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Default property list, configured in MCR.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultSet.class.getName() + " is empty",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(DEFAULT_MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(DEFAULT_MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultNotSet.class.getName() + " is empty",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultNotSet.class.getName() + " is empty",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultNotSet.class.getName() + " is empty",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultNotSet.class.getName() + " is empty",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultSet.class.getName() + " is empty",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultSet.class.getName() + " is empty",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultSet.class.getName() + " is empty",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo.Class").instantiate());
        assertEquals("Property list, configured in Foo.List (and its sub-properties)," +
            " for target field 'list' in configured class " + RequiredDefaultSet.class.getName() + " is empty",
            exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List", string = "Value,"),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List", string = "DefaultValue,"),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo.Class").instantiate();
        assertEquals(MAP, instance.list);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo.Class",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.List.1", string = "Value"),
        @MCRTestProperty(key = "Foo.List.2", empty = true),
        @MCRTestProperty(key = "MCR.List.1", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.List.2", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo.Class").instantiate();
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

    public static class RequiredDefaultNotSet {

        @MCRPropertyList(name = "List")
        public List<String> list;

    }

    public static class RequiredDefaultSet {

        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;

    }

}

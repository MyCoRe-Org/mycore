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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.ofName;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.test.MyCoReTest;

/**
 * Exhaustive list of tests for the following conditions:
 * <ol>
 *   <li>Annotation has <code>required = false</code> or not</li>
 *   <li>Property value is not set, empty or non-empty</li>
 *   <li>Annotation has <code>defaultName = "..."</code> or not</li>
 *   <li>Default property value is not set, empty or non-empty</li>
 * </ol>
 * <table style="border-collapse: collapse;">
 *   <caption>Expected results for different conditions</caption>
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
 *     <td style="border: 1px solid;"><code>null</code></td>
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
 *     <td style="border: 1px solid;">empty</td>
 *     <td style="border: 1px solid;">empty</td>
 *     <td style="border: 1px solid;">empty</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>DefaultValue</code></td>
 *     <td style="border: 1px solid;"><code>DefaultValue</code></td>
 *     <td style="border: 1px solid;"><code>DefaultValue</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">empty</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">empty</td>
 *     <td style="border: 1px solid;">empty</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;"><code>Value</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>Value</code></td>
 *     <td style="border: 1px solid;"><code>Value</code></td>
 *   </tr>
 * </table>
 */
@MyCoReTest
public class MCRInstantiatorPropertyTest {

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
    })
    public void notRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertNull(instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertNull(instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertNull(instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
    })
    public void notRequiredPropertyNotSetDefaultSetDefaultPropertyNotSet() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Default property, configured in MCR.Value (and its sub-properties)," +
            " for target field 'value' in configured class " + NotRequiredDefaultSet.class.getName()
            + " is missing", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyNotSetDefaultSetDefaultPropertyEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("DefaultValue", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void notRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void notRequiredPropertyEmptyDefaultSetDefaultPropertyNotSet() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyEmptyDefaultSetDefaultPropertyEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void notRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void notRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
    })
    public void requiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property, configured in Foo.Value (and its sub-properties)," +
            " for target field 'value' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is missing", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property, configured in Foo.Value (and its sub-properties)," +
            " for target field 'value' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is missing", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property, configured in Foo.Value (and its sub-properties)," +
            " for target field 'value' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is missing", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
    })
    public void requiredPropertyNotSetDefaultSetDefaultPropertyNotSet() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Default property, configured in MCR.Value (and its sub-properties)," +
            " for target field 'value' in configured class " + RequiredDefaultSet.class.getName()
            + " is missing", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyNotSetDefaultSetDefaultPropertyEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("DefaultValue", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void requiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void requiredPropertyEmptyDefaultSetDefaultPropertyNotSet() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyEmptyDefaultSetDefaultPropertyEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void requiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void requiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals("Value", instance.value);
    }

    public static class NotRequiredDefaultNotSet {

        @MCRProperty(name = "Value", required = false)
        public String value;

    }

    public static class NotRequiredDefaultSet {

        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;

    }

    public static class RequiredDefaultNotSet {

        @MCRProperty(name = "Value")
        public String value;

    }

    public static class RequiredDefaultSet {

        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;

    }

}

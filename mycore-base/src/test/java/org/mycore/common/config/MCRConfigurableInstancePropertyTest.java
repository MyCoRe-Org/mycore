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

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.test.MyCoReTest;

/**
 * Programmatically created exhaustive list of tests for the following conditions:
 * <ol>
 *   <li>Annotation has <code>required = false</code> or not</li>
 *   <li>Configuration property for instance value is not set, empty or non-empty</li>
 *   <li>Annotation has <code>defaultName = "..."</code> or not</li>
 *   <li>Configuration property for default value is not set, empty or non-empty</li>
 * </ol>
 * <table style="border-collapse: collapse;">
 *   <caption>Expected results for different conditions</caption>
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
 *     <td style="border: 1px solid;"><code>null</code></td>
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
 *     <td style="border: 1px solid;">empty</td>
 *     <td style="border: 1px solid;">empty</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>DefaultValue</code></td>
 *     <td style="border: 1px solid;"><code>DefaultValue</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;">empty</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">empty</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">no</td>
 *     <td style="border: 1px solid;"><code>Value</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>Value</code></td>
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
 *     <td style="border: 1px solid;">empty</td>
 *     <td style="border: 1px solid;">empty</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>DefaultValue</code></td>
 *     <td style="border: 1px solid;"><code>DefaultValue</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;">empty</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">empty</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>Value</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>Value</code></td>
 *   </tr>
 * </table>
 */
@MyCoReTest
public class MCRConfigurableInstancePropertyTest {

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
    })
    public void notRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertNull(instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertNull(instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertNull(instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
    })
    public void notRequiredPropertyNotSetDefaultSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDefaultSet.class, configuration);
        });
        assertEquals("The default property MCR.Value is missing", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyNotSetDefaultSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals("DefaultValue", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void notRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void notRequiredPropertyEmptyDefaultSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyEmptyDefaultSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void notRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultNotSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void notRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            NotRequiredDefaultSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
    })
    public void requiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDefaultNotSet.class, configuration);
        });
        assertEquals("The required property Foo.Value is missing", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDefaultNotSet.class, configuration);
        });
        assertEquals("The required property Foo.Value is missing", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDefaultNotSet.class, configuration);
        });
        assertEquals("The required property Foo.Value is missing", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
    })
    public void requiredPropertyNotSetDefaultSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDefaultSet.class, configuration);
        });
        assertEquals("The default property MCR.Value is missing", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyNotSetDefaultSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals("DefaultValue", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void requiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void requiredPropertyEmptyDefaultSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyEmptyDefaultSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals("", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void requiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultNotSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultNotSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void requiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void requiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void requiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredDefaultSet instance = MCRConfigurableInstanceHelper.getInstance(
            RequiredDefaultSet.class, configuration);
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

    public static class RequiredDefaultSet {
        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;
    }

    public static class RequiredDefaultNotSet {
        @MCRProperty(name = "Value")
        public String value;
    }

}

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
        classNameOf = NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet.class),
    })
    public void notRequired_PropertyNotSet_DefaultNotSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet.class, configuration);
        assertNull(instance.value);
    }

    public static class NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value", required = false)
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequired_PropertyNotSet_DefaultNotSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty.class, configuration);
        assertNull(instance.value);
    }

    public static class NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value", required = false)
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequired_PropertyNotSet_DefaultNotSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty.class, configuration);
        assertNull(instance.value);
    }

    public static class NotRequiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value", required = false)
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotSetDefaultSetDefaultPropertyNotSet.class),
    })
    public void notRequired_PropertyNotSet_DefaultSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotSetDefaultSetDefaultPropertyNotSet.class, configuration);
        });
        assertEquals("The default property MCR.Value is missing", exception.getMessage());
    }

    public static class NotRequiredPropertyNotSetDefaultSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotSetDefaultSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequired_PropertyNotSet_DefaultSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotSetDefaultSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotSetDefaultSetDefaultPropertyEmpty.class, configuration);
        assertEquals("", instance.value);
    }

    public static class NotRequiredPropertyNotSetDefaultSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequired_PropertyNotSet_DefaultSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty.class, configuration);
        assertEquals("DefaultValue", instance.value);
    }

    public static class NotRequiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void notRequired_PropertyEmpty_DefaultNotSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet.class, configuration);
        assertEquals("", instance.value);
    }

    public static class NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value", required = false)
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequired_PropertyEmpty_DefaultNotSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty.class, configuration);
        assertEquals("", instance.value);
    }

    public static class NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value", required = false)
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequired_PropertyEmpty_DefaultNotSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty.class, configuration);
        assertEquals("", instance.value);
    }

    public static class NotRequiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value", required = false)
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyEmptyDefaultSetDefaultPropertyNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void notRequired_PropertyEmpty_DefaultSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyEmptyDefaultSetDefaultPropertyNotSet instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyEmptyDefaultSetDefaultPropertyNotSet.class, configuration);
        assertEquals("", instance.value);
    }

    public static class NotRequiredPropertyEmptyDefaultSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyEmptyDefaultSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequired_PropertyEmpty_DefaultSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyEmptyDefaultSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyEmptyDefaultSetDefaultPropertyEmpty.class, configuration);
        assertEquals("", instance.value);
    }

    public static class NotRequiredPropertyEmptyDefaultSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequired_PropertyEmpty_DefaultSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty.class, configuration);
        assertEquals("", instance.value);
    }

    public static class NotRequiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void notRequired_PropertyNotEmpty_DefaultNotSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value", required = false)
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequired_PropertyNotEmpty_DefaultNotSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value", required = false)
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequired_PropertyNotEmpty_DefaultNotSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class NotRequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value", required = false)
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void notRequired_PropertyNotEmpty_DefaultSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void notRequired_PropertyNotEmpty_DefaultSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void notRequired_PropertyNotEmpty_DefaultSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class NotRequiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet.class),
    })
    public void Required_PropertyNotSet_DefaultNotSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet.class, configuration);
        });
        assertEquals("The required property Foo.Value is missing", exception.getMessage());
    }

    public static class RequiredPropertyNotSetDefaultNotSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void Required_PropertyNotSet_DefaultNotSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty.class, configuration);
        });
        assertEquals("The required property Foo.Value is missing", exception.getMessage());
    }

    public static class RequiredPropertyNotSetDefaultNotSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void Required_PropertyNotSet_DefaultNotSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty.class, configuration);
        });
        assertEquals("The required property Foo.Value is missing", exception.getMessage());
    }

    public static class RequiredPropertyNotSetDefaultNotSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotSetDefaultSetDefaultPropertyNotSet.class),
    })
    public void Required_PropertyNotSet_DefaultSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotSetDefaultSetDefaultPropertyNotSet.class, configuration);
        });
        assertEquals("The default property MCR.Value is missing", exception.getMessage());
    }

    public static class RequiredPropertyNotSetDefaultSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotSetDefaultSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void Required_PropertyNotSet_DefaultSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyNotSetDefaultSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotSetDefaultSetDefaultPropertyEmpty.class, configuration);
        assertEquals("", instance.value);
    }

    public static class RequiredPropertyNotSetDefaultSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void Required_PropertyNotSet_DefaultSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty.class, configuration);
        assertEquals("DefaultValue", instance.value);
    }

    public static class RequiredPropertyNotSetDefaultSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void Required_PropertyEmpty_DefaultNotSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet.class, configuration);
        assertEquals("", instance.value);
    }

    public static class RequiredPropertyEmptyDefaultNotSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void Required_PropertyEmpty_DefaultNotSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty.class, configuration);
        assertEquals("", instance.value);
    }

    public static class RequiredPropertyEmptyDefaultNotSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void Required_PropertyEmpty_DefaultNotSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty.class, configuration);
        assertEquals("", instance.value);
    }

    public static class RequiredPropertyEmptyDefaultNotSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyEmptyDefaultSetDefaultPropertyNotSet.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
    })
    public void Required_PropertyEmpty_DefaultSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyEmptyDefaultSetDefaultPropertyNotSet instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyEmptyDefaultSetDefaultPropertyNotSet.class, configuration);
        assertEquals("", instance.value);
    }

    public static class RequiredPropertyEmptyDefaultSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyEmptyDefaultSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void Required_PropertyEmpty_DefaultSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyEmptyDefaultSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyEmptyDefaultSetDefaultPropertyEmpty.class, configuration);
        assertEquals("", instance.value);
    }

    public static class RequiredPropertyEmptyDefaultSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "Foo.Value", empty = true),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void Required_PropertyEmpty_DefaultSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty.class, configuration);
        assertEquals("", instance.value);
    }

    public static class RequiredPropertyEmptyDefaultSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void Required_PropertyNotEmpty_DefaultNotSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void Required_PropertyNotEmpty_DefaultNotSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void Required_PropertyNotEmpty_DefaultNotSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class RequiredPropertyNotEmptyDefaultNotSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
    })
    public void Required_PropertyNotEmpty_DefaultSet_DefaultPropertyNotSet() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class RequiredPropertyNotEmptyDefaultSetDefaultPropertyNotSet {
        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", empty = true),
    })
    public void Required_PropertyNotEmpty_DefaultSet_DefaultPropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class RequiredPropertyNotEmptyDefaultSetDefaultPropertyEmpty {
        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty.class),
        @MCRTestProperty(key = "Foo.Value", string = "Value"),
        @MCRTestProperty(key = "MCR.Value", string = "DefaultValue"),
    })
    public void Required_PropertyNotEmpty_DefaultSet_DefaultPropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty.class, configuration);
        assertEquals("Value", instance.value);
    }

    public static class RequiredPropertyNotEmptyDefaultSetDefaultPropertyNotEmpty {
        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;
    }

}

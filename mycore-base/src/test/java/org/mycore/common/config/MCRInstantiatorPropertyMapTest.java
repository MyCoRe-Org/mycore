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

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRPropertyMap;
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
 *     <td style="border: 1px solid;"><code>{}</code></td>
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
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X=Y:y,Z:z</code></td>
 *     <td style="border: 1px solid;"><code>{Y=y, Z=z}</code></td>
 *     <td style="border: 1px solid;"><code>{Y=y, Z=z}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">yes</td>
 *     <td style="border: 1px solid;"><code>X.Y=y</code>, <code>X.Z=z</code></td>
 *     <td style="border: 1px solid;"><code>{Y=y, Z=z}</code></td>
 *     <td style="border: 1px solid;"><code>{Y=y, Z=z}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;"><code>A=</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;"><code>A=B:b,C:c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{B=b, C=c}</code></td>
 *     <td style="border: 1px solid;"><code>{B=b, C=c}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;"><code>A.B=b</code>, <code>A.C=c</code></td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{B=b, C=c}</code></td>
 *     <td style="border: 1px solid;"><code>{B=b, C=c}</code></td>
 *   </tr>
 * </table>
 */
@MyCoReTest
public class MCRInstantiatorPropertyMapTest {

    private static final Map<String, String> EMPTY = Map.of();

    private static final Map<String, String> DEFAULT_LIST = Map.of("NonEmpty", "DefaultValue");

    private static final Map<String, String> MAP = Map.of("NonEmpty", "Value");

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyMissing() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(NotRequiredDefaultSet.class, "Foo").instantiate());
        assertEquals("Default property map, configured in MCR.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + NotRequiredDefaultSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(DEFAULT_LIST, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(DEFAULT_LIST, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyMissing() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultNotSet instance = ofName(NotRequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = NotRequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void notRequiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        NotRequiredDefaultSet instance = ofName(NotRequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyShortEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void requiredPropertyMissingDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Default property map, configured in MCR.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyShortEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Default property map, configured in MCR.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyShortNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(DEFAULT_LIST, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void requiredPropertyMissingDefaultSetDefaultPropertyLongNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(DEFAULT_LIST, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultNotSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyShortEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void requiredPropertyShortEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Property map, configured in Foo.Map (and its sub-properties)," +
            " for target field 'map' in configured class " + RequiredDefaultSet.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyMissing() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyMissing() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map", string = "NonEmpty:Value,Empty:"),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void requiredPropertyShortNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyMissing() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyShortNotEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultNotSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultNotSetDefaultPropertyLongNotEmpty() {
        RequiredDefaultNotSet instance = ofName(RequiredDefaultNotSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyMissing() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map", string = "NonEmpty:DefaultValue,Empty:"),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyShortNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    @Test
    @MCRTestConfiguration(properties = { @MCRTestProperty(key = "Foo",
        classNameOf = RequiredDefaultSet.class),
        @MCRTestProperty(key = "Foo.Map.NonEmpty", string = "Value"),
        @MCRTestProperty(key = "Foo.Map.Empty", empty = true),
        @MCRTestProperty(key = "MCR.Map.NonEmpty", string = "DefaultValue"),
        @MCRTestProperty(key = "MCR.Map.Empty", empty = true),
    })
    public void requiredPropertyLongNotEmptyDefaultSetDefaultPropertyLongNotEmpty() {
        RequiredDefaultSet instance = ofName(RequiredDefaultSet.class, "Foo").instantiate();
        assertEquals(MAP, instance.map);
    }

    public static class NotRequiredDefaultNotSet {

        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;

    }

    public static class NotRequiredDefaultSet {

        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;

    }

    public static class RequiredDefaultNotSet {

        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;

    }

    public static class RequiredDefaultSet {

        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;

    }

}

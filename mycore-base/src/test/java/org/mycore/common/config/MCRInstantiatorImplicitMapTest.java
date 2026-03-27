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
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.ofName;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.test.MyCoReTest;

/**
 * Exhaustive list of tests for the following conditions:
 * <ol>
 *   <li>Annotation has <code>required = false</code> or not</li>
 *   <li>Class-property is not set, set to an empty string or set to a fully qualified class name</li>
 *   <li>Annotation has no sentinel, has an implicitly enabling sentinel, has an explicitly enabling sentinel,
 *   has an implicitly disabling sentinel, has an explicitly disabling sentinel</li>
 *   <li>Instance-property value (for a single element-map) is not set, set empty or set non-empty</li>
 * </ol>
 * <table style="border-collapse: collapse;">
 *   <caption>
 *     <strong>Expected results for different conditions</strong><br>
 *     (with a nested class with optional property <code>a</code>
 *     and<code>toString</code>-implementation <code>a == null ? "_" : "(" + a + ")"</code>)
 *   </caption>
 *   <tr>
 *     <th style="border: 1px solid;">Class Property</th>
 *     <th style="border: 1px solid;">Sentinel</th>
 *     <th style="border: 1px solid;">Instance Property</th>
 *     <th style="border: 1px solid;">Expected Optional Result</th>
 *     <th style="border: 1px solid;">Expected Required Result</th>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">none</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">none</td>
 *     <td style="border: 1px solid;"><code>A.a=</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">none</td>
 *     <td style="border: 1px solid;"><code>A.a=Value</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">impl. enabled</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">impl. enabled</td>
 *     <td style="border: 1px solid;"><code>A.a=</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">impl. enabled</td>
 *     <td style="border: 1px solid;"><code>A.a=Value</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">expl. enabled</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;"><code>{A=_}</code></td>
 *     <td style="border: 1px solid;"><code>{A=_}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">expl. enabled</td>
 *     <td style="border: 1px solid;"><code>A.a=</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">expl. enabled</td>
 *     <td style="border: 1px solid;"><code>A.a=Value</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">set</td>
 *     <td style="border: 1px solid;">none</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;"><code>{A=_}</code></td>
 *     <td style="border: 1px solid;"><code>{A=_}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">set</td>
 *     <td style="border: 1px solid;">none</td>
 *     <td style="border: 1px solid;"><code>A.a=</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">set</td>
 *     <td style="border: 1px solid;">none</td>
 *     <td style="border: 1px solid;"><code>A.a=Value</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">set</td>
 *     <td style="border: 1px solid;">impl. or expl. enabled</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;"><code>{A=_}</code></td>
 *     <td style="border: 1px solid;"><code>{A=_}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">set</td>
 *     <td style="border: 1px solid;">impl. or expl. enabled</td>
 *     <td style="border: 1px solid;"><code>A.a=</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">set</td>
 *     <td style="border: 1px solid;">impl. or expl. enabled</td>
 *     <td style="border: 1px solid;"><code>A.a=Value</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">impl. or expl. disabled</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">set empty</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 * </table>
 */
@MyCoReTest
public class MCRInstantiatorImplicitMapTest {

    public static final String ENABLED = "true";

    public static final String DISABLED = "false";

    public static final Map<String, Nested> EMPTY = Map.of();

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
    })
    public void notRequiredClassPropertyMissingNoSentinelValuePropertyMissing() {
        NotRequiredNoSentinel instance = ofName(NotRequiredNoSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyMissingNoSentinelValuePropertyEmpty() {
        NotRequiredNoSentinel instance = ofName(NotRequiredNoSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyMissingNoSentinelValuePropertyNotEmpty() {
        NotRequiredNoSentinel instance = ofName(NotRequiredNoSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
    })
    public void notRequiredClassPropertyMissingSentinelEnabledValuePropertyMissing() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyMissingSentinelEnabledValuePropertyEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyMissingSentinelEnabledValuePropertyNotEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
    })
    public void notRequiredClassPropertyMissingSentinelSetEnabledValuePropertyMissing() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertNull(instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyMissingSentinelSetEnabledValuePropertyEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyMissingSentinelSetEnabledValuePropertyNotEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
    })
    public void notRequiredClassPropertyMissingSentinelDisabledValuePropertyMissing() {
        NotRequiredDisablingSentinel instance = ofName(NotRequiredDisablingSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyMissingSentinelDisabledValuePropertyEmpty() {
        NotRequiredDisablingSentinel instance = ofName(NotRequiredDisablingSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyMissingSentinelDisabledValuePropertyNotEmpty() {
        NotRequiredDisablingSentinel instance = ofName(NotRequiredDisablingSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
    })
    public void notRequiredClassPropertyMissingSentinelSetDisabledValuePropertyMissing() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyMissingSentinelSetDisabledValuePropertyEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyMissingSentinelSetDisabledValuePropertyNotEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
    })
    public void notRequiredClassPropertyEmptyNoSentinelValuePropertyMissing() {
        NotRequiredNoSentinel instance = ofName(NotRequiredNoSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyEmptyNoSentinelValuePropertyEmpty() {
        NotRequiredNoSentinel instance = ofName(NotRequiredNoSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyEmptyNoSentinelValuePropertyNotEmpty() {
        NotRequiredNoSentinel instance = ofName(NotRequiredNoSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
    })
    public void notRequiredClassPropertyEmptySentinelEnabledValuePropertyMissing() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyEmptySentinelEnabledValuePropertyEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyEmptySentinelEnabledValuePropertyNotEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
    })
    public void notRequiredClassPropertyEmptySentinelSetEnabledValuePropertyMissing() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyEmptySentinelSetEnabledValuePropertyEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyEmptySentinelSetEnabledValuePropertyNotEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
    })
    public void notRequiredClassPropertyEmptySentinelDisabledValuePropertyMissing() {
        NotRequiredDisablingSentinel instance = ofName(NotRequiredDisablingSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyEmptySentinelDisabledValuePropertyEmpty() {
        NotRequiredDisablingSentinel instance = ofName(NotRequiredDisablingSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyEmptySentinelDisabledValuePropertyNotEmpty() {
        NotRequiredDisablingSentinel instance = ofName(NotRequiredDisablingSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
    })
    public void notRequiredClassPropertyEmptySentinelSetDisabledValuePropertyMissing() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyEmptySentinelSetDisabledValuePropertyEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyEmptySentinelSetDisabledValuePropertyNotEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void notRequiredClassPropertyNotEmptyNoSentinelValuePropertyMissing() {
        NotRequiredNoSentinel instance = ofName(NotRequiredNoSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertNull(instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyNotEmptyNoSentinelValuePropertyEmpty() {
        NotRequiredNoSentinel instance = ofName(NotRequiredNoSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyNotEmptyNoSentinelValuePropertyNotEmpty() {
        NotRequiredNoSentinel instance = ofName(NotRequiredNoSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void notRequiredClassPropertyNotEmptySentinelEnabledValuePropertyMissing() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertNull(instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyNotEmptySentinelEnabledValuePropertyEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyNotEmptySentinelEnabledValuePropertyNotEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertNotNull(instance.nestedMap.get("foo").value);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
    })
    public void notRequiredClassPropertyNotEmptySentinelSetEnabledValuePropertyMissing() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertNull(instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyNotEmptySentinelSetEnabledValuePropertyEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyNotEmptySentinelSetEnabledValuePropertyNotEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertNotNull(instance.nestedMap.get("foo").value);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void notRequiredClassPropertyNotEmptySentinelDisabledValuePropertyMissing() {
        NotRequiredDisablingSentinel instance = ofName(NotRequiredDisablingSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyNotEmptySentinelDisabledValuePropertyEmpty() {
        NotRequiredDisablingSentinel instance = ofName(NotRequiredDisablingSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyNotEmptySentinelDisabledValuePropertyNotEmpty() {
        NotRequiredDisablingSentinel instance = ofName(NotRequiredDisablingSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
    })
    public void notRequiredClassPropertyNotEmptySentinelSetDisabledValuePropertyMissing() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyNotEmptySentinelSetDisabledValuePropertyEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyNotEmptySentinelSetDisabledValuePropertyNotEmpty() {
        NotRequiredSentinel instance = ofName(NotRequiredSentinel.class, "Foo").instantiate();
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
    })
    public void requiredClassPropertyMissingNoSentinelValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredNoSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyMissingNoSentinelValuePropertyEmpty() {
        RequiredNoSentinel instance = ofName(RequiredNoSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyMissingNoSentinelValuePropertyNotEmpty() {
        RequiredNoSentinel instance = ofName(RequiredNoSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
    })
    public void requiredClassPropertyMissingSentinelEnabledValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyMissingSentinelEnabledValuePropertyEmpty() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyMissingSentinelEnabledValuePropertyNotEmpty() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
    })
    public void requiredClassPropertyMissingSentinelSetEnabledValuePropertyMissing() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertNull(instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyMissingSentinelSetEnabledValuePropertyEmpty() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyMissingSentinelSetEnabledValuePropertyNotEmpty() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
    })
    public void requiredClassPropertyMissingSentinelDisabledValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredDisablingSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyMissingSentinelDisabledValuePropertyEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredDisablingSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyMissingSentinelDisabledValuePropertyNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredDisablingSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
    })
    public void requiredClassPropertyMissingSentinelSetDisabledValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyMissingSentinelSetDisabledValuePropertyEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyMissingSentinelSetDisabledValuePropertyNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
    })
    public void requiredClassPropertyEmptyNoSentinelValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredNoSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyEmptyNoSentinelValuePropertyEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredNoSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyEmptyNoSentinelValuePropertyNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredNoSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
    })
    public void requiredClassPropertyEmptySentinelEnabledValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyEmptySentinelEnabledValuePropertyEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyEmptySentinelEnabledValuePropertyNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
    })
    public void requiredClassPropertyEmptySentinelSetEnabledValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyEmptySentinelSetEnabledValuePropertyEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyEmptySentinelSetEnabledValuePropertyNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
    })
    public void requiredClassPropertyEmptySentinelDisabledValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredDisablingSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyEmptySentinelDisabledValuePropertyEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredDisablingSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyEmptySentinelDisabledValuePropertyNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredDisablingSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
    })
    public void requiredClassPropertyEmptySentinelSetDisabledValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyEmptySentinelSetDisabledValuePropertyEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyEmptySentinelSetDisabledValuePropertyNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void requiredClassPropertyNotEmptyNoSentinelValuePropertyMissing() {
        RequiredNoSentinel instance = ofName(RequiredNoSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertNull(instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyNotEmptyNoSentinelValuePropertyEmpty() {
        RequiredNoSentinel instance = ofName(RequiredNoSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyNotEmptyNoSentinelValuePropertyNotEmpty() {
        RequiredNoSentinel instance = ofName(RequiredNoSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void requiredClassPropertyNotEmptySentinelEnabledValuePropertyMissing() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertNull(instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyNotEmptySentinelEnabledValuePropertyEmpty() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyNotEmptySentinelEnabledValuePropertyNotEmpty() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
    })
    public void requiredClassPropertyNotEmptySentinelSetEnabledValuePropertyMissing() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertNull(instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyNotEmptySentinelSetEnabledValuePropertyEmpty() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyNotEmptySentinelSetEnabledValuePropertyNotEmpty() {
        RequiredSentinel instance = ofName(RequiredSentinel.class, "Foo").instantiate();
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void requiredClassPropertyNotEmptySentinelDisabledValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredDisablingSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyNotEmptySentinelDisabledValuePropertyEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredDisablingSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyNotEmptySentinelDisabledValuePropertyNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredDisablingSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
    })
    public void requiredClassPropertyNotEmptySentinelSetDisabledValuePropertyMissing() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyNotEmptySentinelSetDisabledValuePropertyEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyNotEmptySentinelSetDisabledValuePropertyNotEmpty() {
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class,
            () -> ofName(MCRConfigurationException.class, "Foo").instantiate());
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    public static class NotRequiredNoSentinel {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class, required = false)
        public Map<String, Nested> nestedMap;

    }

    public static class NotRequiredSentinel {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class, required = false, sentinel = @MCRSentinel)
        public Map<String, Nested> nestedMap;

    }

    public static class NotRequiredDisablingSentinel {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class, required = false,
            sentinel = @MCRSentinel(defaultValue = false))
        public Map<String, Nested> nestedMap;

    }

    public static class RequiredNoSentinel {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class)
        public Map<String, Nested> nestedMap;

    }

    public static class RequiredSentinel {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class, sentinel = @MCRSentinel)
        public Map<String, Nested> nestedMap;

    }

    public static class RequiredDisablingSentinel {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class,
            sentinel = @MCRSentinel(defaultValue = false))
        public Map<String, Nested> nestedMap;

    }

    public static final class Nested {

        @MCRProperty(name = "Value", required = false)
        public String value;

    }

}

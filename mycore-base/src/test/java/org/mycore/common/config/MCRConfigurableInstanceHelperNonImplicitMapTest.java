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
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">none</td>
 *     <td style="border: 1px solid;"><code>A.a=Value</code></td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
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
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">impl. enabled</td>
 *     <td style="border: 1px solid;"><code>A.a=Value</code></td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">expl. enabled</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">expl. enabled</td>
 *     <td style="border: 1px solid;"><code>A.a=</code></td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">expl. enabled</td>
 *     <td style="border: 1px solid;"><code>A=Value</code></td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
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
 *     <td style="border: 1px solid;"><code>A=(Value)</code></td>
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
public class MCRConfigurableInstanceHelperNonImplicitMapTest {

    public static final String ENABLED = "true";

    public static final String DISABLED = "false";

    public static final Map<String, Nested> EMPTY = Map.of();

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
    })
    public void notRequiredClassPropertyMissingNoSentinelValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredNoSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyMissingNoSentinelValuePropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredNoSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyMissingNoSentinelValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredNoSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
    })
    public void notRequiredClassPropertyMissingSentinelEnabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyMissingSentinelEnabledValuePropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyMissingSentinelEnabledValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
    })
    public void notRequiredClassPropertyMissingSentinelSetEnabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyMissingSentinelSetEnabledValuePropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyMissingSentinelSetEnabledValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
    })
    public void notRequiredClassPropertyMissingSentinelDisabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDisablingSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDisablingSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyMissingSentinelDisabledValuePropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDisablingSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDisablingSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyMissingSentinelDisabledValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDisablingSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDisablingSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
    })
    public void notRequiredClassPropertyMissingSentinelSetDisabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyMissingSentinelSetDisabledValuePropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyMissingSentinelSetDisabledValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
    })
    public void notRequiredClassPropertyEmptyNoSentinelValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredNoSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyEmptyNoSentinelValuePropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredNoSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyEmptyNoSentinelValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredNoSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
    })
    public void notRequiredClassPropertyEmptySentinelEnabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyEmptySentinelEnabledValuePropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyEmptySentinelEnabledValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
    })
    public void notRequiredClassPropertyEmptySentinelSetEnabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
    })
    public void notRequiredClassPropertyEmptySentinelDisabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDisablingSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDisablingSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyEmptySentinelDisabledValuePropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDisablingSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDisablingSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyEmptySentinelDisabledValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDisablingSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDisablingSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", empty = true),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
    })
    public void notRequiredClassPropertyEmptySentinelSetDisabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void notRequiredClassPropertyNotEmptyNoSentinelValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredNoSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredNoSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredNoSentinel.class, configuration);
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void notRequiredClassPropertyNotEmptySentinelEnabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
    })
    public void notRequiredClassPropertyNotEmptySentinelSetEnabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void notRequiredClassPropertyNotEmptySentinelDisabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDisablingSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDisablingSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void notRequiredClassPropertyNotEmptySentinelDisabledValuePropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDisablingSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDisablingSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void notRequiredClassPropertyNotEmptySentinelDisabledValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredDisablingSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredDisablingSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = NotRequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = DISABLED),
    })
    public void notRequiredClassPropertyNotEmptySentinelSetDisabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        NotRequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                NotRequiredSentinel.class, configuration);
        assertEquals(EMPTY, instance.nestedMap);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
    })
    public void requiredClassPropertyMissingNoSentinelValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredNoSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredNoSentinel.class, configuration);
        });
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredNoSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredNoSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyMissingNoSentinelValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredNoSentinel.class, configuration);
        });
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredNoSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
    })
    public void requiredClassPropertyMissingSentinelEnabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyMissingSentinelEnabledValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
    })
    public void requiredClassPropertyMissingSentinelSetEnabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", empty = true),
    })
    public void requiredClassPropertyMissingSentinelSetEnabledValuePropertyEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo.Enabled", string = ENABLED),
        @MCRTestProperty(key = "Foo.Nested.foo.Value", string = "Value"),
    })
    public void requiredClassPropertyMissingSentinelSetEnabledValuePropertyNotEmpty() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
        assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
            " for target field 'nestedMap' in configured class " + RequiredSentinel.class.getName()
            + " is empty", exception.getMessage());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
    })
    public void requiredClassPropertyMissingSentinelDisabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDisablingSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDisablingSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDisablingSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredNoSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredNoSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredNoSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDisablingSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDisablingSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDisablingSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredNoSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredNoSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredNoSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredNoSentinel.class, configuration);
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void requiredClassPropertyNotEmptySentinelEnabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        RequiredSentinel instance =
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        assertNotNull(instance.nestedMap);
        assertEquals("Value", instance.nestedMap.get("foo").value);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "Foo", classNameOf = RequiredDisablingSentinel.class),
        @MCRTestProperty(key = "Foo.Nested.foo", classNameOf = Nested.class),
    })
    public void requiredClassPropertyNotEmptySentinelDisabledValuePropertyMissing() {
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDisablingSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDisablingSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredDisablingSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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
        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurationException exception = assertThrows(MCRConfigurationException.class, () -> {
            MCRConfigurableInstanceHelper.getInstance(
                RequiredSentinel.class, configuration);
        });
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

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class, sentinel = @MCRSentinel(defaultValue = false))
        public Map<String, Nested> nestedMap;

    }

    public static class Nested {

        @MCRProperty(name = "Value", required = false)
        public String value;

    }

}

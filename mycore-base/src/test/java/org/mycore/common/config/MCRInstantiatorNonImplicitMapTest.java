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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.test.MyCoReTest;

/**
 * Tests for the following conditions:
 * <ol>
 *   <li>Annotation has <code>required = false</code> or not</li>
 *   <li>Class-property is not set, set to an empty string or set to a fully qualified class name</li>
 *   <li>Annotation has
 *   <ol>
 *     <li>no sentinel,</li>
 *     <li>an enabling sentinel, but no configured sentinel value (implicitly enabling sentinel)</li>
 *     <li>a sentinel and an enabling configured sentinel value (explicitly enabling sentinel)</li>
 *     <li>a disabling sentinel, but no configured sentinel value (implicitly disabling sentinel)</li>
 *     <li>a sentinel and a disabling configured sentinel value (explicitly disabling sentinel)</li>
 *   </ol>
 *   <li>Value-property value (for a single element-map) is not set, set empty or set non-empty</li>
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
 *     <th style="border: 1px solid;">Value Property</th>
 *     <th style="border: 1px solid;">Expected Optional Result</th>
 *     <th style="border: 1px solid;">Expected Required Result</th>
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
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">none or impl. enabled</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">none or impl. enabled</td>
 *     <td style="border: 1px solid;"><code>A.a=</code></td>
 *     <td style="border: 1px solid;"><code>{}</code></td>
 *     <td style="border: 1px solid;">Exception</td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;">none or impl. enabled</td>
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
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;">not set</td>
 *     <td style="border: 1px solid;"><code>{A=_}</code></td>
 *     <td style="border: 1px solid;"><code>{A=_}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">set</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>A.a=</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *     <td style="border: 1px solid;"><code>{A=()}</code></td>
 *   </tr>
 *   <tr>
 *     <td style="border: 1px solid;">set</td>
 *     <td style="border: 1px solid;">-</td>
 *     <td style="border: 1px solid;"><code>A.a=Value</code></td>
 *     <td style="border: 1px solid;"><code>A=(Value)</code></td>
 *     <td style="border: 1px solid;"><code>{A=(Value)}</code></td>
 *   </tr>
 * </table>
 */
@MyCoReTest
public class MCRInstantiatorNonImplicitMapTest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String CONFIGURED_CLASS_PROPERTY = "Foo.Class";

    public static final String NESTED_CLASS_PROPERTY = "Foo.Nested.foo.Class";

    public static final String NESTED_SENTINEL_PROPERTY = "Foo.Nested.foo.Enabled";

    public static final String NESTED_VALUE_PROPERTY = "Foo.Nested.foo.Value";

    private static Stream<Arguments> provideAllParameterCombinations() {
        List<Arguments> argumentsList = new ArrayList<>();
        for (Boolean required : List.of(false, true)) {
            for (ClassProperty classProperty : ClassProperty.values()) {
                for (Sentinel sentinel : Sentinel.values()) {
                    for (ValueProperty valueProperty : ValueProperty.values()) {
                        argumentsList.add(Arguments.of(required, classProperty, sentinel, valueProperty));
                    }
                }
            }
        }
        return argumentsList.stream();
    }

    @ParameterizedTest
    @MethodSource("provideAllParameterCombinations")
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = NESTED_CLASS_PROPERTY, empty = true),
        @MCRTestProperty(key = NESTED_VALUE_PROPERTY, empty = true)
    })
    void test(boolean required, ClassProperty classProperty, Sentinel sentinel, ValueProperty valueProperty) {

        // log all parameters
        LOGGER.info("TEST PARAMETERS");
        LOGGER.info("required={}", required);
        LOGGER.info("classProperty={}", classProperty);
        LOGGER.info("sentinel={}", sentinel);
        LOGGER.info("valueProperty={}", valueProperty);

        // select class to be configured
        Class<? extends Configurable> configuredClass;
        if (required) {
            configuredClass = switch (sentinel) {
                case NONE -> RequiredNoSentinel.class;
                case IMPLICITLY_DISABLED -> RequiredDisablingSentinel.class;
                default -> RequiredEnablingSentinel.class;
            };
        } else {
            configuredClass = switch (sentinel) {
                case NONE -> NotRequiredNoSentinel.class;
                case IMPLICITLY_DISABLED -> NotRequiredDisablingSentinel.class;
                default -> NotRequiredEnablingSentinel.class;
            };
        }

        // set class property for configured instance
        MCRConfiguration2.set(CONFIGURED_CLASS_PROPERTY, configuredClass.getName());

        // set class property for nested instance
        // empty string is default via @MCRTestProperty above, may need to overwrite
        switch (classProperty) {
            case NOT_SET -> MCRConfiguration2.set(NESTED_CLASS_PROPERTY, (String) null);
            case SET_NON_EMPTY -> MCRConfiguration2.set(NESTED_CLASS_PROPERTY, Nested.class.getName());
        }

        // set sentinel property for nested instance
        switch (sentinel) {
            case EXPLICITLY_ENABLED -> MCRConfiguration2.set(NESTED_SENTINEL_PROPERTY, "true");
            case EXPLICITLY_DISABLED -> MCRConfiguration2.set(NESTED_SENTINEL_PROPERTY, "false");
        }

        // set value property for property of nested instance
        // empty string is default via @MCRTestProperty above, may need to overwrite
        switch (valueProperty) {
            case NOT_SET -> MCRConfiguration2.set(NESTED_VALUE_PROPERTY, (String) null);
            case SET_NON_EMPTY -> MCRConfiguration2.set(NESTED_VALUE_PROPERTY, "Value");
        }

        // log all relevant configuration entries
        LOGGER.info("CONFIGURATION PROPERTIES");
        Map<String, String> propertiesMap = MCRConfiguration2.getPropertiesMap();
        LOGGER.info("{}={}", CONFIGURED_CLASS_PROPERTY, get(propertiesMap, CONFIGURED_CLASS_PROPERTY));
        LOGGER.info("{}={}", NESTED_CLASS_PROPERTY, get(propertiesMap, NESTED_CLASS_PROPERTY));
        LOGGER.info("{}={}", NESTED_SENTINEL_PROPERTY, get(propertiesMap, NESTED_SENTINEL_PROPERTY));
        LOGGER.info("{}={}", NESTED_VALUE_PROPERTY, get(propertiesMap, NESTED_VALUE_PROPERTY));

        // perform instantiation of configured class
        Configurable instance = null;
        MCRConfigurationException exception = null;
        try {
            instance = MCRInstanceConfiguration.ofName(Configurable.class, CONFIGURED_CLASS_PROPERTY).instantiate();
        } catch (MCRConfigurationException e) {
            exception = e;
        }

        // all the indications a nested class should not be instantiated (or instantiation should be suppressed)
        boolean shouldNotInstantiateNestedClass = false;
        shouldNotInstantiateNestedClass |= classProperty == ClassProperty.NOT_SET;
        shouldNotInstantiateNestedClass |= classProperty == ClassProperty.SET_EMPTY;
        shouldNotInstantiateNestedClass |= sentinel == Sentinel.IMPLICITLY_DISABLED;
        shouldNotInstantiateNestedClass |= sentinel == Sentinel.EXPLICITLY_DISABLED;

        if (required && shouldNotInstantiateNestedClass) {

            assertNull(instance);
            assertNotNull(exception);

            assertEquals("Instance map, configured in Foo.Nested (and its sub-properties)," +
                " for target field 'nestedMap' in configured class " + configuredClass.getName()
                + " is empty", exception.getMessage());

        } else {

            assertNull(exception);
            assertNotNull(instance);

            Map<String, Nested> nestedMap = instance.nestedMap();
            assertNotNull(nestedMap);

            if (shouldNotInstantiateNestedClass) {

                assertTrue(nestedMap.isEmpty());

            } else {

                assertEquals(1, nestedMap.size());
                Nested nested = nestedMap.get("foo");
                assertNotNull(nested);

                switch (valueProperty) {
                    case NOT_SET -> assertNull(nested.value);
                    case SET_EMPTY -> assertEquals("", nested.value);
                    case SET_NON_EMPTY -> assertEquals("Value", nested.value);
                }

            }

        }

    }

    private static String get(Map<String, String> map, String key) {
        String value = map.get(key);
        return value == null ? "null" : "'" + value + "'";
    }

    enum ClassProperty {

        NOT_SET,

        SET_EMPTY,

        SET_NON_EMPTY

    }

    enum Sentinel {

        NONE,

        IMPLICITLY_ENABLED,

        EXPLICITLY_ENABLED,

        IMPLICITLY_DISABLED,

        EXPLICITLY_DISABLED

    }

    enum ValueProperty {

        NOT_SET,

        SET_EMPTY,

        SET_NON_EMPTY

    }

    public interface Configurable {

        Map<String, Nested> nestedMap();

    }

    public static class NotRequiredNoSentinel implements Configurable {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class, required = false)
        public Map<String, Nested> nestedMap;

        @Override
        public Map<String, Nested> nestedMap() {
            return nestedMap;
        }

    }

    public static class NotRequiredEnablingSentinel implements Configurable {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class, required = false, sentinel = @MCRSentinel)
        public Map<String, Nested> nestedMap;

        @Override
        public Map<String, Nested> nestedMap() {
            return nestedMap;
        }

    }

    public static class NotRequiredDisablingSentinel implements Configurable {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class, required = false,
            sentinel = @MCRSentinel(defaultValue = false))
        public Map<String, Nested> nestedMap;

        @Override
        public Map<String, Nested> nestedMap() {
            return nestedMap;
        }

    }

    public static class RequiredNoSentinel implements Configurable {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class)
        public Map<String, Nested> nestedMap;

        @Override
        public Map<String, Nested> nestedMap() {
            return nestedMap;
        }

    }

    public static class RequiredEnablingSentinel implements Configurable {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class, sentinel = @MCRSentinel)
        public Map<String, Nested> nestedMap;

        @Override
        public Map<String, Nested> nestedMap() {
            return nestedMap;
        }

    }

    public static class RequiredDisablingSentinel implements Configurable {

        @MCRInstanceMap(name = "Nested", valueClass = Nested.class,
            sentinel = @MCRSentinel(defaultValue = false))
        public Map<String, Nested> nestedMap;

        @Override
        public Map<String, Nested> nestedMap() {
            return nestedMap;
        }

    }

    public static class Nested {

        @MCRProperty(name = "Value", required = false)
        public String value;

    }

}

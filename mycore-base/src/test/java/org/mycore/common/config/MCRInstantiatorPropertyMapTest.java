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
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.test.MyCoReTest;

/**
 * Tests for the following conditions:
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

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String CONFIGURED_CLASS_PROPERTY = "Foo.Class";

    public static final String MAP_PROPERTY = "Foo.Map";

    public static final String MAP_PROPERTY_NON_EMPTY = "Foo.Map.nonEmpty";

    public static final String MAP_PROPERTY_EMPTY = "Foo.Map.empty";

    public static final String DEFAULT_MAP_PROPERTY = "MCR.Map";

    public static final String DEFAULT_MAP_PROPERTY_NON_EMPTY = "MCR.Map.nonEmpty";

    public static final String DEFAULT_MAP_PROPERTY_EMPTY = "MCR.Map.empty";

    private static Stream<Arguments> provideAllParameterCombinations() {
        List<Arguments> argumentsList = new ArrayList<>();
        for (Boolean required : List.of(false, true)) {
            for (ValueProperty valueProperty : ValueProperty
                .values()) {
                for (Boolean defaultValue : List.of(false, true)) {
                    for (DefaultProperty defaultProperty : DefaultProperty
                        .values()) {
                        argumentsList.add(Arguments.of(required, valueProperty, defaultValue, defaultProperty));
                    }
                }
            }
        }
        return argumentsList.stream();
    }

    @ParameterizedTest
    @MethodSource("provideAllParameterCombinations")
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = MAP_PROPERTY, empty = true),
        @MCRTestProperty(key = MAP_PROPERTY_NON_EMPTY, empty = true),
        @MCRTestProperty(key = MAP_PROPERTY_EMPTY, empty = true),
        @MCRTestProperty(key = DEFAULT_MAP_PROPERTY, empty = true),
        @MCRTestProperty(key = DEFAULT_MAP_PROPERTY_NON_EMPTY, empty = true),
        @MCRTestProperty(key = DEFAULT_MAP_PROPERTY_EMPTY, empty = true)
    })
    void test(boolean required, ValueProperty valueProperty,
        boolean defaultValue, DefaultProperty defaultProperty) {

        // log all parameters
        LOGGER.info("TEST PARAMETERS");
        LOGGER.info("required={}", required);
        LOGGER.info("valueProperty={}", valueProperty);
        LOGGER.info("defaultValue={}", defaultValue);
        LOGGER.info("defaultProperty={}", defaultProperty);

        // select class to be configured
        Class<? extends Configurable> configuredClass;
        if (required) {
            if (defaultValue) {
                configuredClass = RequiredDefaultSet.class;
            } else {
                configuredClass = RequiredDefaultNotSet.class;
            }
        } else {
            if (defaultValue) {
                configuredClass = NotRequiredDefaultSet.class;
            } else {
                configuredClass = NotRequiredDefaultNotSet.class;
            }
        }

        // set class property for configured instance
        MCRConfiguration2.set(CONFIGURED_CLASS_PROPERTY, configuredClass.getName());

        // set class property for nested instance
        // empty strings are default via @MCRTestProperty above, may need to overwrite
        switch (valueProperty) {
            case NOT_SET -> {
                MCRConfiguration2.set(MAP_PROPERTY, (String) null);
                MCRConfiguration2.set(MAP_PROPERTY_NON_EMPTY, (String) null);
                MCRConfiguration2.set(MAP_PROPERTY_EMPTY, (String) null);
            }
            case SET_EMPTY -> {
                MCRConfiguration2.set(MAP_PROPERTY, "");
                MCRConfiguration2.set(MAP_PROPERTY_NON_EMPTY, (String) null);
                MCRConfiguration2.set(MAP_PROPERTY_EMPTY, (String) null);
            }
            case SET_SHORT_FORM -> {
                MCRConfiguration2.set(MAP_PROPERTY, "nonEmpty:Value,empty:");
                MCRConfiguration2.set(MAP_PROPERTY_NON_EMPTY, (String) null);
                MCRConfiguration2.set(MAP_PROPERTY_EMPTY, (String) null);
            }
            case SET_LONG_FORM -> {
                MCRConfiguration2.set(MAP_PROPERTY, (String) null);
                MCRConfiguration2.set(MAP_PROPERTY_NON_EMPTY, "Value");
            }
        }

        // set value property for property of nested instance
        // empty string is default via @MCRTestProperty above, may need to overwrite
        switch (defaultProperty) {
            case NOT_SET -> {
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY, (String) null);
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY_NON_EMPTY, (String) null);
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY_EMPTY, (String) null);
            }
            case SET_EMPTY -> {
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY, "");
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY_NON_EMPTY, (String) null);
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY_EMPTY, (String) null);
            }
            case SET_SHORT_FORM -> {
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY, "nonEmpty:DefaultValue,empty:");
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY_NON_EMPTY, (String) null);
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY_EMPTY, (String) null);
            }
            case SET_LONG_FORM -> {
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY, (String) null);
                MCRConfiguration2.set(DEFAULT_MAP_PROPERTY_NON_EMPTY, "DefaultValue");
            }
        }

        // log all relevant configuration entries
        LOGGER.info("CONFIGURATION PROPERTIES");
        Map<String, String> propertiesMap = MCRConfiguration2.getPropertiesMap();
        LOGGER.info("{}={}", CONFIGURED_CLASS_PROPERTY, get(propertiesMap, CONFIGURED_CLASS_PROPERTY));
        LOGGER.info("{}={}", MAP_PROPERTY, get(propertiesMap, MAP_PROPERTY));
        LOGGER.info("{}={}", MAP_PROPERTY_NON_EMPTY, get(propertiesMap, MAP_PROPERTY_NON_EMPTY));
        LOGGER.info("{}={}", MAP_PROPERTY_EMPTY, get(propertiesMap, MAP_PROPERTY_EMPTY));
        LOGGER.info("{}={}", DEFAULT_MAP_PROPERTY, get(propertiesMap, DEFAULT_MAP_PROPERTY));
        LOGGER.info("{}={}", DEFAULT_MAP_PROPERTY_NON_EMPTY, get(propertiesMap, DEFAULT_MAP_PROPERTY_NON_EMPTY));
        LOGGER.info("{}={}", DEFAULT_MAP_PROPERTY_EMPTY, get(propertiesMap, DEFAULT_MAP_PROPERTY_EMPTY));

        // perform instantiation of configured class
        Configurable instance = null;
        MCRConfigurationException exception = null;
        try {
            instance = MCRInstanceConfiguration.ofName(Configurable.class, CONFIGURED_CLASS_PROPERTY).instantiate();
        } catch (MCRConfigurationException e) {
            exception = e;
        }

        boolean missingDefaultConfiguration = false;
        missingDefaultConfiguration |= valueProperty.notSet() && defaultValue && defaultProperty.notSet();
        missingDefaultConfiguration |= required && valueProperty.notSet() && defaultValue && defaultProperty.setEmpty();

        // all the indications a nested property should not be created (or creation should be suppressed)
        boolean shouldNotCreateProperty = false;
        shouldNotCreateProperty |= valueProperty.notSet() && !defaultValue;
        shouldNotCreateProperty |= valueProperty.notSet() && defaultValue && defaultProperty.setEmpty();
        shouldNotCreateProperty |= valueProperty.setEmpty();

        if (missingDefaultConfiguration) {

            assertNull(instance);
            assertNotNull(exception);

            assertEquals("Default property map, configured in MCR.Map (and its sub-properties),"
                + " for target field 'map' in configured class " + configuredClass.getName()
                + " is empty", exception.getMessage());

        } else if (required && shouldNotCreateProperty) {

            assertNull(instance);
            assertNotNull(exception);

            assertEquals("Property map, configured in Foo.Map (and its sub-properties),"
                + " for target field 'map' in configured class " + configuredClass.getName()
                + " is empty", exception.getMessage());

        } else {

            assertNull(exception);
            assertNotNull(instance);

            Map<String, String> list = instance.map();
            assertNotNull(list);

            if (shouldNotCreateProperty) {

                assertTrue(list.isEmpty());

            } else {

                assertEquals(1, list.size());
                String value = list.get("nonEmpty");
                assertNotNull(value);

                if (valueProperty.notSet()) {
                    assertEquals("DefaultValue", value);
                } else {
                    assertEquals("Value", value);
                }

            }

        }

    }

    private static String get(Map<String, String> map, String key) {
        String value = map.get(key);
        return value == null ? "null" : "'" + value + "'";
    }

    enum ValueProperty {

        NOT_SET,

        SET_EMPTY,

        SET_SHORT_FORM,

        SET_LONG_FORM;

        public boolean notSet() {
            return this == NOT_SET;
        }

        public boolean setEmpty() {
            return this == SET_EMPTY;
        }

    }

    enum DefaultProperty {

        NOT_SET,

        SET_EMPTY,

        SET_SHORT_FORM,

        SET_LONG_FORM;

        public boolean notSet() {
            return this == NOT_SET;
        }

        public boolean setEmpty() {
            return this == SET_EMPTY;
        }

    }

    public interface Configurable {

        Map<String, String> map();

    }

    public static class NotRequiredDefaultNotSet implements Configurable {

        @MCRPropertyMap(name = "Map", required = false)
        public Map<String, String> map;

        @Override
        public Map<String, String> map() {
            return map;
        }

    }

    public static class NotRequiredDefaultSet implements Configurable {

        @MCRPropertyMap(name = "Map", required = false, defaultName = "MCR.Map")
        public Map<String, String> map;

        @Override
        public Map<String, String> map() {
            return map;
        }

    }

    public static class RequiredDefaultNotSet implements Configurable {

        @MCRPropertyMap(name = "Map")
        public Map<String, String> map;

        @Override
        public Map<String, String> map() {
            return map;
        }

    }

    public static class RequiredDefaultSet implements Configurable {

        @MCRPropertyMap(name = "Map", defaultName = "MCR.Map")
        public Map<String, String> map;

        @Override
        public Map<String, String> map() {
            return map;
        }

    }

}

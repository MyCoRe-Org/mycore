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
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.test.MyCoReTest;

/**
 * Tests for the following conditions:
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
public class MCRConfigurableInstancePropertyTest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String CONFIGURED_CLASS_PROPERTY = "Foo.Class";

    public static final String VALUE_PROPERTY = "Foo.Value";

    public static final String DEFAULT_VALUE_PROPERTY = "MCR.Value";

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
        @MCRTestProperty(key = VALUE_PROPERTY, empty = true),
        @MCRTestProperty(key = DEFAULT_VALUE_PROPERTY, empty = true)
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
            case NOT_SET -> MCRConfiguration2.set(VALUE_PROPERTY, (String) null);
            case SET_EMPTY -> MCRConfiguration2.set(VALUE_PROPERTY, "");
            case SET_NON_EMPTY -> MCRConfiguration2.set(VALUE_PROPERTY, "Value");
        }

        // set value property for property of nested instance
        // empty string is default via @MCRTestProperty above, may need to overwrite
        switch (defaultProperty) {
            case NOT_SET -> MCRConfiguration2.set(DEFAULT_VALUE_PROPERTY, (String) null);
            case SET_EMPTY -> MCRConfiguration2.set(DEFAULT_VALUE_PROPERTY, "");
            case SET_NON_EMPTY -> MCRConfiguration2.set(DEFAULT_VALUE_PROPERTY, "DefaultValue");
        }

        // log all relevant configuration entries
        LOGGER.info("CONFIGURATION PROPERTIES");
        Map<String, String> propertiesMap = MCRConfiguration2.getPropertiesMap();
        LOGGER.info("{}={}", CONFIGURED_CLASS_PROPERTY, get(propertiesMap, CONFIGURED_CLASS_PROPERTY));
        LOGGER.info("{}={}", VALUE_PROPERTY, get(propertiesMap, VALUE_PROPERTY));
        LOGGER.info("{}={}", DEFAULT_VALUE_PROPERTY, get(propertiesMap, DEFAULT_VALUE_PROPERTY));

        // perform instantiation of configured class
        Configurable instance = null;
        MCRConfigurationException exception = null;
        try {
            MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName(CONFIGURED_CLASS_PROPERTY);
            instance = MCRConfigurableInstanceHelper.getInstance(Configurable.class, configuration);
        } catch (MCRConfigurationException e) {
            exception = e;
        }

        boolean missingDefaultConfiguration = valueProperty.notSet() && defaultValue && defaultProperty.notSet();

        // all the indications a nested property should not be created (or creation should be suppressed)
        boolean shouldNotCreateProperty = valueProperty.notSet() && !defaultValue;

        if (missingDefaultConfiguration) {

            assertNull(instance);
            assertNotNull(exception);

            assertEquals("Default property, configured in MCR.Value (and its sub-properties),"
                + " for target field 'value' in configured class " + configuredClass.getName()
                + " is missing", exception.getMessage());

        } else if (required && shouldNotCreateProperty) {

            assertNull(instance);
            assertNotNull(exception);

            assertEquals("Property, configured in Foo.Value (and its sub-properties),"
                + " for target field 'value' in configured class " + configuredClass.getName()
                + " is missing", exception.getMessage());

        } else {

            assertNull(exception);
            assertNotNull(instance);

            String value = instance.value();

            if (shouldNotCreateProperty) {

                assertNull(value);

            } else {

                assertNotNull(value);

                if (valueProperty.notSet()) {

                    if (defaultProperty.setEmpty()) {
                        assertEquals("", value);
                    } else {
                        assertEquals("DefaultValue", value);
                    }

                } else {

                    if (valueProperty.setEmpty()) {
                        assertEquals("", value);
                    } else {
                        assertEquals("Value", value);
                    }

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

        SET_NON_EMPTY;

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

        SET_NON_EMPTY;

        public boolean notSet() {
            return this == NOT_SET;
        }

        public boolean setEmpty() {
            return this == SET_EMPTY;
        }

    }

    public interface Configurable {

        String value();

    }

    public static class NotRequiredDefaultNotSet implements Configurable {

        @MCRProperty(name = "Value", required = false)
        public String value;

        @Override
        public String value() {
            return value;
        }

    }

    public static class NotRequiredDefaultSet implements Configurable {

        @MCRProperty(name = "Value", required = false, defaultName = "MCR.Value")
        public String value;

        @Override
        public String value() {
            return value;
        }

    }

    public static class RequiredDefaultNotSet implements Configurable {

        @MCRProperty(name = "Value")
        public String value;

        @Override
        public String value() {
            return value;
        }

    }

    public static class RequiredDefaultSet implements Configurable {

        @MCRProperty(name = "Value", defaultName = "MCR.Value")
        public String value;

        @Override
        public String value() {
            return value;
        }

    }

}

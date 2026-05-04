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
import org.mycore.common.config.annotation.MCRPropertyList;
import org.mycore.test.MyCoReTest;

/**
 * Tests for the following conditions:
 * <ol>
 *   <li>Annotation has <code>required = false</code> or not</li>
 *   <li>Property value (for a single-element list) is not set, set empty in short form,
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
public class MCRConfigurableInstancePropertyListTest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String CONFIGURED_CLASS_PROPERTY = "Foo.Class";

    public static final String LIST_PROPERTY = "Foo.List";

    public static final String LIST_PROPERTY_1 = "Foo.List.1";

    public static final String LIST_PROPERTY_2 = "Foo.List.2";

    public static final String DEFAULT_LIST_PROPERTY = "MCR.List";

    public static final String DEFAULT_LIST_PROPERTY_1 = "MCR.List.1";

    public static final String DEFAULT_LIST_PROPERTY_2 = "MCR.List.2";

    private static Stream<Arguments> provideAllParameterCombinations() {
        List<Arguments> argumentsList = new ArrayList<>();
        for (Boolean required : List.of(false, true)) {
            for (ValueProperty valueProperty : ValueProperty.values()) {
                for (Boolean defaultValue : List.of(false, true)) {
                    for (DefaultProperty defaultProperty : DefaultProperty.values()) {
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
        @MCRTestProperty(key = LIST_PROPERTY, empty = true),
        @MCRTestProperty(key = LIST_PROPERTY_1, empty = true),
        @MCRTestProperty(key = LIST_PROPERTY_2, empty = true),
        @MCRTestProperty(key = DEFAULT_LIST_PROPERTY, empty = true),
        @MCRTestProperty(key = DEFAULT_LIST_PROPERTY_1, empty = true),
        @MCRTestProperty(key = DEFAULT_LIST_PROPERTY_2, empty = true)
    })
    void test(boolean required, ValueProperty valueProperty, boolean defaultValue, DefaultProperty defaultProperty) {

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
                MCRConfiguration2.set(LIST_PROPERTY, (String) null);
                MCRConfiguration2.set(LIST_PROPERTY_1, (String) null);
                MCRConfiguration2.set(LIST_PROPERTY_2, (String) null);
            }
            case SET_EMPTY -> {
                MCRConfiguration2.set(LIST_PROPERTY, "");
                MCRConfiguration2.set(LIST_PROPERTY_1, (String) null);
                MCRConfiguration2.set(LIST_PROPERTY_2, (String) null);
            }
            case SET_SHORT_FORM -> {
                MCRConfiguration2.set(LIST_PROPERTY, "Value,");
                MCRConfiguration2.set(LIST_PROPERTY_1, (String) null);
                MCRConfiguration2.set(LIST_PROPERTY_2, (String) null);
            }
            case SET_LONG_FORM -> {
                MCRConfiguration2.set(LIST_PROPERTY, (String) null);
                MCRConfiguration2.set(LIST_PROPERTY_1, "Value");
            }
        }

        // set value property for property of nested instance
        // empty string is default via @MCRTestProperty above, may need to overwrite
        switch (defaultProperty) {
            case NOT_SET -> {
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY, (String) null);
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY_1, (String) null);
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY_2, (String) null);
            }
            case SET_EMPTY -> {
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY, "");
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY_1, (String) null);
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY_2, (String) null);
            }
            case SET_SHORT_FORM -> {
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY, "DefaultValue,");
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY_1, (String) null);
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY_2, (String) null);
            }
            case SET_LONG_FORM -> {
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY, (String) null);
                MCRConfiguration2.set(DEFAULT_LIST_PROPERTY_1, "DefaultValue");
            }
        }

        // log all relevant configuration entries
        LOGGER.info("CONFIGURATION PROPERTIES");
        Map<String, String> propertiesMap = MCRConfiguration2.getPropertiesMap();
        LOGGER.info("{}={}", CONFIGURED_CLASS_PROPERTY, get(propertiesMap, CONFIGURED_CLASS_PROPERTY));
        LOGGER.info("{}={}", LIST_PROPERTY, get(propertiesMap, LIST_PROPERTY));
        LOGGER.info("{}={}", LIST_PROPERTY_1, get(propertiesMap, LIST_PROPERTY_1));
        LOGGER.info("{}={}", LIST_PROPERTY_2, get(propertiesMap, LIST_PROPERTY_2));
        LOGGER.info("{}={}", DEFAULT_LIST_PROPERTY, get(propertiesMap, DEFAULT_LIST_PROPERTY));
        LOGGER.info("{}={}", DEFAULT_LIST_PROPERTY_1, get(propertiesMap, DEFAULT_LIST_PROPERTY_1));
        LOGGER.info("{}={}", DEFAULT_LIST_PROPERTY_2, get(propertiesMap, DEFAULT_LIST_PROPERTY_2));

        // perform instantiation of configured class
        Configurable instance = null;
        MCRConfigurationException exception = null;
        try {
            MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName(CONFIGURED_CLASS_PROPERTY);
            instance = MCRConfigurableInstanceHelper.getInstance(Configurable.class, configuration);
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

            assertEquals("Default property list, configured in MCR.List (and its sub-properties),"
                + " for target field 'list' in configured class " + configuredClass.getName()
                + " is empty", exception.getMessage());

        } else if (required && shouldNotCreateProperty) {

            assertNull(instance);
            assertNotNull(exception);

            assertEquals("Property list, configured in Foo.List (and its sub-properties),"
                + " for target field 'list' in configured class " + configuredClass.getName()
                + " is empty", exception.getMessage());

        } else {

            assertNull(exception);
            assertNotNull(instance);

            List<String> list = instance.list();
            assertNotNull(list);

            if (shouldNotCreateProperty) {

                assertTrue(list.isEmpty());

            } else {

                assertEquals(1, list.size());
                String value = list.getFirst();
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

        List<String> list();

    }

    public static class NotRequiredDefaultNotSet implements Configurable {

        @MCRPropertyList(name = "List", required = false)
        public List<String> list;

        @Override
        public List<String> list() {
            return list;
        }

    }

    public static class NotRequiredDefaultSet implements Configurable {

        @MCRPropertyList(name = "List", required = false, defaultName = "MCR.List")
        public List<String> list;

        @Override
        public List<String> list() {
            return list;
        }

    }

    public static class RequiredDefaultNotSet implements Configurable {

        @MCRPropertyList(name = "List")
        public List<String> list;

        @Override
        public List<String> list() {
            return list;
        }

    }

    public static class RequiredDefaultSet implements Configurable {

        @MCRPropertyList(name = "List", defaultName = "MCR.List")
        public List<String> list;

        @Override
        public List<String> list() {
            return list;
        }

    }

}

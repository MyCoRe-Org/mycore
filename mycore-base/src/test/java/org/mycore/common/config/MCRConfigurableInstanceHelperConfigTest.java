/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;

public class MCRConfigurableInstanceHelperConfigTest extends MCRTestCase {

    public static final String INSTANCE_NAME_PREFIX = "MCR.CIH.";

    private static final String INSTANCE_1_NAME = INSTANCE_NAME_PREFIX + "Test1";

    private static final String INSTANCE_2_NAME = INSTANCE_NAME_PREFIX + "Test2";

    private static final String DEFAULT_KEY = "DefaultKey";

    private static final String DEFAULT_VALUE = "DefaultValue";

    private static final String ASSIGNED_KEY = "AssignedKey";

    private static final String ASSIGNED_VALUE = "AssignedValue";

    private static final String UNASSIGNED_KEY = "UnassignedKey";

    private static final String KEY_REQUIRED_FIELD = "KeyRequiredField";

    private static final String VALUE_REQUIRED_FIELD = "ValueRequiredField";

    private static final String KEY_REQUIRED_FIELD_WITH_DEFAULT = "KeyRequiredFieldWithDefault";

    private static final String KEY_ABSENT_OPTIONAL_FIELD = "KeyAbsentOptionalField";

    private static final String KEY_PRESENT_OPTIONAL_FIELD = "KeyPresentOptionalField";

    private static final String VALUE_PRESENT_OPTIONAL_FIELD = "ValuePresentOptionalField";

    private static final String KEY_ABSENT_OPTIONAL_FIELD_WITH_DEFAULT = "KeyAbsentOptionalFieldWithDefault";

    private static final String KEY_PRESENT_OPTIONAL_FIELD_WITH_DEFAULT = "KeyPresentOptionalFieldWithDefault";

    private static final String VALUE_PRESENT_OPTIONAL_FIELD_WITH_DEFAULT = "ValuePresentOptionalFieldWithDefault";

    private static final String KEY_ABSOLUTE_FIELD = "KeyAbsoluteField";

    private static final String VALUE_ABSOLUTE_FIELD = "ValueAbsoluteField";

    private static final String KEY_REQUIRED_METHOD = "KeyRequiredMethod";

    private static final String VALUE_REQUIRED_METHOD = "ValueRequiredMethod";

    private static final String KEY_REQUIRED_METHOD_WITH_DEFAULT = "KeyRequiredMethodWithDefault";

    private static final String KEY_ABSENT_OPTIONAL_METHOD = "KeyAbsentOptionalMethod";

    private static final String KEY_PRESENT_OPTIONAL_METHOD = "KeyPresentOptionalMethod";

    private static final String VALUE_PRESENT_OPTIONAL_METHOD = "ValuePresentOptionalMethod";

    private static final String KEY_ABSENT_OPTIONAL_METHOD_WITH_DEFAULT = "KeyAbsentOptionalMethodWithDefault";

    private static final String KEY_PRESENT_OPTIONAL_METHOD_DEFAULT = "KeyPresentOptionalMethodWithDefault";

    private static final String VALUE_PRESENT_OPTIONAL_METHOD_DEFAULT = "ValuePresentOptionalMethodWithDefault";

    private static final String KEY_ABSOLUTE_METHOD = "KeyAbsoluteMethod";

    private static final String VALUE_ABSOLUTE_METHOD = "ValueAbsoluteMethod";

    private static final String KEY_CONVERTING_METHOD = "KeyConvertingMethod";

    private static final String VALUE_CONVERTING_METHOD = "ValueConvertingMethod";

    private static final String KEY_ORDERED_METHOD = "KeyOrderedMethod";

    private static final String VALUE_ORDERED_METHOD = "ValueOrderedMethod";

    private static final String KEY_ORDERED_METHOD_1 = "KeyOrderedMethod1";

    private static final String VALUE_ORDERED_METHOD_1 = "ValueOrderedMethod1";

    private static final String KEY_ORDERED_METHOD_2 = "KeyOrderedMethod2";

    private static final String VALUE_ORDERED_METHOD_2 = "ValueOrderedMethod2";

    public static final List<String> VALUES_ORDERED_METHOD = Arrays.asList(
        VALUE_ORDERED_METHOD,
        VALUE_ORDERED_METHOD_1,
        VALUE_ORDERED_METHOD_2
    );

    private static final String VALUE_ORDERED_POST_CONSTRUCTION = "ValueOrderedPostConstruction";

    private static final String VALUE_ORDERED_POST_CONSTRUCTION_1 = "ValueOrderedPostConstruction1";

    private static final String VALUE_ORDERED_POST_CONSTRUCTION_2 = "ValueOrderedPostConstruction2";

    public static final List<String> VALUES_ORDERED_POST_CONSTRUCTION = Arrays.asList(
        VALUE_ORDERED_POST_CONSTRUCTION,
        VALUE_ORDERED_POST_CONSTRUCTION_1,
        VALUE_ORDERED_POST_CONSTRUCTION_2
    );

    public static final List<String> VALUES_ORDERED_OVERALL = Stream.concat(
        VALUES_ORDERED_METHOD.stream(),
        VALUES_ORDERED_POST_CONSTRUCTION.stream()
    ).toList();

    @Test
    public void test() {

        Map<String, ?> instances = MCRConfiguration2.getInstances(INSTANCE_NAME_PREFIX);
        assertEquals("Except two instances!", 2, instances.size());

        testInstance(INSTANCE_1_NAME, false);
        testInstance(INSTANCE_2_NAME, true);

    }

    private void testInstance(String instanceName, boolean withClassSuffix) {

        String fullInstanceName = withClassSuffix(instanceName, withClassSuffix);

        List<String> list = MCRConfiguration2.getInstantiatablePropertyKeys(fullInstanceName).toList();
        assertTrue("Properties should contain " + instanceName, list.contains(fullInstanceName));

        Optional<ConfigurableTestInstance> instance = MCRConfigurableInstanceHelper.getInstance(fullInstanceName);
        assertTrue("Test " + fullInstanceName + " should be present", instance.isPresent());

        validateFields(instance.get());
        validateMethods(instance.get());
        validatePostConstruction(instance.get(), fullInstanceName);

    }

    void validateFields(ConfigurableTestInstance instance) {

        assertTrue("The map field should contain the assigned test key",
            instance.map.containsKey(ASSIGNED_KEY));

        assertEquals("The assigned test property should be present in map field",
            ASSIGNED_VALUE, instance.map.get(ASSIGNED_KEY));

        assertFalse("The unassigned test property should not be present in map field",
            instance.map.containsKey(UNASSIGNED_KEY));

        assertEquals("The required field should match",
            VALUE_REQUIRED_FIELD, instance.required);

        assertEquals("The required field with default should match",
            DEFAULT_VALUE, instance.requiredWithDefault);

        assertNull("The absent optional field should be null",
            instance.absentOptional);

        assertEquals("The present optional field should match",
            VALUE_PRESENT_OPTIONAL_FIELD, instance.presentOptional);

        assertEquals("The absent optional field with default should match",
            DEFAULT_VALUE, instance.absentOptionalWithDefault);

        assertEquals("The present optional field with default should match",
            VALUE_PRESENT_OPTIONAL_FIELD_WITH_DEFAULT, instance.presentOptionalWithDefault);

        assertEquals("The absolute field should match",
            VALUE_ABSOLUTE_FIELD, instance.absolute);

    }

    void validateMethods(ConfigurableTestInstance instance) {

        assertTrue("The map method value should contain the assigned test key",
            instance.getMap().containsKey(ASSIGNED_KEY));

        assertEquals("The assigned test property should be present in map method value",
            ASSIGNED_VALUE, instance.getMap().get(ASSIGNED_KEY));

        assertFalse("The unassigned test property should not be present in map method value",
            instance.getMap().containsKey(UNASSIGNED_KEY));

        assertEquals("The required method value should match",
            VALUE_REQUIRED_METHOD, instance.getRequired());

        assertEquals("The required method value with default should match",
            DEFAULT_VALUE, instance.getRequiredWithDefault());

        assertNull("The absent optional method value should be null",
            instance.getAbsentOptional());

        assertEquals("The present optional method value should match",
            VALUE_PRESENT_OPTIONAL_METHOD, instance.getPresentOptional());

        assertEquals("The absent optional method value with default should match",
            DEFAULT_VALUE, instance.getAbsentOptionalWithDefault());

        assertEquals("The present optional method value with default should match",
            VALUE_PRESENT_OPTIONAL_METHOD_DEFAULT, instance.getPresentOptionalWithDefault());

        assertEquals("The absolute method value should match",
            VALUE_ABSOLUTE_METHOD, instance.getAbsolute());

        assertEquals("The converting method value should match",
            VALUE_CONVERTING_METHOD.length(), instance.getConverting());

        assertEquals("The ordered method values should match",
            VALUES_ORDERED_METHOD, instance.getOrderedMethodValues());

    }

    void validatePostConstruction(ConfigurableTestInstance instance, String fullInstanceName) {

        assertEquals("Post construction value should match",
            fullInstanceName, instance.postConstructionProperty);

        assertEquals("The ordered post construction values should match",
            VALUES_ORDERED_POST_CONSTRUCTION, instance.getOrderedPostConstructionValues());

        assertEquals("The ordered method and post construction values should match",
            VALUES_ORDERED_OVERALL, instance.getOrderedOverallValues());

    }

    @Override
    protected Map<String, String> getTestProperties() {

        final Map<String, String> testProperties = super.getTestProperties();

        testProperties.put(DEFAULT_KEY, DEFAULT_VALUE);
        testProperties.put(KEY_ABSOLUTE_FIELD, VALUE_ABSOLUTE_FIELD);
        testProperties.put(KEY_ABSOLUTE_METHOD, VALUE_ABSOLUTE_METHOD);

        configureInstance(testProperties, INSTANCE_1_NAME, false);
        configureInstance(testProperties, INSTANCE_2_NAME, true);

        return testProperties;
    }

    private void configureInstance(Map<String, String> testProperties, String instanceName, boolean withClassSuffix) {
        String fullInstanceName = withClassSuffix(instanceName, withClassSuffix);
        testProperties.put(fullInstanceName, ConfigurableTestInstance.class.getName());
        testProperties.put(instanceName + "." + ASSIGNED_KEY, ASSIGNED_VALUE);
        testProperties.put(instanceName + "." + KEY_REQUIRED_FIELD, VALUE_REQUIRED_FIELD);
        testProperties.put(instanceName + "." + KEY_PRESENT_OPTIONAL_FIELD, VALUE_PRESENT_OPTIONAL_FIELD);
        testProperties.put(instanceName + "." + KEY_PRESENT_OPTIONAL_FIELD_WITH_DEFAULT,
            VALUE_PRESENT_OPTIONAL_FIELD_WITH_DEFAULT);
        testProperties.put(instanceName + "." + KEY_REQUIRED_METHOD, VALUE_REQUIRED_METHOD);
        testProperties.put(instanceName + "." + KEY_PRESENT_OPTIONAL_METHOD, VALUE_PRESENT_OPTIONAL_METHOD);
        testProperties.put(instanceName + "." + KEY_PRESENT_OPTIONAL_METHOD_DEFAULT,
            VALUE_PRESENT_OPTIONAL_METHOD_DEFAULT);
        testProperties.put(instanceName + "." + KEY_CONVERTING_METHOD, VALUE_CONVERTING_METHOD);
        testProperties.put(instanceName + "." + KEY_ORDERED_METHOD, VALUE_ORDERED_METHOD);
        testProperties.put(instanceName + "." + KEY_ORDERED_METHOD_1, VALUE_ORDERED_METHOD_1);
        testProperties.put(instanceName + "." + KEY_ORDERED_METHOD_2, VALUE_ORDERED_METHOD_2);
    }

    private String withClassSuffix(String instanceName, boolean withClassSuffix) {
        return instanceName + (withClassSuffix ? ".Class" : "");
    }

    public static class ConfigurableTestInstance {

        @MCRProperty(name = "*")
        public Map<String, String> map;

        @MCRProperty(name = KEY_REQUIRED_FIELD)
        public String required;

        @MCRProperty(name = KEY_REQUIRED_FIELD_WITH_DEFAULT, defaultName = DEFAULT_KEY)
        public String requiredWithDefault;

        @MCRProperty(name = KEY_ABSENT_OPTIONAL_FIELD, required = false)
        public String absentOptional;

        @MCRProperty(name = KEY_PRESENT_OPTIONAL_FIELD, required = false)
        public String presentOptional;

        @MCRProperty(name = KEY_ABSENT_OPTIONAL_FIELD_WITH_DEFAULT, defaultName = DEFAULT_KEY, required = false)
        public String absentOptionalWithDefault;

        @MCRProperty(name = KEY_PRESENT_OPTIONAL_FIELD_WITH_DEFAULT, defaultName = DEFAULT_KEY, required = false)
        public String presentOptionalWithDefault;

        @MCRProperty(name = KEY_ABSOLUTE_FIELD, absolute = true)
        public String absolute;

        private Map<String, String> mapMethodValue;

        private String requiredMethodValue;

        private String requiredMethodValueWithDefault;

        private String absentOptionalMethodValue;

        private String presentOptionalMethodValue;

        private String absentOptionalMethodValueWithDefault;

        private String presentOptionalMethodValueWithDefault;

        private String absoluteMethodValue;

        private int convertingMethodValue;

        private final List<String> orderedMethodValues = new ArrayList<>(3);

        public String postConstructionProperty;

        private final List<String> orderedPostConstructionValues = new ArrayList<>(3);

        private final List<String> orderedOverallValues = new ArrayList<>(6);

        public Map<String, String> getMap() {
            return mapMethodValue;
        }

        @MCRProperty(name = "*")
        public void setMap(Map<String, String> mapValue) {
            this.mapMethodValue = mapValue;
        }

        public String getRequired() {
            return requiredMethodValue;
        }

        @MCRProperty(name = KEY_REQUIRED_METHOD)
        public void setRequired(String requiredValue) {
            this.requiredMethodValue = requiredValue;
        }

        public String getRequiredWithDefault() {
            return requiredMethodValueWithDefault;
        }

        @MCRProperty(name = KEY_REQUIRED_METHOD_WITH_DEFAULT, defaultName = DEFAULT_KEY)
        public void setRequiredWithDefault(String requiredValueWithDefault) {
            this.requiredMethodValueWithDefault = requiredValueWithDefault;
        }

        public String getAbsentOptional() {
            return absentOptionalMethodValue;
        }

        @MCRProperty(name = KEY_ABSENT_OPTIONAL_METHOD, required = false)
        public void setAbsentOptional(String absentOptionalValue) {
            this.absentOptionalMethodValue = absentOptionalValue;
        }

        public String getPresentOptional() {
            return presentOptionalMethodValue;
        }

        @MCRProperty(name = KEY_PRESENT_OPTIONAL_METHOD, required = false)
        public void setOPresentOptional(String presentOptionalValue) {
            this.presentOptionalMethodValue = presentOptionalValue;
        }

        public String getAbsentOptionalWithDefault() {
            return absentOptionalMethodValueWithDefault;
        }

        @MCRProperty(name = KEY_ABSENT_OPTIONAL_METHOD_WITH_DEFAULT, defaultName = DEFAULT_KEY, required = false)
        public void setAbsentOptionalWithDefault(String absentOptionalValueWithDefault) {
            this.absentOptionalMethodValueWithDefault = absentOptionalValueWithDefault;
        }

        public String getPresentOptionalWithDefault() {
            return presentOptionalMethodValueWithDefault;
        }

        @MCRProperty(name = KEY_PRESENT_OPTIONAL_METHOD_DEFAULT, defaultName = DEFAULT_KEY, required = false)
        public void setPresentOptionalWithDefault(String presentOptionalValueWithDefault) {
            this.presentOptionalMethodValueWithDefault = presentOptionalValueWithDefault;
        }

        public String getAbsolute() {
            return absoluteMethodValue;
        }

        @MCRProperty(name = KEY_ABSOLUTE_METHOD, absolute = true)
        public void setAbsolute(String absoluteValue) {
            this.absoluteMethodValue = absoluteValue;
        }

        public int getConverting() {
            return convertingMethodValue;
        }

        @MCRProperty(name = KEY_CONVERTING_METHOD)
        public void setConverting(String convertingValue) {
            this.convertingMethodValue = convertingValue.length();
        }

        @MCRProperty(name = KEY_ORDERED_METHOD_2, order = 2)
        public void setOrdered2(String orderedValue) {
            addOrdered(orderedValue);
        }

        @MCRProperty(name = KEY_ORDERED_METHOD_1, order = 1)
        public void setOrdered1(String orderedValue) {
            addOrdered(orderedValue);
        }

        @MCRProperty(name = KEY_ORDERED_METHOD)
        public void setOrdered(String orderedValue) {
            addOrdered(orderedValue);
        }

        private void addOrdered(String orderedValue) {
            this.orderedMethodValues.add(orderedValue);
            this.orderedOverallValues.add(orderedValue);
        }

        public List<String> getOrderedMethodValues() {
            return orderedMethodValues;
        }

        @MCRPostConstruction
        public void callPostConstruction(String property) throws MCRConfigurationException {
            postConstructionProperty = property;
        }

        @MCRPostConstruction(order = 2)
        public void callPostConstructionOrdered2() {
            addPostConstructionOrdered(VALUE_ORDERED_POST_CONSTRUCTION_2);

        }

        @MCRPostConstruction(order = 1)
        public void callPostConstructionOrdered1() {
            addPostConstructionOrdered(VALUE_ORDERED_POST_CONSTRUCTION_1);
        }

        @MCRPostConstruction
        public void callPostConstructionOrdered() {
            addPostConstructionOrdered(VALUE_ORDERED_POST_CONSTRUCTION);
        }

        private void addPostConstructionOrdered(String ordered) {
            this.orderedPostConstructionValues.add(ordered);
            this.orderedOverallValues.add(ordered);
        }

        public List<String> getOrderedPostConstructionValues() {
            return orderedPostConstructionValues;
        }

        public List<String> getOrderedOverallValues() {
            return orderedOverallValues;
        }

    }
}

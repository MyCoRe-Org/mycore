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

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class MCRConfigurableInstanceHelperTest extends MCRTestCase {

    private static final String TEST_KEY = "TestKey";

    private static final String TEST_ANNOT_KEY = "AnnotTestKey";

    private static final String TEST_ANNOT_KEY2 = "AnnotTestKey2";

    private static final Integer TEST_ANNOT_VALUE_CONVERTED = 5;

    private static final String INVALID_TEST_KEY = "InvalidKey";

    private static final String TEST_KEY_VAL = "TestValue";

    private static final String ANNOT_TEST_KEY_VAL1 = "TestValue1";

    private static final String ANNOT_TEST_KEY_VAL2 = "5";

    private static final String INSTANCE_NAME = "MCR.CI.Test";

    private static final String INSTANCE_2_NAME = "MCR.CI.Test2";

    public static final String INSTANCE_2_NAME_W_CLASS = INSTANCE_2_NAME + ".Class";

    @Test
    public void test() {
        Optional<ConfigurableTestInstance> instance1 = MCRConfigurableInstanceHelper.getInstance(INSTANCE_NAME);
        Optional<ConfigurableTestInstance> instance2 = MCRConfigurableInstanceHelper
            .getInstance(INSTANCE_2_NAME_W_CLASS);

        Assert.assertTrue("Test instance should be present", instance1.isPresent());
        Assert.assertTrue("Test instance 2 should be present", instance2.isPresent());

        validate(instance1.get());
        validate(instance2.get());

        List<String> list = MCRConfiguration2.getInstantiatablePropertyKeys("MCR.CI.").collect(Collectors.toList());
        Assert.assertTrue("Properties should contain " + INSTANCE_NAME, list.contains(INSTANCE_NAME));
        Assert.assertTrue("Properties should contain " + INSTANCE_2_NAME_W_CLASS,
            list.contains(INSTANCE_2_NAME_W_CLASS));

        Map<String, Callable<ConfigurableTestInstance>> instances = MCRConfiguration2.getInstances("MCR.CI.");
        Assert.assertEquals("Except two instances!", 2, instances.size());
        instances.values().forEach(v -> {
            try {
                ConfigurableTestInstance cti = v.call();
                validate(cti);
            } catch (Exception e) {
                throw new MCRException(e);
            }
        });
    }

    void validate(ConfigurableTestInstance instance) {
        Assert.assertTrue("The instance should contain the Test key!", instance.getProperties().containsKey(TEST_KEY));
        Assert.assertEquals(TEST_KEY_VAL, instance.getProperties().get(TEST_KEY));

        Assert.assertFalse("the property should not be present",
            instance.getProperties().containsKey(INVALID_TEST_KEY));

        Assert.assertEquals("Annotated field should match!", ANNOT_TEST_KEY_VAL1, instance.getMyAnnotatedProp());

        Assert.assertEquals("Annotated method should put int to the count field!", TEST_ANNOT_VALUE_CONVERTED,
            instance.getCount());

        Assert.assertTrue("Post construction should have been executed", instance.postConstructionCalled);
    }

    @Override
    protected Map<String, String> getTestProperties() {
        final Map<String, String> testProperties = super.getTestProperties();

        testProperties.put(INSTANCE_NAME, ConfigurableTestInstance.class.getName());
        testProperties.put(INSTANCE_NAME + "." + TEST_KEY, TEST_KEY_VAL);
        testProperties.put(INSTANCE_NAME + "." + TEST_ANNOT_KEY, ANNOT_TEST_KEY_VAL1);
        testProperties.put(INSTANCE_NAME + "." + TEST_ANNOT_KEY2, ANNOT_TEST_KEY_VAL2);

        testProperties.put(INSTANCE_2_NAME_W_CLASS, ConfigurableTestInstance.class.getName());
        testProperties.put(INSTANCE_2_NAME + "." + TEST_KEY, TEST_KEY_VAL);
        testProperties.put(INSTANCE_2_NAME + "." + TEST_ANNOT_KEY, ANNOT_TEST_KEY_VAL1);
        testProperties.put(INSTANCE_2_NAME + "." + TEST_ANNOT_KEY2, ANNOT_TEST_KEY_VAL2);

        return testProperties;
    }

    public static class ConfigurableTestInstance {

        @MCRProperty(name = "*")
        public Map<String, String> properties;

        @MCRProperty(name = TEST_ANNOT_KEY)
        public String myAnnotatedProp;

        @MCRProperty(name = "Optional", required = false)
        public String myOptionalProp;

        public boolean postConstructionCalled = false;

        protected Integer count;

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        @MCRProperty(name = TEST_ANNOT_KEY2)
        public void setCount(String count) {
            this.count = Integer.parseInt(count);
        }

        public String getMyAnnotatedProp() {
            return myAnnotatedProp;
        }

        @MCRPostConstruction
        public void checkConfiguration(String prop) throws MCRConfigurationException {
            postConstructionCalled = true;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }
}

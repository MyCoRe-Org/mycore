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

import static org.junit.Assert.assertNotNull;

import java.util.function.Supplier;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRConfigurationProxy;

public class MCRConfigurableInstanceHelperBasicTest extends MCRTestCase {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithConstructor.class)
        }
    )
    public void constructorFactory() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithConstructor instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithFactory.class)
        }
    )
    public void factory() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithFactory instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);

    }

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithoutConstructorOrFactory.class)
        }
    )
    public void noConstructorOrFactory() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(configuration);

    }

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMultipleFactories.class)
        }
    )
    public void multipleFactories() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(configuration);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithConfigurationProxy.class),
            @MCRTestProperty(key = "Foo.Value", string = "Value")
        }
    )
    public void proxyFactory() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithConfigurationProxy instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);

    }

    public static class TestClassWithConstructor {

        public TestClassWithConstructor() {

        }

    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static class TestClassWithFactory {

        private TestClassWithFactory() {

        }

        public static TestClassWithFactory getInstance() {
            return new TestClassWithFactory();
        }

    }

    public static class TestClassWithoutConstructorOrFactory {

        private TestClassWithoutConstructorOrFactory() {

        }

    }

    @SuppressWarnings({"InstantiationOfUtilityClass", "unused"})
    public static class TestClassWithMultipleFactories {

        private TestClassWithMultipleFactories() {

        }

        public static TestClassWithMultipleFactories getInstanceA() {
            return new TestClassWithMultipleFactories();
        }

        public static TestClassWithMultipleFactories getInstanceB() {
            return new TestClassWithMultipleFactories();
        }

    }

    @SuppressWarnings({"unused"})
    @MCRConfigurationProxy(proxyClass = TestClassWithConfigurationProxy.Factory.class)
    public static class TestClassWithConfigurationProxy {


        public TestClassWithConfigurationProxy() {
        }

        public static class Factory implements Supplier<TestClassWithConfigurationProxy> {

            @Override
            public TestClassWithConfigurationProxy get() {
                return new TestClassWithConfigurationProxy();
            }

        }

    }

}

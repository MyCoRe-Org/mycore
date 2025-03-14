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

import static org.junit.Assert.assertNotNull;

import java.util.function.Supplier;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRFactory;

public class MCRConfigurableInstanceHelperBasicTest extends MCRTestCase {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithSingletonFactory.class)
        })
    public void singletonFactory() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithSingletonFactory instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithSingletonFactory.class, configuration);

        assertNotNull(instance);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWitAnnotatedFactory.class)
        })
    public void annotatedFactory() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWitAnnotatedFactory instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWitAnnotatedFactory.class, configuration);

        assertNotNull(instance);

    }

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWitAnnotatedFactories.class)
        })
    public void annotatedFactories() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(TestClassWitAnnotatedFactories.class, configuration);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithConstructor.class)
        })
    public void constructorFactory() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithConstructor instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithConstructor.class, configuration);

        assertNotNull(instance);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWitLegacyFactory.class)
        })
    public void legacyFactory() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWitLegacyFactory instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWitLegacyFactory.class, configuration);

        assertNotNull(instance);

    }

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWitLegacyFactories.class)
        })
    public void legacyFactories() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(TestClassWitLegacyFactories.class, configuration);

    }

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithoutConstructorOrFactory.class)
        })
    public void noConstructorOrFactory() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(Object.class, configuration);

    }

    @Test(expected = MCRConfigurationException.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMultipleFactories.class)
        })
    public void multipleFactories() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        MCRConfigurableInstanceHelper.getInstance(Object.class, configuration);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Value", string = "Value")
        })
    public void configurationImplicit() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        ImplicitTestClass instance = MCRConfigurableInstanceHelper.getInstance(ImplicitTestClass.class, configuration,
            MCRConfigurableInstanceHelper.ADD_IMPLICIT_CLASS_PROPERTIES);

        assertNotNull(instance);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithConfigurationProxy.class),
            @MCRTestProperty(key = "Foo.Value", string = "Value")
        })
    public void configurationProxy() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithConfigurationProxy instance = MCRConfigurableInstanceHelper
            .getInstance(TestClassWithConfigurationProxy.class, configuration);

        assertNotNull(instance);

    }


    @SuppressWarnings("InstantiationOfUtilityClass")
    public static class TestClassWithSingletonFactory {

        public static final TestClassWithSingletonFactory SINGLETON_INSTANCE = new TestClassWithSingletonFactory();

        private TestClassWithSingletonFactory() {
        }

        public static TestClassWithSingletonFactory getInstance() {
            return SINGLETON_INSTANCE;
        }

    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static class TestClassWitAnnotatedFactory {

        private TestClassWitAnnotatedFactory() {
        }

        @MCRFactory
        public static TestClassWitAnnotatedFactory createInstance() {
            return new TestClassWitAnnotatedFactory();
        }

    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static class TestClassWitAnnotatedFactories {

        private TestClassWitAnnotatedFactories() {
        }

        @MCRFactory
        public static TestClassWitAnnotatedFactories obtainInstance() {
            return createInstance();
        }

        @MCRFactory
        public static TestClassWitAnnotatedFactories createInstance() {
            return new TestClassWitAnnotatedFactories();
        }

    }

    public static class TestClassWithConstructor {
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static class TestClassWitLegacyFactory {

        private TestClassWitLegacyFactory() {
        }

        public static TestClassWitLegacyFactory instance() {
            return new TestClassWitLegacyFactory();
        }

    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static class TestClassWitLegacyFactories {

        private TestClassWitLegacyFactories() {
        }

        public static TestClassWitLegacyFactories instance() {
            return createInstance();
        }

        public static TestClassWitLegacyFactories createInstance() {
            return new TestClassWitLegacyFactories();
        }

    }


    public static class TestClassWithoutConstructorOrFactory {

        private TestClassWithoutConstructorOrFactory() {

        }

    }

    @SuppressWarnings({ "InstantiationOfUtilityClass", "unused" })
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

    public static final class ImplicitTestClass {
    }

    @SuppressWarnings({ "unused" })
    @MCRConfigurationProxy(proxyClass = TestClassWithConfigurationProxy.Factory.class)
    public static class TestClassWithConfigurationProxy {

        public static class Factory implements Supplier<TestClassWithConfigurationProxy> {

            @Override
            public TestClassWithConfigurationProxy get() {
                return new TestClassWithConfigurationProxy();
            }

        }

    }

}

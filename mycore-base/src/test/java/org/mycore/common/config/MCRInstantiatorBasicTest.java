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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.IMPLICIT_OPTION;
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.ofName;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRFactory;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRInstantiatorBasicTest {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithSingletonFactory.class)
        })
    public void singletonFactory() {

        TestClassWithSingletonFactory instance = ofName(TestClassWithSingletonFactory.class, "Foo").instantiate();

        assertNotNull(instance);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWitAnnotatedFactory.class)
        })
    public void annotatedFactory() {

        TestClassWitAnnotatedFactory instance = ofName(TestClassWitAnnotatedFactory.class, "Foo").instantiate();

        assertNotNull(instance);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWitAnnotatedFactories.class)
        })
    public void annotatedFactories() {

        assertThrows(MCRConfigurationException.class,
            () -> ofName(TestClassWitAnnotatedFactories.class, "Foo").instantiate());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithConstructor.class)
        })
    public void constructorFactory() {

        TestClassWithConstructor instance = ofName(TestClassWithConstructor.class, "Foo").instantiate();

        assertNotNull(instance);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithoutConstructorOrFactory.class)
        })
    public void noConstructorOrFactory() {

        assertThrows(MCRConfigurationException.class,
            () -> ofName(TestClassWithoutConstructorOrFactory.class, "Foo").instantiate());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithMultipleFactories.class)
        })
    public void multipleFactories() {

        assertThrows(MCRConfigurationException.class,
            () -> ofName(TestClassWithMultipleFactories.class, "Foo").instantiate());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Value", string = "Value")
        })
    public void configurationImplicit() {

        ImplicitTestClass instance = ofName(ImplicitTestClass.class, "Foo", IMPLICIT_OPTION).instantiate();

        assertNotNull(instance);

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithConfigurationProxy.class),
            @MCRTestProperty(key = "Foo.Value", string = "Value")
        })
    public void configurationProxy() {

        TestClassWithConfigurationProxy instance = ofName(TestClassWithConfigurationProxy.class, "Foo").instantiate();

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

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
import static org.mycore.common.config.instantiator.MCRInstanceConfiguration.ofName;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRInstantiatorProxyTest {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = TestClass.class),
            @MCRTestProperty(key = "Foo.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Property2", string = "Value2")
        })
    public void classWithProxy() {

        TestClass instance = ofName(TestClass.class, "Foo").instantiate();

        assertNotNull(instance);
        assertEquals("Value1-Value2", instance.value());

    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo.Class", classNameOf = ExtendedTestClass.class),
            @MCRTestProperty(key = "Foo.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Property2", string = "Value2"),
            @MCRTestProperty(key = "Foo.AdditionalProperty", string = "AdditionalValue")
        })
    public void subClassWithProxy() {

        ExtendedTestClass instance = ofName(ExtendedTestClass.class, "Foo").instantiate();

        assertNotNull(instance);
        assertEquals("Value1-Value2", instance.value());
        assertEquals("AdditionalValue", instance.additionalValue());

    }

    @MCRConfigurationProxy(proxyClass = TestClass.Factory.class)
    public static class TestClass {

        private final String value;

        public TestClass(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static class Factory implements Supplier<TestClass> {

            @MCRProperty(name = "Property1")
            public String value1;

            @MCRProperty(name = "Property2")
            public String value2;

            @Override
            public TestClass get() {
                return new TestClass(getValue());
            }

            protected final String getValue() {
                return value1 + "-" + value2;
            }

        }

    }

    @MCRConfigurationProxy(proxyClass = ExtendedTestClass.Factory.class)
    public static class ExtendedTestClass extends TestClass {

        private final String additionalValue;

        public ExtendedTestClass(String value, String additionalValue) {
            super(value);
            this.additionalValue = additionalValue;
        }

        public String additionalValue() {
            return additionalValue;
        }

        // Java doesn't allow subclasses to implement an interface with a narrower subtype.
        // Therefore, we can not guarantee that ExtendedTestClass#get returns an ExtendedTestClass.
        // This is an argument against extending configuration proxy factories, even though
        // the alternative is duplicating some boilerplate code.
        public static class Factory extends TestClass.Factory /* implements Supplier<ExtendedTestClass> */ {

            @MCRProperty(name = "AdditionalProperty")
            public String additionalValue;

            @Override
            public ExtendedTestClass get() {
                return new ExtendedTestClass(getValue(), additionalValue);
            }

        }

    }

}

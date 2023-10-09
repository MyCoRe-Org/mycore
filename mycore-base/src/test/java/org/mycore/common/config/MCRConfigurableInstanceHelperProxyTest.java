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
import static org.junit.Assert.assertNotNull;

import java.util.function.Supplier;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

public class MCRConfigurableInstanceHelperProxyTest extends MCRTestCase {

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "Foo", classNameOf = TestClassWithConfigurationProxy.class),
            @MCRTestProperty(key = "Foo.Property1", string = "Value1"),
            @MCRTestProperty(key = "Foo.Property2", string = "Value2")
        }
    )
    public void annotated() {

        MCRInstanceConfiguration configuration = MCRInstanceConfiguration.ofName("Foo");
        TestClassWithConfigurationProxy instance = MCRConfigurableInstanceHelper.getInstance(configuration);

        assertNotNull(instance);
        assertEquals("Value1-Value2", instance.value());

    }


    @MCRConfigurationProxy(proxyClass = TestClassWithConfigurationProxy.Factory.class)
    public static class TestClassWithConfigurationProxy {

        private final String value;

        public TestClassWithConfigurationProxy(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static class Factory implements Supplier<TestClassWithConfigurationProxy> {

            @MCRProperty(name = "Property1")
            public String value1;

            @MCRProperty(name = "Property2")
            public String value2;

            @Override
            public TestClassWithConfigurationProxy get() {
                return new TestClassWithConfigurationProxy(value1 + "-" + value2);
            }

        }

    }

}

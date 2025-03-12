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

package org.mycore.test.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRTestExtensionTest {

    @Test
    public void testPropertyFromFile() {
        Assertions.assertEquals("MyCoRe", MCRConfiguration2.getStringOrThrow("MCR.NameOfProject"));
    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "junit", classNameOf = MCRTestExtensionTest.class)
        })
    public void testClassValue() {
        Assertions.assertEquals(MCRTestExtensionTest.class.getName(), MCRConfiguration2.getStringOrThrow("junit"));
    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "junit", empty = true)
        })
    public void testEmptyValue() {
        Assertions.assertTrue(MCRConfigurationBase.getString("junit").isPresent());
        Assertions.assertTrue(MCRConfigurationBase.getString("junit").get().isEmpty());
        Assertions.assertThrowsExactly(MCRConfigurationException.class,
            () -> MCRConfiguration2.getStringOrThrow("junit"));
    }

    @Nested
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "foo", string = "foo"),
            @MCRTestProperty(key = "bar", string = "bar")
        })
    class MCRTestExtensionTest1 {
        @Test
        public void testClassProperty() {
            Assertions.assertEquals("foo", MCRConfiguration2.getStringOrThrow("foo"));
        }

        @Test
        @MCRTestConfiguration(
            properties = {
                @MCRTestProperty(key = "foo2", string = "foo2")
            })
        public void testMethodProperty() {
            Assertions.assertEquals("foo2", MCRConfiguration2.getStringOrThrow("foo2"));
        }

    }

    @Nested
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "bar", string = "foo")
        })
    class MCRTestExtensionTest2 extends MCRTestExtensionTest1 {
        @Test
        public void testClassProperty() {
            Assertions.assertEquals("foo", MCRConfiguration2.getStringOrThrow("foo"));
        }

        @Test
        public void testClassPropertyOverwrite() {
            Assertions.assertEquals("foo", MCRConfiguration2.getStringOrThrow("bar"));
        }
    }
}

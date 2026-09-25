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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationBase;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.test.MCRTestExtension;
import org.mycore.test.MyCoReTest;

/**
 * This class is a test for {@link MCRTestExtension}.
 * <p>
 * It tests that unit tests of actual MyCoRe code, annotated with {@link MyCoReTest}, can use the features
 * provided by {@link MCRTestExtension} correctly.
 */
@MyCoReTest
public class MCRTestExtensionTest {

    /**
     * Make sure that tests of actual MyCoRe code can use the MyCoRe-API in
     * set-up methods, annotated with {@link BeforeAll}.
     */
    @BeforeAll
    public static void testSetUp() {
        new MCRObject(); // relies on configuration to be initialized
    }

    /**
     * Make sure that tests of actual MyCoRe code can use the MyCoRe-API in
     * tear-down methods, annotated with {@link AfterAll}.
     */
    @AfterAll
    public static void testTearDown() {
        new MCRObject(); // relies on configuration to be initialized
    }

    @Test
    public void testPropertyFromFile() {
        assertEquals("MyCoRe", MCRConfiguration2.getStringOrThrow("MCR.NameOfProject"));
    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "junit", classNameOf = MCRTestExtensionTest.class)
        })
    public void testClassValue() {
        assertEquals(MCRTestExtensionTest.class.getName(), MCRConfiguration2.getStringOrThrow("junit"));
    }

    @Test
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "junit", empty = true)
        })
    public void testEmptyValue() {
        assertTrue(MCRConfigurationBase.getString("junit").isPresent());
        assertTrue(MCRConfigurationBase.getString("junit").get().isEmpty());
        assertThrowsExactly(MCRConfigurationException.class,
            () -> MCRConfiguration2.getStringOrThrow("junit"));
    }

    @Nested
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "foo", string = "foo"),
            @MCRTestProperty(key = "bar", string = "bar"),
            @MCRTestProperty(key = "hello", classNameOf = SayHello.class)
        })
    class MCRTestExtensionTest1 {
        @Test
        public void testClassProperty() {
            assertEquals("foo", MCRConfiguration2.getStringOrThrow("foo"));
        }

        @Test
        @MCRTestConfiguration(
            properties = {
                @MCRTestProperty(key = "foo2", string = "foo2")
            })
        public void testMethodProperty() {
            assertEquals("foo2", MCRConfiguration2.getStringOrThrow("foo2"));
        }

        @Test
        public void testSayHello() {
            //Checks implementation of the class SayHello or subclasses.
            MCRConfiguration2.getInstanceOf(SayHello.class, "hello").ifPresent(sayHello -> {
                String name = "Junit";
                String helloResponse = sayHello.sayHello(name);
                assertNotNull(helloResponse, "Hello response should not be null");
                System.out.println(helloResponse);
                assertTrue(helloResponse.contains(name),
                    "Could not find name '" + name + "' in response: " + helloResponse);
            });
        }
    }

    @Nested
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = "bar", string = "foo"),
            @MCRTestProperty(key = "hello", classNameOf = SagHallo.class)
        })
    class MCRTestExtensionTest2 extends MCRTestExtensionTest1 {
        @Test
        public void testClassProperty() {
            assertEquals("foo", MCRConfiguration2.getStringOrThrow("foo"));
        }

        @Test
        public void testClassPropertyOverwrite() {
            assertEquals("foo", MCRConfiguration2.getStringOrThrow("bar"));
        }
    }

    /**
     * Pins down the precedence of configuration properties: what the test itself declares wins over what an
     * extension contributed, no matter whether the test declares it on the class or on the method.
     */
    @Nested
    @ExtendWith(PropertyContributingExtension.class)
    @MCRTestConfiguration(
        properties = {
            @MCRTestProperty(key = PropertyContributingExtension.CONTESTED_KEY, string = "fromClassAnnotation")
        })
    class PropertyPrecedence {

        @Test
        public void testExtensionPropertyIsApplied() {
            assertEquals("fromExtension",
                MCRConfiguration2.getStringOrThrow(PropertyContributingExtension.UNCONTESTED_KEY));
        }

        @Test
        public void testClassAnnotationWinsOverExtension() {
            assertEquals("fromClassAnnotation",
                MCRConfiguration2.getStringOrThrow(PropertyContributingExtension.CONTESTED_KEY));
        }

        @Test
        @MCRTestConfiguration(
            properties = {
                @MCRTestProperty(key = PropertyContributingExtension.CONTESTED_KEY, string = "fromMethodAnnotation")
            })
        public void testMethodAnnotationWinsOverExtension() {
            assertEquals("fromMethodAnnotation",
                MCRConfiguration2.getStringOrThrow(PropertyContributingExtension.CONTESTED_KEY));
        }

    }

    /**
     * Contributes properties the way a real extension does, one of them under a key that the test class also
     * declares.
     */
    public static class PropertyContributingExtension implements Extension, BeforeAllCallback {

        static final String CONTESTED_KEY = "junit.precedence.contested";

        static final String UNCONTESTED_KEY = "junit.precedence.uncontested";

        @Override
        public void beforeAll(ExtensionContext context) {
            Map<String, String> classProperties = MCRTestExtension.getClassProperties(context);
            classProperties.put(CONTESTED_KEY, "fromExtension");
            classProperties.put(UNCONTESTED_KEY, "fromExtension");
        }

    }

    public static class SayHello {
        public String sayHello(String name) {
            return "Hello " + name + "!";
        }
    }

    public static class SagHallo extends SayHello {
        @Override
        public String sayHello(String name) {
            return "Hallo " + name + "!";
        }
    }

}

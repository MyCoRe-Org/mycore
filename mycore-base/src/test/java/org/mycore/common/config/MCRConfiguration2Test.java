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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.annotation.MCRProperty;

import jakarta.inject.Singleton;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRConfiguration2Test extends MCRTestCase {

    @Test(expected = MCRConfigurationException.class)
    public final void testDeprecatedProperties() {
        String deprecatedProperty = "MCR.Editor.FileUpload.MaxSize";
        MCRConfiguration2.getString(deprecatedProperty);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.C2.Object", classNameOf = TestObject.class),
        @MCRTestProperty(key = "MCR.C2.Object.Class", classNameOf = TestObject.class),
        @MCRTestProperty(key = "MCR.C2.Object.class", classNameOf = TestObject.class)
    })
    public final void testObjectInstanceIsReturned() {
        assertTrue(MCRConfiguration2.<TestObject>getInstanceOf("MCR.C2.Object").isPresent());
        assertTrue(MCRConfiguration2.<TestObject>getInstanceOf("MCR.C2.Object.Class").isPresent());
        assertTrue(MCRConfiguration2.<TestObject>getInstanceOf("MCR.C2.Object.class").isPresent());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.C2.Object", classNameOf = TestObject.class),
        @MCRTestProperty(key = "MCR.C2.Object.Foo", string = "Bar")
    })
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public final void testObjectInstanceWithoutSuffixIsConfigured() {
        TestObject instance = MCRConfiguration2.<TestObject>getInstanceOf("MCR.C2.Object").get();
        assertEquals("Bar", instance.getFoo());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.C2.Object.Class", classNameOf = TestObject.class),
        @MCRTestProperty(key = "MCR.C2.Object.Foo", string = "Bar")
    })
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public final void testObjectInstanceWithUpperCaseSuffixIsConfigured() {
        TestObject instance = MCRConfiguration2.<TestObject>getInstanceOf("MCR.C2.Object.Class").get();
        assertEquals("Bar", instance.getFoo());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.C2.Object.class", classNameOf = TestObject.class),
        @MCRTestProperty(key = "MCR.C2.Object.Foo", string = "Bar")
    })
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public final void testObjectInstanceWithLowerCaseSuffixIsConfigured() {
        TestObject instance = MCRConfiguration2.<TestObject>getInstanceOf("MCR.C2.Object.class").get();
        assertEquals("Bar", instance.getFoo());
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.C2.Object", classNameOf = TestObject.class)
    })
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public final void testObjectInstanceIsNotShared() {
        TestObject instance1 = MCRConfiguration2.<TestObject>getInstanceOf("MCR.C2.Object").get();
        TestObject instance2 = MCRConfiguration2.<TestObject>getInstanceOf("MCR.C2.Object").get();
        assertNotEquals(instance1, instance2);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.C2.SameObject", classNameOf = TestObject.class)
    })
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public final void testSingleObjectInstanceWithSameKeyIsShared() {
        TestObject instance1 = MCRConfiguration2.<TestObject>getSingleInstanceOf("MCR.C2.SameObject").get();
        TestObject instance2 = MCRConfiguration2.<TestObject>getSingleInstanceOf("MCR.C2.SameObject").get();
        assertEquals(instance1, instance2);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.C2.DifferentObject1", classNameOf = TestObject.class),
        @MCRTestProperty(key = "MCR.C2.DifferentObject2", classNameOf = TestObject.class)
    })
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public final void testSingleObjectInstanceWithDifferentKeyIsNotShared() {
        TestObject instance1 = MCRConfiguration2.<TestObject>getSingleInstanceOf("MCR.C2.DifferentObject1").get();
        TestObject instance2 = MCRConfiguration2.<TestObject>getSingleInstanceOf("MCR.C2.DifferentObject2").get();
        assertNotEquals(instance1, instance2);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.C2.Singleton", classNameOf = TestSingleton.class),
        @MCRTestProperty(key = "MCR.C2.Singleton.Foo", string = "Bar")
    })
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public final void testSingletonInstanceIsShared() {
        TestSingleton instance1 = MCRConfiguration2.<TestSingleton>getInstanceOf("MCR.C2.Singleton").get();
        TestSingleton instance2 = MCRConfiguration2.<TestSingleton>getInstanceOf("MCR.C2.Singleton").get();
        assertSame(instance1, instance2);
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.C2.Singleton", classNameOf = TestSingleton.class),
        @MCRTestProperty(key = "MCR.C2.Singleton.Foo", string = "Bar")
    })
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public final void testSingletonInstanceIsConfigured() {
        TestSingleton instance = MCRConfiguration2.<TestSingleton>getInstanceOf("MCR.C2.Singleton").get();
        assertEquals("Bar", instance.getFoo());
    }

    public static final class TestObject {

        @MCRProperty(name = "Foo", required = false)
        public String foo;

        public String getFoo() {
            return foo;
        }

    }

    @Singleton
    public static final class TestSingleton {

        private static final TestSingleton INSTANCE = new TestSingleton();

        @MCRProperty(name = "Foo", required = false)
        public String foo;

        public static TestSingleton getInstance() {
            return INSTANCE;
        }

        public String getFoo() {
            return foo;
        }

    }

}

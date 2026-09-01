package org.mycore.common.config.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mycore.common.config.MCRConfiguration2.getInstanceOfOrThrow;
import static org.mycore.common.config.MCRConfiguration2.set;

import org.junit.jupiter.api.Test;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRExample01 {

    @Test
    public void example() {

        set("MCR.Foo.Class", Foo.class.getName());

        Foo foo = getInstanceOfOrThrow(Foo.class, "MCR.Foo");

        assertEquals(Foo.class, foo.getClass());
        assertEquals("MyCoRe", foo.toString());

    }

    public static class Foo {

        private final String value;

        private Foo(String name) {
            this.value = name;
        }

        public static Foo getInstance() {
            return new Foo("MyCoRe");
        }

        @Override
        public String toString() {
            return value;
        }

    }

}

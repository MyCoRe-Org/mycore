package org.mycore.util.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

public class MCRDecoratorTest {

    @Test
    public void isDecorated() {
        Container container = new Container("base");
        TestDecorator a = new TestDecorator(container, "a");

        assertTrue("a should be decorated", MCRDecorator.isDecorated(a));
        assertFalse("container does not implement the MCRDecorator interface", MCRDecorator.isDecorated(container));
    }

    @Test
    public void get() {
        Container container = new Container("base");
        TestDecorator a = new TestDecorator(container, "a");
        TestDecorator b = new TestDecorator(a, "b");
        Optional<Container> resolved = MCRDecorator.get(b);

        assertTrue(resolved.isPresent());
        assertEquals(a.getValue(), resolved.map(Container::getValue).orElse("empty"));
        assertFalse(MCRDecorator.get(container).isPresent());
    }

    @Test
    public void resolve() {
        Container container = new Container("base");
        TestDecorator a = new TestDecorator(container, "a");
        TestDecorator b = new TestDecorator(a, "b");
        Optional<Container> resolved = MCRDecorator.resolve(b);

        assertTrue(resolved.isPresent());
        assertEquals(container.getValue(), resolved.map(Container::getValue).orElse("empty"));
        assertFalse(MCRDecorator.resolve(container).isPresent());
    }

    private static class TestDecorator extends Container implements MCRDecorator<Container> {

        private Container container;

        TestDecorator(Container container, String value) {
            super(value);
            this.container = container;
        }

        @Override
        public Container get() {
            return this.container;
        }

    }

    private static class Container {

        private String value;

        Container(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

}

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

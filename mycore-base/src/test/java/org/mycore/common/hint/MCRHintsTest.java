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

package org.mycore.common.hint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRHintsTest extends MCRTestCase {

    private static final MCRHintKey<Foo> FOO_HINT = new MCRHintKey<>(Foo.class, "FOO", Foo::toString);

    private static final MCRHintKey<Foo> OTHER_FOO_HINT = new MCRHintKey<>(Foo.class, "OTHER_FOO", Foo::toString);

    private static final MCRHintKey<Bar> BAR_HINT = new MCRHintKey<>(Bar.class, "BAR", Bar::toString);

    @Test
    public void emptyHints() {

        MCRHintsBuilder builder = new MCRHintsBuilder();
        MCRHints hints = builder.build();

        Optional<Foo> fooHint = hints.get(FOO_HINT);
        assertNotNull(fooHint);
        assertTrue(fooHint.isEmpty());

    }

    @Test
    public void nullValueHints() {

        MCRHintsBuilder builder = new MCRHintsBuilder();
        builder.add(FOO_HINT, (Foo) null);
        MCRHints hints = builder.build();

        Optional<Foo> fooHint = hints.get(FOO_HINT);
        assertNotNull(fooHint);
        assertTrue(fooHint.isEmpty());

    }

    @Test
    public void nonNullValueHints() {

        Foo foo = new Foo();

        MCRHintsBuilder builder = new MCRHintsBuilder();
        builder.add(FOO_HINT, foo);
        MCRHints hints = builder.build();

        Optional<Foo> fooHint = hints.get(FOO_HINT);
        assertNotNull(fooHint);
        assertTrue(fooHint.isPresent());
        assertEquals(foo, fooHint.get());

    }

    @Test
    public void emptyOptionalHints() {

        MCRHintsBuilder builder = new MCRHintsBuilder();
        builder.add(FOO_HINT, Optional.empty());
        MCRHints hints = builder.build();

        Optional<Foo> fooHint = hints.get(FOO_HINT);
        assertNotNull(fooHint);
        assertTrue(fooHint.isEmpty());

    }

    @Test
    public void nonEmptyOptionalHints() {

        Foo foo = new Foo();

        MCRHintsBuilder builder = new MCRHintsBuilder();
        builder.add(FOO_HINT, Optional.of(foo));
        MCRHints hints = builder.build();

        Optional<Foo> fooHint = hints.get(FOO_HINT);
        assertNotNull(fooHint);
        assertTrue(fooHint.isPresent());
        assertEquals(foo, fooHint.get());

    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void multipleValuesHints() {

        Foo foo = new Foo();
        Foo otherFoo = new Foo();
        Bar bar = new Bar();

        MCRHintsBuilder builder = new MCRHintsBuilder();
        builder.add(FOO_HINT, foo);
        builder.add(OTHER_FOO_HINT, otherFoo);
        builder.add(BAR_HINT, bar);
        MCRHints hints = builder.build();

        Optional<Foo> fooHint = hints.get(FOO_HINT);
        assertEquals(foo, fooHint.get());

        Optional<Foo> otherFooHint = hints.get(OTHER_FOO_HINT);
        assertEquals(otherFoo, otherFooHint.get());

        Optional<Bar> barHint = hints.get(BAR_HINT);
        assertEquals(bar, barHint.get());

    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void replaceValuesHints() {

        Foo foo = new Foo();
        Foo replacementFoo = new Foo();

        MCRHintsBuilder builder = new MCRHintsBuilder();
        builder.add(FOO_HINT, foo);
        builder.add(FOO_HINT, replacementFoo);
        MCRHints hints = builder.build();

        Optional<Foo> fooHint = hints.get(FOO_HINT);
        assertEquals(replacementFoo, fooHint.get());

    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void builderKeepingValues() {

        Foo foo = new Foo();

        MCRHintsBuilder builder = new MCRHintsBuilder();
        builder.add(FOO_HINT, foo);
        MCRHintsBuilder builder2 = builder.build().builder();
        MCRHints hints = builder2.build();

        Optional<Foo> fooHint = hints.get(FOO_HINT);
        assertEquals(foo, fooHint.get());

    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void builderReplacingValues() {

        Foo foo = new Foo();
        Foo otherFoo = new Foo();

        MCRHintsBuilder builder = new MCRHintsBuilder();
        builder.add(FOO_HINT, foo);
        MCRHints hints = builder.build();

        MCRHintsBuilder otherBuilder = hints.builder();
        otherBuilder.add(FOO_HINT, otherFoo);
        MCRHints otherHints = otherBuilder.build();

        Optional<Foo> fooHint = hints.get(FOO_HINT);
        assertEquals(foo, fooHint.get());

        Optional<Foo> otherFooHint = otherHints.get(FOO_HINT);
        assertEquals(otherFoo, otherFooHint.get());

    }

    @Test
    public void builderExtendingValues() {

        Foo foo = new Foo();
        Foo otherFoo = new Foo();

        MCRHintsBuilder builder = new MCRHintsBuilder();
        builder.add(FOO_HINT, foo);
        MCRHints hints = builder.build();

        MCRHintsBuilder otherBuilder = hints.builder();
        otherBuilder.add(OTHER_FOO_HINT, otherFoo);
        MCRHints otherHints = otherBuilder.build();

        assertTrue(hints.get(FOO_HINT).isPresent());
        assertFalse(hints.get(OTHER_FOO_HINT).isPresent());

        assertTrue(otherHints.get(FOO_HINT).isPresent());
        assertTrue(otherHints.get(OTHER_FOO_HINT).isPresent());

    }

    private static class Foo {

    }

    private static class Bar {

    }

}

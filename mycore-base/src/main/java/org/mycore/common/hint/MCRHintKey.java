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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A key for {@link MCRHints}.
 *
 * @param <T> The type for values used in conjunction with this key.
 */
public class MCRHintKey<T> implements Comparable<MCRHintKey<?>> {

    private final String name;

    private final Function<T, String> formatter;

    private final List<Consumer<T>> checks;

    /**
     * @deprecated Use {@link #MCRHintKey(Class, Class, String, Function)} instead
     */
    @Deprecated
    public MCRHintKey(Class<?> hintClass, String name, Function<T, String> formatter) {
        this(hintClass, Object.class, name, formatter, List.of());
    }

    public MCRHintKey(Class<?> hintClass, Class<?> namespaceClass, String name, Function<T, String> formatter) {
        this(hintClass, namespaceClass, name, formatter, List.of());
    }

    public MCRHintKey(Class<?> hintClass, Class<?> namespaceClass, String name, Function<T, String> formatter,
        List<Consumer<T>> checks) {
        Objects.requireNonNull(hintClass);
        Objects.requireNonNull(namespaceClass);
        this.name = Objects.requireNonNull(name) + "@" + namespaceClass.getName();
        this.formatter = Objects.requireNonNull(formatter, "Formatter must not be null");
        this.checks = new ArrayList<>(Objects.requireNonNull(checks, "Checks must not be null"));
        this.checks.forEach(check -> Objects.requireNonNull(checks, "Check must not be null"));
    }

    public final String format(T value) {
        if (value == null) {
            return "null";
        }
        String formattedValue = formatter.apply(value);
        if (formattedValue == null) {
            return value.getClass().getName();
        }
        return formattedValue;
    }

    public final T check(T value) {
        checks.forEach(check -> check.accept(value));
        return value;
    }

    @Override
    public final String toString() {
        return name;
    }

    @Override
    public final int compareTo(MCRHintKey<?> other) {
        return name.compareTo(other.name);
    }

}

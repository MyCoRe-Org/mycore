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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A key for {@link MCRHints}.
 *
 * @param <T> The type for values used in conjunction with this key.
 */
public class MCRCollectionHintKey<T, C extends Collection<T>> extends MCRHintKey<C> {

    public MCRCollectionHintKey(Class<?> hintValueClass, Class<?> namespaceClass, String name,
        Function<C, String> formatter) {
        this(hintValueClass, namespaceClass, name, formatter, List.of(), List.of());
    }

    public MCRCollectionHintKey(Class<?> hintValueClass, Class<?> namespaceClass, String name,
        Function<C, String> formatter, List<Consumer<C>> checks) {
        this(hintValueClass, namespaceClass, name, formatter, checks, List.of());
    }

    public MCRCollectionHintKey(Class<?> hintValueClass, Class<?> namespaceClass, String name,
        Function<C, String> formatter, List<Consumer<C>> checks, List<Consumer<T>> elementChecks) {
        super(hintValueClass, namespaceClass, name, formatter, combineChecks(checks, elementChecks));
    }

    private static <T, C extends Collection<T>> List<Consumer<C>> combineChecks(List<Consumer<C>> checks,
        List<Consumer<T>> elementChecks) {

        Objects.requireNonNull(elementChecks, "Element checks must not be null");
        elementChecks.forEach(elementCheck -> Objects.requireNonNull(elementCheck, "Element check must not be null"));

        List<Consumer<T>> allElementChecks = new ArrayList<>(elementChecks.size() + 1);
        allElementChecks.add(element -> Objects.requireNonNull(element, "Element must not be null"));
        allElementChecks.addAll(elementChecks);

        List<Consumer<C>> combinedChecks = new ArrayList<>(checks.size() + 1);
        combinedChecks.addAll(checks);
        combinedChecks.add(collection -> checkElements(collection, allElementChecks));

        return combinedChecks;

    }

    private static <T, C extends Collection<T>> void checkElements(C collection, List<Consumer<T>> elementChecks) {
        collection.forEach(element -> checkElement(element, elementChecks));
    }

    private static <T> void checkElement(T element, List<Consumer<T>> elementChecks) {
        elementChecks.forEach(elementCheck -> elementCheck.accept(element));
    }

}

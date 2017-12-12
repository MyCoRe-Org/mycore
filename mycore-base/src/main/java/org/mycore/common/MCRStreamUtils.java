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

package org.mycore.common;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;

/**
 * Helper methods to handle common Stream use cases.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRStreamUtils {

    /**
     * Short circuit for calling <code>flatten(node, subNodeSupplier, subNodeSupplier, t -&gt; true)</code>
     * @param node node that holds kind-of subtree.
     * @param subNodeSupplier a function that delivers subtree items of next level
     * @param streamProvider a function that makes a Stream of a Collection&lt;T&gt;, usually <code>Collection::stream</code> or <code>Collection::parallelStream</code>
     * @see #flatten(Object, Function, Function, Predicate)
     * @since 2016.04
     */
    public static <T> Stream<T> flatten(T node, Function<T, Collection<T>> subNodeSupplier,
        Function<Collection<T>, Stream<T>> streamProvider) {
        return Stream.concat(Stream.of(node), subNodeSupplier.andThen(streamProvider).apply(node).flatMap(
            subNode -> flatten(subNode, subNodeSupplier, streamProvider)));
    }

    /**
     * Example:
     * <pre>
     *   MCRCategory foo = MCRCategoryDAOFactory.getInstance().getCategory(MCRCategoryID.rootID("foo"), -1);
     *   Stream&lt;MCRCategory&gt; parentCategories = flatten(foo, MCRCategory::getChildren, true, MCRCategory::hasChildren);
     * </pre>
     * @param node first node the stream is made of
     * @param subNodeSupplier a function that delivers subtree items of next level
     * @param streamProvider a function that makes a Stream of a Collection&lt;T&gt;, usually <code>Collection::stream</code> or <code>Collection::parallelStream</code>
     * @param filter a predicate that filters the element of the next level
     * @since 2016.04
     */
    public static <T> Stream<T> flatten(T node, Function<T, Collection<T>> subNodeSupplier,
        Function<Collection<T>, Stream<T>> streamProvider, Predicate<T> filter) {
        return Stream.concat(Stream.of(node),
            subNodeSupplier.andThen(streamProvider).apply(node).filter(filter).flatMap(
                subNode -> flatten(subNode, subNodeSupplier, streamProvider, filter)));
    }

    /**
     * Transforms an Enumeration in a Stream.
     * @param e the enumeration to transform
     * @return a sequential, ordered Stream of unknown size
     */
    public static <T> Stream<T> asStream(Enumeration<T> e) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(Iterators.forEnumeration(e), Spliterator.ORDERED), false);
    }

    /**
     * Concats any number of Streams not just 2 as in {@link Stream#concat(Stream, Stream)}.
     * @since 2016.04
     */
    @SafeVarargs
    public static <T> Stream<T> concat(Stream<T>... streams) {
        return Stream.of(streams).reduce(Stream::concat).orElse(Stream.empty());
    }

    /**
     * Stream distinct by filter function.
     * <p>
     * <code>
     * persons.stream().filter(MCRStreamUtils.distinctByKey(p -&gt; p.getName());
     * </code>
     * </p>
     * It should be noted that for ordered parallel stream this solution does not guarantee
     * which object will be extracted (unlike normal distinct()).
     * 
     * @see <a href="https://stackoverflow.com/questions/23699371/java-8-distinct-by-property">stackoverflow</a>
     * @param keyExtractor a compare function
     * @return a predicate
     */
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    /**
     * Negates a predicate.
     *
     * @see <a href="https://stackoverflow.com/questions/28235764/how-can-i-negate-a-lambda-predicate">stackoverflow</a>
     * @param predicate the predicate to negate
     * @return the negated predicate
     */
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }

}

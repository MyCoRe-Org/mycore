/**
 * 
 */
package org.mycore.common;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Spliterator;
import java.util.Spliterators;
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
     * Short circuit for calling <code>flatten(node, subNodeSupplier, parallel, t -&gt; true)</code>
     * @param node node that holds kind-of subtree.
     * @param subNodeSupplier a function that delivers subtree items of next level
     * @param parallel if the returned Stream should be parallel
     * @see #flatten(Object, Function, boolean, Predicate)
     * @since 2015.12
     */
    public static <T> Stream<T> flatten(T node, Function<T, Collection<T>> subNodeSupplier, boolean parallel) {
        Collection<T> subNodes = subNodeSupplier.apply(node);
        return Stream.concat(Stream.of(node), (parallel ? subNodes.parallelStream() : subNodes.stream())
            .flatMap(subNode -> flatten(subNode, subNodeSupplier, parallel)));
    }

    /**
     * Example:
     * <pre>
     *   MCRCategory foo = MCRCategoryDAOFactory.getInstance().getCategory(MCRCategoryID.rootID("foo"), -1);
     *   Stream&lt;MCRCategory&gt; parentCategories = flatten(foo, MCRCategory::getChildren, true, MCRCategory::hasChildren);
     * </pre>
     * @param node first node the stream is made of
     * @param subNodesupplier a function that delivers subtree items of next level
     * @param parallel if the returned Stream should be parallel
     * @param filter a predicate that filters the element of the next level
     * @since 2015.12
     */
    public static <T> Stream<T> flatten(T node, Function<T, Collection<T>> subNodesupplier, boolean parallel,
        Predicate<T> filter) {
        Collection<T> subNodes = subNodesupplier.apply(node);
        return Stream.concat(Stream.of(node), (parallel ? subNodes.parallelStream() : subNodes.stream()).filter(filter)
            .flatMap(subNode -> flatten(subNode, subNodesupplier, parallel, filter)));
    }

    /**
     * Transforms an Enumeration in a Stream.
     * @param e the enumeration to transform
     * @return a sequential, ordered Stream of unknown size
     */
    public static <T> Stream<T> asStream(Enumeration<T> e) {
        return StreamSupport
            .stream(Spliterators.spliteratorUnknownSize(Iterators.forEnumeration(e), Spliterator.ORDERED), false);
    }

    /**
     * Concats any number of Streams not just 2 as in {@link Stream#concat(Stream, Stream)}.
     * @since 2016.04
     */
    @SafeVarargs
    public static <T> Stream<T> concat(Stream<T>... streams) {
        return Stream.of(streams).reduce(Stream::concat).orElse(Stream.empty());
    }

}

/**
 * 
 */
package org.mycore.common;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Helper methods to handle common Stream use cases.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRStreamUtils {

    /**
     * Short circuit for calling <code>flatten(node, subNodeSupplier, parallel, t -> true)</code>
     * @param node node that holds kind-of subtree.
     * @param subNodeSupplier a function that delivers subtree items of next level
     * @param parallel if the returned Stream should be parallel
     * @see #flatten(Object, Function, boolean, Predicate)
     * @since 2015.12
     */
    public static <T> Stream<T> flatten(T node, Function<T, Collection<T>> subNodeSupplier, boolean parallel) {
        Collection<T> subNodes = subNodeSupplier.apply(node);
        return Stream.concat(Stream.of(node), (parallel ? subNodes.parallelStream() : subNodes.stream()).flatMap(subNode -> flatten(subNode, subNodeSupplier, parallel)));
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
    
}

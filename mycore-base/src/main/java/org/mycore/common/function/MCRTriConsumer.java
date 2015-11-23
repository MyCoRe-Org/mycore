/**
 * 
 */
package org.mycore.common.function;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents an operation that accepts two input arguments and returns no result. This is the three-arity
 * specialization of {@link Consumer}. Unlike most other {@link FunctionalInterface functional interfaces},
 * <code>MCRTriConsumer</code> is expected to operate via side-effects.
 * <p>
 * This is a {@link FunctionalInterface functional interface} whose functional method is
 * {@link #accept(Object, Object, Object)}.
 * </p>
 * 
 * @author Thomas Scheffler (yagee)
 */
@FunctionalInterface
public interface MCRTriConsumer<T, U, V> {
    /**
     * Performs this operation on the given arguments.
     * 
     * @param t
     *            the first input argument
     * @param u
     *            the second input argument
     * @param v
     *            the third input argument
     */
    public void accept(T t, U u, V v);

    /**
     * Returns a composed MCRTriConsumer that performs, in sequence, this operation followed by the after operation. If
     * performing either operation throws an exception, it is relayed to the caller of the composed operation. If
     * performing this operation throws an exception, the after operation will not be performed.
     * 
     * @param after
     *            the Operation to perform after this operation
     * @return a composed MCRTriConsumer that performs in sequence this operation followed by the after operation
     * @throws NullPointerException
     *             if after is null
     */
    public default MCRTriConsumer<T, U, V> andThen(MCRTriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }
}

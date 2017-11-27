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
    void accept(T t, U u, V v);

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
    default MCRTriConsumer<T, U, V> andThen(MCRTriConsumer<? super T, ? super U, ? super V> after) {
        Objects.requireNonNull(after);
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }
}

package org.mycore.common.function;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a function that accepts one argument and produces a result and throws an Exception.
 *
 * <p>Use {@link #toFunction()} or {@link #toFunction(BiFunction, Class)} to transform this MCRThrowFunction into a Function that can be handled throughout Java 8.</p>
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the exception that is throw by this function-
 *
 * @since 2015.12
 */
@FunctionalInterface
public interface MCRThrowFunction<T, R, E extends Throwable> {
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R apply(T t) throws E;

    /**
     * Returns a composed function that first applies the {@code before}
     * function to its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of input to the {@code before} function, and to the
     *           composed function
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before}
     * function and then applies this function
     * @throws NullPointerException if before is null
     *
     * @see #andThen(MCRThrowFunction)
     */
    default <V> MCRThrowFunction<V, R, E> compose(MCRThrowFunction<? super V, ? extends T, ? extends E> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     *
     * @see #compose(MCRThrowFunction)
     */
    default <V> MCRThrowFunction<T, V, E> andThen(MCRThrowFunction<? super R, ? extends V, ? extends E> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    /**
     * Returns a function that catches &lt;E&gt; and forwards it to the <code>throwableHandler</code> together with the Exception.
     * 
     * Use this method if you want to react on the certain Exceptions and return a result or rethrow a specific RuntimeExption.
     * @param throwableHandler a BiFunction that handles original Input and caught Exception
     * @param exClass class of exception to catch
     */
    default Function<T, R> toFunction(BiFunction<T, E, R> throwableHandler, Class<? super E> exClass) {
        return t -> {
            try {
                return this.apply(t);
            } catch (Throwable e) {
                if (exClass.isAssignableFrom(e.getClass())) {
                    @SuppressWarnings("unchecked")
                    E handableException = (E) e;
                    return throwableHandler.apply(t, handableException);
                }
                throw (RuntimeException) e;
            }
        };
    }

    /**
     * Returns a Function that applies &lt;T&gt; and catches any exception and wraps it into a {@link RuntimeException} if needed.
     * Use this if you just want no specific exception handling.
     */
    default Function<T, R> toFunction() {
        return toFunction((t, e) -> {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }, Throwable.class);
    }
}

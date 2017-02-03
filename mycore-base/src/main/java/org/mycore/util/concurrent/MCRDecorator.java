package org.mycore.util.concurrent;

import java.util.Optional;

import com.google.common.reflect.TypeToken;

/**
 * Classes can implement this interface if they want to use
 * the decorator pattern. Contains some static helper methods
 * to check if an instance is implementing the decorator.
 * 
 * @author Matthias Eichner
 */
public interface MCRDecorator<V> {

    /**
     * Returns the enclosing instance.
     * 
     * @return the decorated instance
     */
    public V get();

    /**
     * Checks if the given object is decorated by this interface.
     * 
     * @param decorator the interface to check
     * @return true if the instance is decorated by an MCRDecorator
     */
    public static boolean isDecorated(Object decorator) {
        return TypeToken.of(decorator.getClass()).getTypes().interfaces().stream().filter(tt -> {
            return tt.isSubtypeOf(MCRDecorator.class);
        }).findAny().isPresent();
    }

    /**
     * Returns an optional with the enclosing value of the decorator. The decorator
     * should implement the {@link MCRDecorator} interface. If not, an empty optional
     * is returned.
     * 
     * @param decorator the MCRDecorator
     * @return an optional with the decorated instance
     */
    @SuppressWarnings("unchecked")
    public static <V> Optional<V> get(Object decorator) {
        if (isDecorated(decorator)) {
            try {
                return Optional.of(((MCRDecorator<V>) decorator).get());
            } catch (Exception exc) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

}

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
    V get();

    /**
     * Checks if the given object is decorated by this interface.
     * 
     * @param decorator the interface to check
     * @return true if the instance is decorated by an MCRDecorator
     */
    static boolean isDecorated(Object decorator) {
        return TypeToken.of(decorator.getClass()).getTypes().interfaces().stream()
            .filter(tt -> tt.isSubtypeOf(MCRDecorator.class)).findAny().isPresent();
    }

    /**
     * Returns an optional with the enclosing value of the decorator. The decorator
     * should implement the {@link MCRDecorator} interface. If not, an empty optional
     * is returned.
     * 
     * <ul>
     *   <li>get: MCRDecorator -&gt; <b>MCRDecorator</b> -&gt; MCRDecorator -&gt; object
     *   <li>resolve: MCRDecorator -&gt; MCRDecorator -&gt; MCRDecorator -&gt; <b>object</b>
     * </ul>
     * 
     * @param decorator the MCRDecorator
     * @return an optional with the decorated instance
     */
    @SuppressWarnings("unchecked")
    static <V> Optional<V> get(Object decorator) {
        if (isDecorated(decorator)) {
            try {
                return Optional.of(((MCRDecorator<V>) decorator).get());
            } catch (Exception exc) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * Same as {@link #get()}, but returns the last object which does not implement
     * the decorator interface anymore.
     * 
     * <ul>
     *   <li>get: MCRDecorator -&gt; <b>MCRDecorator</b> -&gt; MCRDecorator -&gt; object
     *   <li>resolve: MCRDecorator -&gt; MCRDecorator -&gt; MCRDecorator -&gt; <b>object</b>
     * </ul>
     * 
     * @param decorator the MCRDecorator
     * @return an optional with the decorated instance
     */
    static <V> Optional<V> resolve(Object decorator) {
        Optional<V> base = get(decorator);
        while (base.isPresent()) {
            Optional<V> nextLevel = get(base.get());
            if (nextLevel.isPresent()) {
                base = nextLevel;
            } else {
                break;
            }
        }
        return base;
    }

}

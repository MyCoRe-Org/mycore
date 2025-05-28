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

import java.util.Optional;

import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.MCRResourceResolver;
import org.mycore.resource.provider.MCRResourceProvider;

/**
 * An immutable, typed lookup table that is intended to provide configuration values for complex calculations
 * like {@link MCRResourceResolver#resolve(MCRResourcePath, MCRHints)}.
 * <p>
 * The intended use ist to replace long parameter lists with a single {@link MCRHints} parameter, especially
 * in case of parameters that have sensible default values which are only deviated from in rare edge cases.
 * A convenience method without that parameter, using the default values, should also be provided, like
 * {@link MCRResourceResolver#resolve(MCRResourcePath)}.
 * <p>
 * Something similar could also be achieved with a specialized POJO
 * (i.e. <code>Foo#bar(String normalParameter, Foo$BarConfiguration configuration)</code>) or a
 * simple collection (i.e. <code>Foo#bar(String normalParameter, Map&lt;String, ?&gt; configuration)</code>).
 * Compared to those approaches, {@link MCRHints} allows for a certain mount of extendability while
 * also keeping type safety.
 * <p>
 * For example, it would be possible to define a custom {@link MCRHintKey} that is used in a custom
 * {@link MCRResourceProvider} that is configured to be used deep in
 * {@link MCRResourceResolver#resolve(MCRResourcePath, MCRHints)} and passing concrete values to this method.
 */
public interface MCRHints {

    MCRHints EMPTY = new MCRHintsBuilder().build();

    /**
     * Returns the value for the given {@link MCRHintKey} as an {@link Optional}.
     */
    <T> Optional<T> get(MCRHintKey<T> hintKey);

    /**
     * Returns the value for the given {@link MCRHintKey} or the given fallback.
     */
    <T> T getOrElse(MCRHintKey<T> hintKey, T fallback);

    /**
     * Returns a {@link MCRHintsBuilder} that contains all entries stored in this {@link MCRHints}.
     * Intended to allow creation of modified {@link MCRHints}.
     */
    MCRHintsBuilder builder();

}

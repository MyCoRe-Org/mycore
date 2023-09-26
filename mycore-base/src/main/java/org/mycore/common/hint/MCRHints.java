/*
 *
 * $Revision$ $Date$
 *
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 *
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
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

    /**
     * Returns the value for the given {@link MCRHintKey} as an {@link Optional}.
     */
    <T> Optional<T> get(MCRHintKey<T> hintKey);

    /**
     * Returns a {@link MCRHintsBuilder} that contains all entries stored in this {@link MCRHints}.
     * Intended to allow creation of modified {@link MCRHints}.
     */
    MCRHintsBuilder builder();

}
